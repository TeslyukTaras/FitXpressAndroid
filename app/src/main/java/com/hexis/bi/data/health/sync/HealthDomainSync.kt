package com.hexis.bi.data.health.sync

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.data.health.local.contiguousDateRanges
import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import com.hexis.bi.data.health.remote.HealthRemoteDataSource
import com.hexis.bi.utils.constants.TerraCacheConstants
import com.hexis.bi.utils.constants.TerraSyncConstants
import java.time.Duration
import java.time.LocalDate
import com.hexis.bi.data.terra.MergedSourceResult
import com.hexis.bi.data.terra.TerraRestIdentity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import timber.log.Timber

internal interface HealthDomainSpec<T> {
    val source: String

    val label: String

    fun dayOf(item: T): LocalDate

    fun parse(rows: List<JsonElement>): List<T>

    fun merge(perSource: List<List<T>>): List<T>

    fun toAggregate(item: T): CanonicalDailyAggregate

    fun toDomain(aggregate: CanonicalDailyAggregate): T

    suspend fun fetchJson(terraUserId: String, start: LocalDate, end: LocalDate): Result<List<JsonElement>>
}

internal class HealthDomainSync<T>(
    private val local: HealthLocalDataSource,
    private val remote: HealthRemoteDataSource,
    private val auth: FirebaseAuth,
    private val spec: HealthDomainSpec<T>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    val updates: Flow<Unit> = local.changes.filter { it == spec.source }.map { }

    suspend fun cached(start: LocalDate, end: LocalDate): List<T> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val fingerprint = local.storedFingerprint(uid) ?: return emptyList()
        return local.allAggregates(uid, spec.source, dateRange(start, end), fingerprint).toDomainList()
    }

    suspend fun sync(start: LocalDate, end: LocalDate): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(notAuthenticated())
        return refresh(uid, start, end).map { }
    }

    suspend fun range(start: LocalDate, end: LocalDate): Result<List<T>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(notAuthenticated())
        val known = local.storedFingerprint(uid)
        if (known != null) {
            val days = dateRange(start, end)
            if (local.staleDays(uid, known, spec.source, days, TTL).isEmpty()) {
                return Result.success(local.aggregates(uid, known, spec.source, days, TTL).toDomainList())
            }
        }
        return refresh(uid, start, end)
    }

    private suspend fun refresh(uid: String, start: LocalDate, end: LocalDate): Result<List<T>> =
        withContext(io) {
            local.singleFlight(uid, spec.source) {
                val days = dateRange(start, end)
                val identities = remote.identities().getOrElse { error ->
                    return@singleFlight staleOr(uid, days, null, error, "Provider lookup failed")
                }
                val fingerprint = remote.fingerprint(identities)
                local.rememberFingerprint(uid, fingerprint)

                val missing = local.missingDays(uid, fingerprint, spec.source, days, TTL)
                if (missing.isEmpty()) {
                    local.logStats(uid)
                    return@singleFlight Result.success(
                        local.aggregates(uid, fingerprint, spec.source, days, TTL).toDomainList(),
                    )
                }

                val transient = mutableListOf<T>()
                var failure: Throwable? = null
                val windows = contiguousDateRanges(missing).flatMap { it.windowed() }

                coroutineScope {
                    var pending = windows.firstOrNull()?.let { first -> asyncFetch(identities, first) }
                    for ((index, window) in windows.withIndex()) {
                        val inFlight = pending ?: break
                        pending = windows.getOrNull(index + 1)?.let { next -> asyncFetch(identities, next) }

                        val merged = inFlight.await().getOrElse { error ->
                            failure = error
                            pending?.cancel()
                            return@coroutineScope
                        }
                        local.recordProviderCalls(uid, spec.source, merged.totalSources)

                        val windowRows = merged.rows.filter { spec.dayOf(it) in window }
                        if (merged.complete) {
                            local.storeDays(uid, fingerprint, windowRows.map(spec::toAggregate))
                            local.storeEmptyDays(
                                uid, fingerprint, spec.source,
                                window.toDateList() - windowRows.map(spec::dayOf).toSet(),
                            )
                        } else {
                            // Served but not cached: a partial answer is not authoritative.
                            transient += windowRows
                            Timber.w(
                                "%s provider refresh partial (%d/%d); results served but not cached",
                                spec.label, merged.successfulSources, merged.totalSources,
                            )
                        }
                    }
                }

                failure?.let { error ->
                    return@singleFlight staleOr(uid, days, fingerprint, error, "${spec.label} refresh failed")
                }

                local.logStats(uid)
                val final = local.allAggregates(uid, spec.source, days, fingerprint)
                    .mapValues { (_, aggregate) -> spec.toDomain(aggregate) }
                    .toMutableMap()
                transient.forEach { final.putIfAbsent(spec.dayOf(it), it) }
                Result.success(final.toDomainSorted())
            }
        }

    private fun CoroutineScope.asyncFetch(
        identities: List<TerraRestIdentity>,
        window: ClosedRange<LocalDate>,
    ): Deferred<Result<MergedSourceResult<T>>> = async {
        remote.fetchRange(
            identities = identities,
            start = window.start,
            end = window.endInclusive,
            fetchJson = spec::fetchJson,
            parse = spec::parse,
            merge = spec::merge,
        )
    }

    private suspend fun staleOr(
        uid: String,
        days: List<LocalDate>,
        fingerprint: String?,
        error: Throwable,
        reason: String,
    ): Result<List<T>> {
        val stale = local.allAggregates(uid, spec.source, days, fingerprint)
        if (stale.isEmpty()) return Result.failure(error)
        local.recordStaleFallback(uid, spec.source, stale.size)
        Timber.w(error, "%s; serving %d stale %s days", reason, stale.size, spec.label)
        return Result.success(stale.toDomainList())
    }

    private fun Map<LocalDate, CanonicalDailyAggregate>.toDomainList(): List<T> =
        toSortedMap().values.map(spec::toDomain)

    private fun Map<LocalDate, T>.toDomainSorted(): List<T> = toSortedMap().values.toList()

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
