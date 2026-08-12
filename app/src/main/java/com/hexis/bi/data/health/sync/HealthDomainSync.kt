package com.hexis.bi.data.health.sync

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.data.health.local.contiguousDateRanges
import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import com.hexis.bi.data.health.remote.HealthRemoteDataSource
import com.hexis.bi.utils.constants.HealthAnalysisConstants
import com.hexis.bi.utils.constants.TerraCacheConstants
import com.hexis.bi.utils.constants.TerraSyncConstants
import java.time.Duration
import java.time.LocalDate
import com.hexis.bi.data.terra.MergedSourceResult
import com.hexis.bi.data.terra.TerraRestIdentity
import com.hexis.bi.data.terra.decodeTerraRestIdentity
import com.hexis.bi.data.terra.encodeForCursor
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import timber.log.Timber

internal interface HealthDomainSpec<T> {
    val source: String

    val label: String

    val fetchLookbackDays: Long get() = 0L

    fun dayOf(item: T): LocalDate

    fun parse(rows: List<Any?>): List<T>

    fun merge(perSource: List<List<T>>): List<T>

    fun toAggregate(item: T): CanonicalDailyAggregate

    fun toDomain(aggregate: CanonicalDailyAggregate): T

    suspend fun fetchJson(terraUserId: String, start: LocalDate, end: LocalDate): Result<List<Any?>>
}

internal class HealthDomainSync<T>(
    private val local: HealthLocalDataSource,
    private val remote: HealthRemoteDataSource,
    private val auth: FirebaseAuth,
    private val spec: HealthDomainSpec<T>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + io),
) {

    val updates: Flow<Unit> = local.changes.filter { it == spec.source }.map { }

    private val pendingFills = ConcurrentHashMap.newKeySet<String>()

    suspend fun coverage(start: LocalDate, end: LocalDate): HealthRangeCoverage = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext HealthRangeCoverage.SETTLED
        val identities = remote.fetchableIdentities(storedIdentities(uid)).ids()
        if (identities.isEmpty()) return@withContext HealthRangeCoverage.SETTLED
        local.coverage(uid, identities, spec.source, dateRange(start, end))
    }

    suspend fun sync(start: LocalDate, end: LocalDate): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(notAuthenticated())
        return refresh(uid, start, end)
    }

    suspend fun range(start: LocalDate, end: LocalDate, withSamples: Boolean = false): Result<List<T>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(notAuthenticated())
        val days = dateRange(start, end)
        val known = storedIdentities(uid)
        if (hasGaps(uid, known, days)) fillInBackground(uid, start, end)
        return Result.success(mergedAny(uid, known.ids(), days, withSamples))
    }

    private suspend fun hasGaps(
        uid: String,
        known: List<TerraRestIdentity>,
        days: List<LocalDate>,
    ): Boolean = withContext(io) {
        if (known.isEmpty()) return@withContext true
        val fetchable = remote.fetchableIdentities(known).ids()
        if (fetchable.isEmpty()) return@withContext false
        local.hasUnsyncedDays(uid, fetchable, spec.source, days, TTL)
    }

    private fun fillInBackground(uid: String, start: LocalDate, end: LocalDate) {
        val key = "$uid:$start:$end"
        if (!pendingFills.add(key)) return
        scope.launch {
            try {
                refresh(uid, start, end)
            } finally {
                pendingFills.remove(key)
            }
        }
    }

    private suspend fun refresh(uid: String, start: LocalDate, end: LocalDate): Result<Unit> =
        withContext(io) {
            local.singleFlight(uid, spec.source) {
                val days = dateRange(start, end)
                val identities = remote.identities().getOrElse { error ->
                    Timber.w(error, "Provider lookup failed")
                    return@singleFlight Result.failure(error)
                }
                local.rememberIdentityOrder(uid, identities.all.map { it.encodeForCursor() })

                val fetchable = identities.fetchable
                if (fetchable.isEmpty()) {
                    Timber.w("%s has no queryable source; serving cache only", spec.label)
                    return@singleFlight Result.success(Unit)
                }

                val missing = local.missingDays(uid, fetchable.ids(), spec.source, days, TTL)
                if (missing.isEmpty()) {
                    recordSynced(uid, fetchable, days.first()..days.last())
                    return@singleFlight Result.success(Unit)
                }

                val windows = contiguousDateRanges(missing).flatMap { it.windowed() }
                var failure: Throwable? = null
                var stored = 0
                var consecutiveFailures = 0
                var unreachable = false

                for (window in windows) {
                    val merged = fetchWindow(fetchable, window).getOrElse { error ->
                        failure = error
                        consecutiveFailures++
                        null
                    }
                    if (merged == null) {
                        if (consecutiveFailures >= HealthAnalysisConstants.MAX_CONSECUTIVE_FAILURES) {
                            unreachable = true
                            break
                        }
                        continue
                    }
                    consecutiveFailures = 0
                    local.recordProviderCalls(uid, spec.source, merged.totalSources)
                    store(uid, window, merged)
                    stored++
                }

                val error = failure
                if (error != null) {
                    val unreachableSource = unreachable || stored == 0
                    Timber.w(
                        "%s refresh %s: %d/%d windows stored (%s)",
                        spec.label, if (unreachableSource) "aborted, source unreachable" else "incomplete",
                        stored, windows.size, error.message ?: error::class.simpleName,
                    )
                    return@singleFlight Result.failure(
                        if (unreachableSource) HealthSourceUnavailable(error) else error,
                    )
                }

                Result.success(Unit)
            }
        }

    private suspend fun store(uid: String, window: ClosedRange<LocalDate>, merged: MergedSourceResult<T>) {
        if (auth.currentUser?.uid != uid) {
            Timber.w("%s refresh finished for a signed-out user; discarding %s", spec.label, window)
            return
        }
        if (!merged.complete) {
            Timber.w(
                "%s refresh partial (%d/%d sources); caching the sources that answered",
                spec.label, merged.successfulSources, merged.totalSources,
            )
        }
        for ((terraUserId, rows) in merged.perIdentity) {
            val windowRows = spec.merge(listOf(rows)).filter { spec.dayOf(it) in window }
            local.storeDays(uid, terraUserId, windowRows.map(spec::toAggregate))
            local.storeEmptyDays(
                uid, terraUserId, spec.source,
                window.toDateList() - windowRows.map(spec::dayOf).toSet(),
            )
            local.recordSyncedRange(uid, terraUserId, spec.source, window)
        }
    }

    private suspend fun recordSynced(
        uid: String,
        identities: List<TerraRestIdentity>,
        window: ClosedRange<LocalDate>,
    ) {
        identities.forEach { local.recordSyncedRange(uid, it.terraUserId, spec.source, window) }
    }

    private suspend fun storedIdentities(uid: String): List<TerraRestIdentity> =
        local.storedIdentityOrder(uid).mapNotNull(::decodeTerraRestIdentity)

    private fun List<TerraRestIdentity>.ids(): List<String> = map { it.terraUserId }

    private suspend fun fetchWindow(
        identities: List<TerraRestIdentity>,
        window: ClosedRange<LocalDate>,
    ): Result<MergedSourceResult<T>> =
        remote.fetchRange(
            identities = identities,
            start = window.start.minusDays(spec.fetchLookbackDays),
            end = window.endInclusive,
            fetchJson = spec::fetchJson,
            parse = spec::parse,
        )

    private suspend fun mergedAny(
        uid: String,
        identities: List<String>,
        days: List<LocalDate>,
        withSamples: Boolean,
    ): List<T> =
        local.allAggregatesByIdentity(uid, identities, spec.source, days, withSamples).mergeToDomain()

    private fun List<List<CanonicalDailyAggregate>>.mergeToDomain(): List<T> =
        spec.merge(map { identityRows -> identityRows.map(spec::toDomain) })

    private fun notAuthenticated() = IllegalStateException("Not authenticated")

    private fun dateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        require(!end.isBefore(start))
        return generateSequence(start) { it.plusDays(1).takeIf { next -> !next.isAfter(end) } }.toList()
    }

    private fun ClosedRange<LocalDate>.toDateList(): List<LocalDate> = dateRange(start, endInclusive)

    private fun ClosedRange<LocalDate>.windowed(
        size: Long = TerraSyncConstants.SYNC_WINDOW_DAYS,
    ): List<ClosedRange<LocalDate>> {
        val windows = mutableListOf<ClosedRange<LocalDate>>()
        var cursor = start
        while (!cursor.isAfter(endInclusive)) {
            val last = minOf(cursor.plusDays(size - 1), endInclusive)
            windows += cursor..last
            cursor = last.plusDays(1)
        }
        return windows
    }

    private companion object {
        val TTL: Duration = Duration.ofMillis(TerraCacheConstants.RANGE_CACHE_TTL_MS)
    }
}
