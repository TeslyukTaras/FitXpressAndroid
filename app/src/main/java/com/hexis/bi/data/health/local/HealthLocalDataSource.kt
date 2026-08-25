package com.hexis.bi.data.health.local

import com.hexis.bi.data.health.model.CanonicalBodyScanAggregate
import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import com.hexis.bi.data.health.sync.HealthRangeCoverage
import com.hexis.bi.utils.constants.CanonicalCacheConstants
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import timber.log.Timber

interface CanonicalUserCacheCleaner {
    suspend fun clearUser(userId: String)
}

internal fun contiguousDateRanges(days: Collection<LocalDate>): List<ClosedRange<LocalDate>> {
    val sorted = days.distinct().sorted()
    if (sorted.isEmpty()) return emptyList()
    val ranges = mutableListOf<ClosedRange<LocalDate>>()
    var start = sorted.first()
    var previous = start
    for (day in sorted.drop(1)) {
        if (day != previous.plusDays(1)) {
            ranges += start..previous
            start = day
        }
        previous = day
    }
    ranges += start..previous
    return ranges
}

internal fun canonicalToday(clock: Clock): LocalDate = LocalDate.now(clock)

internal fun canonicalRetentionFloor(clock: Clock): LocalDate =
    canonicalToday(clock).minusDays(CanonicalCacheConstants.DAY_RETENTION_DAYS)

internal class HealthLocalDataSource(
    private val cache: HealthAggregateDatabase,
    private val environment: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val fetchCoordinator: CanonicalFetchCoordinator = CanonicalFetchCoordinator(),
    private val diagnostics: CanonicalCacheDiagnostics = CanonicalCacheDiagnostics(),
) : CanonicalUserCacheCleaner {

    suspend fun <T> singleFlight(userId: String, source: String, block: suspend () -> T): T =
        fetchCoordinator.run("$environment:$userId:$source", block)
    suspend fun allAggregatesByIdentity(
        userId: String,
        identities: List<String>,
        source: String,
        days: List<LocalDate>,
        withSamples: Boolean,
    ): List<List<CanonicalDailyAggregate>> = withContext(io) {
        if (days.isEmpty()) return@withContext identities.map { emptyList() }
        val wanted = days.toSet()
        identities.map { terraUserId ->
            val byDay = cache.getDays(environment, userId, terraUserId, source, wanted, withSamples)
            days.mapNotNull(byDay::get)
        }
    }

    private fun fetchableWindow(): ClosedRange<LocalDate> =
        canonicalRetentionFloor(clock)..canonicalToday(clock)

    private fun daysCoveredByAllIdentities(
        userId: String,
        identities: List<String>,
        source: String,
        days: List<LocalDate>,
        todayTtl: Duration,
    ): Set<LocalDate> {
        if (identities.isEmpty()) return emptySet()
        val perIdentity = identities.map { terraUserId ->
            cache.freshDays(environment, userId, terraUserId, source, days, todayTtl)
        }
        return days.filterTo(mutableSetOf()) { day -> perIdentity.all { day in it } }
    }

    suspend fun coverage(
        userId: String,
        identities: List<String>,
        source: String,
        days: List<LocalDate>,
    ): HealthRangeCoverage = withContext(io) {
        val fetchable = days.filter { it in fetchableWindow() }
        if (identities.isEmpty() || fetchable.isEmpty()) return@withContext HealthRangeCoverage.SETTLED
        val perIdentity = identities.map { terraUserId ->
            cache.getSyncedRanges(environment, userId, terraUserId, source)
        }
        if (perIdentity.any { it.isEmpty() }) {
            return@withContext HealthRangeCoverage(syncedDays = 0, totalDays = fetchable.size)
                .also { logCoverage(source, fetchable, it, "no stored range") }
        }
        HealthRangeCoverage(
            syncedDays = fetchable.count { day -> perIdentity.all { ranges -> ranges.any { day in it } } },
            totalDays = fetchable.size,
        ).also {
            logCoverage(source, fetchable, it, perIdentity.joinToString(" + ") { ranges ->
                ranges.joinToString(",") { r -> "${r.start}..${r.endInclusive}" }
            })
        }
    }

    suspend fun hasUnsyncedDays(
        userId: String,
        identities: List<String>,
        source: String,
        days: List<LocalDate>,
        todayTtl: Duration,
    ): Boolean = withContext(io) {
        val window = fetchableWindow()
        val fetchable = days.filter { it in window }
        if (identities.isEmpty() || fetchable.isEmpty()) return@withContext false
        val perIdentity = identities.map { terraUserId ->
            cache.getSyncedRanges(environment, userId, terraUserId, source)
        }
        if (perIdentity.any { it.isEmpty() }) return@withContext true
        val uncovered = fetchable.any { day -> perIdentity.any { ranges -> ranges.none { day in it } } }
        if (uncovered) return@withContext true
        val horizon = canonicalToday(clock).minusDays(CanonicalCacheConstants.CONFIRMED_EMPTY_RECHECK_DAYS)
        val recent = fetchable.filterNot { it.isBefore(horizon) }
        if (recent.isEmpty()) return@withContext false
        daysCoveredByAllIdentities(userId, identities, source, recent, todayTtl).size < recent.size
    }

    suspend fun staleDays(
        userId: String,
        identities: List<String>,
        source: String,
        days: List<LocalDate>,
        todayTtl: Duration,
    ): List<LocalDate> = withContext(io) {
        val window = fetchableWindow()
        val fetchable = days.filter { it in window }
        val covered = daysCoveredByAllIdentities(userId, identities, source, fetchable, todayTtl)
        fetchable.filterNot(covered::contains)
    }

    suspend fun missingDays(
        userId: String,
        identities: List<String>,
        source: String,
        days: List<LocalDate>,
        todayTtl: Duration,
    ): List<LocalDate> = withContext(io) {
        val today = canonicalToday(clock)
        val window = fetchableWindow()
        val fetchable = days.filter { it in window }
        days.count { it.isBefore(window.start) }.let { expired ->
            if (expired > 0) Timber.d(
                "Skipped %d %s day(s) older than the %d-day cache retention",
                expired, source, CanonicalCacheConstants.DAY_RETENTION_DAYS,
            )
        }
        val covered = daysCoveredByAllIdentities(userId, identities, source, fetchable, todayTtl)
        val now = clock.instant()
        val forced = forcedRefreshDays(fetchable, today, lastForcedRefreshAt(userId, source), now)
        if (forced.isNotEmpty()) recordForcedRefresh(userId, source, now)
        val present = covered - forced
        diagnostics.recordLookup(userId, source, present.size, fetchable.size - present.size)
        fetchable.filterNot(present::contains)
    }

    private fun forcedRefreshKey(source: String): String =
        "${CanonicalCacheConstants.FORCED_REFRESH_CURSOR_PREFIX}$source"

    private fun lastForcedRefreshAt(userId: String, source: String): Instant? =
        cache.getSyncCursor(environment, userId, forcedRefreshKey(source))
            ?.toLongOrNull()
            ?.let(Instant::ofEpochMilli)

    private fun recordForcedRefresh(userId: String, source: String, now: Instant) {
        cache.setSyncCursor(environment, userId, forcedRefreshKey(source), now.toEpochMilli().toString())
    }

    suspend fun storeDays(
        userId: String, terraUserId: String, aggregates: List<CanonicalDailyAggregate>,
    ) = withContext(io) {
        if (aggregates.isEmpty()) return@withContext
        aggregates.forEach { cache.putDay(environment, userId, terraUserId, it) }
        aggregates.groupingBy { it.source }.eachCount().forEach { (source, count) ->
            diagnostics.recordWrites(userId, source, count)
        }
        cache.pruneExpiredDays(environment, userId)
        aggregates.map { it.source }.distinct().forEach { _changes.tryEmit(it) }
    }

    suspend fun pruneExpiredDaysOnStartup(userId: String) = withContext(io) {
        cache.pruneExpiredDays(environment, userId)
    }

    fun retentionFloor(): LocalDate = canonicalRetentionFloor(clock)

    suspend fun recordSyncedRange(
        userId: String, terraUserId: String, source: String, window: ClosedRange<LocalDate>,
    ) = withContext(io) {
        val fetchable = fetchableWindow()
        val start = maxOf(window.start, fetchable.start)
        val end = minOf(window.endInclusive, fetchable.endInclusive)
        if (start.isAfter(end)) return@withContext
        cache.extendSyncedRange(environment, userId, terraUserId, source, start..end)
    }

    suspend fun storeEmptyDays(
        userId: String, terraUserId: String, source: String, days: List<LocalDate>,
    ) = withContext(io) {
        if (days.isEmpty()) return@withContext
        val written = cache.putConfirmedEmpty(environment, userId, terraUserId, source, days)
        cache.pruneExpiredDays(environment, userId)
        diagnostics.recordWrites(userId, source, written)
        if (written > 0) _changes.tryEmit(source)
    }

    suspend fun storeScans(userId: String, scans: List<CanonicalBodyScanAggregate>): Unit = withContext(io) {
        if (scans.isEmpty()) return@withContext
        scans.forEach { cache.putScan(environment, userId, it) }
        diagnostics.recordWrites(userId, SOURCE_SCAN, scans.size)
        Timber.i("Health cache stored %d scans", scans.size)
        _changes.tryEmit(SOURCE_SCAN)
    }

    suspend fun replaceScans(userId: String, scans: List<CanonicalBodyScanAggregate>): Unit = withContext(io) {
        cache.replaceScans(environment, userId, scans)
        diagnostics.recordWrites(userId, SOURCE_SCAN, scans.size)
        cache.setSyncCursor(environment, userId, SOURCE_SCAN, System.currentTimeMillis().toString())
        Timber.i("Health cache replaced scan snapshot: %d records", scans.size)
        _changes.tryEmit(SOURCE_SCAN)
    }

    suspend fun scanSnapshotIsFresh(userId: String, ttl: Duration): Boolean = withContext(io) {
        val syncedAt = cache.getSyncCursor(environment, userId, SOURCE_SCAN)?.toLongOrNull()
            ?: return@withContext false
        (System.currentTimeMillis() - syncedAt < ttl.toMillis()).also { fresh ->
            diagnostics.recordLookup(userId, SOURCE_SCAN, if (fresh) 1 else 0, if (fresh) 0 else 1)
        }
    }

    suspend fun storedIdentityOrder(userId: String): List<String> = withContext(io) {
        cache.getSyncCursor(environment, userId, IDENTITY_ORDER_CURSOR)
            ?.split(IDENTITY_ORDER_SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }

    suspend fun rememberIdentityOrder(userId: String, encodedIdentities: List<String>) = withContext(io) {
        val encoded = encodedIdentities.joinToString(IDENTITY_ORDER_SEPARATOR)
        if (cache.getSyncCursor(environment, userId, IDENTITY_ORDER_CURSOR) != encoded) {
            cache.setSyncCursor(environment, userId, IDENTITY_ORDER_CURSOR, encoded)
        }
    }

    val changes: SharedFlow<String> get() = _changes.asSharedFlow()

    private val _changes = MutableSharedFlow<String>(extraBufferCapacity = 16)

    suspend fun scans(userId: String): List<CanonicalBodyScanAggregate> = withContext(io) {
        cache.getScans(environment, userId)
    }

    fun recordProviderCalls(userId: String, source: String, count: Int) =
        diagnostics.recordProviderCalls(userId, source, count)

    suspend fun contentSignature(userId: String): String = withContext(io) {
        cache.contentSignature(environment, userId)
    }

    suspend fun readCursor(userId: String, key: String): String? = withContext(io) {
        cache.getSyncCursor(environment, userId, key)
    }

    suspend fun writeCursor(userId: String, key: String, value: String) = withContext(io) {
        cache.setSyncCursor(environment, userId, key, value)
    }

    suspend fun logStats(userId: String): CanonicalCacheStats = withContext(io) {
        cache.stats(environment, userId).also { stats ->
            val runtime = diagnostics.snapshot(userId).values
            val sources = stats.bySource.entries.joinToString(" ") { (source, v) -> "$source=${v.records}" }
            val fetched = runtime.sumOf { it.providerCalls }
            val misses = runtime.sumOf { it.misses }
            val signature = "$userId|$sources|${stats.total.records}|${stats.total.bytes}|$fetched|$misses"
            if (lastStatsSignature.getAndSet(signature) == signature) return@also
            Timber.i(
                "Health cache: %s | %d rows %.1f MiB | fetched %d, hit %d, miss %d",
                sources,
                stats.total.records,
                stats.total.bytes / BYTES_PER_MIB,
                fetched,
                runtime.sumOf { it.hits },
                misses,
            )
        }
    }

    override suspend fun clearUser(userId: String) = withContext(io) {
        cache.clearUser(environment, userId)
        diagnostics.clearUser(userId)
        lastCoverageSignature.clear()
        lastStatsSignature.set(null)
        _changes.tryEmit(SOURCE_DAILY)
        _changes.tryEmit(SOURCE_SLEEP)
        _changes.tryEmit(SOURCE_SCAN)
        Unit
    }

    private fun logCoverage(
        source: String,
        days: List<LocalDate>,
        coverage: HealthRangeCoverage,
        detail: String,
    ) {
        val key = "$source|${days.first()}|${days.last()}"
        val signature = "${coverage.syncedDays}/${coverage.totalDays}"
        if (lastCoverageSignature.put(key, signature) == signature) return
        Timber.d(
            "Health coverage %s [%s..%s]: %d/%d synced (%s)",
            source, days.first(), days.last(),
            coverage.syncedDays, coverage.totalDays, detail,
        )
    }

    private val lastCoverageSignature = ConcurrentHashMap<String, String>()

    private val lastStatsSignature = AtomicReference<String>()

    private fun String.redacted(): String = if (length <= 8) "***" else "${take(4)}…${takeLast(4)}"

    companion object {
        const val SOURCE_DAILY = "daily"
        const val SOURCE_SLEEP = "sleep"
        const val SOURCE_SCAN = "scan"
        private const val IDENTITY_ORDER_CURSOR = "providers"
        private const val IDENTITY_ORDER_SEPARATOR = "|"
        private const val BYTES_PER_MIB = 1_048_576.0
    }
}

/**
 * The trailing window re-pulled regardless of cache freshness, or empty while throttled. Pure so
 * the window and throttle can be reasoned about without a database.
 */
internal fun forcedRefreshDays(
    days: List<LocalDate>,
    today: LocalDate,
    lastForcedAt: Instant?,
    now: Instant,
    windowDays: Long = CanonicalCacheConstants.FORCED_REFRESH_DAYS,
    interval: Duration = CanonicalCacheConstants.FORCED_REFRESH_INTERVAL,
): Set<LocalDate> {
    if (lastForcedAt != null && !lastForcedAt.isAfter(now) &&
        Duration.between(lastForcedAt, now) < interval
    ) return emptySet()
    val oldest = today.minusDays(windowDays - 1)
    return days.filterTo(mutableSetOf()) { !it.isBefore(oldest) && !it.isAfter(today) }
}

internal data class CanonicalRuntimeStats(
    val hits: Long,
    val misses: Long,
    val writes: Long,
    val providerCalls: Long,
)

internal class CanonicalCacheDiagnostics {
    private data class Counters(
        val hits: AtomicLong = AtomicLong(), val misses: AtomicLong = AtomicLong(),
        val writes: AtomicLong = AtomicLong(), val providerCalls: AtomicLong = AtomicLong(),
    )

    private val values = ConcurrentHashMap<String, Counters>()
    private fun counters(userId: String, source: String) = values.computeIfAbsent("$userId|$source") { Counters() }

    fun recordLookup(userId: String, source: String, hits: Int, misses: Int) {
        counters(userId, source).also { it.hits.addAndGet(hits.toLong()); it.misses.addAndGet(misses.toLong()) }
    }
    fun recordWrites(userId: String, source: String, records: Int) {
        counters(userId, source).writes.addAndGet(records.toLong())
    }
    fun recordProviderCalls(userId: String, source: String, count: Int) {
        counters(userId, source).providerCalls.addAndGet(count.toLong())
    }
    fun snapshot(userId: String): Map<String, CanonicalRuntimeStats> = values.mapNotNull { (key, value) ->
        val prefix = "$userId|"
        if (!key.startsWith(prefix)) return@mapNotNull null
        key.removePrefix(prefix) to CanonicalRuntimeStats(
            value.hits.get(), value.misses.get(),
            value.writes.get(), value.providerCalls.get(),
        )
    }.toMap()
    fun clearUser(userId: String) { values.keys.removeAll { it.startsWith("$userId|") } }
}

internal class CanonicalFetchCoordinator {
    private val guard = Mutex()
    private val locks = mutableMapOf<String, Mutex>()

    suspend fun <T> run(key: String, block: suspend () -> T): T {
        guard.lock()
        val lock = try {
            locks.getOrPut(key) { Mutex() }
        } finally {
            guard.unlock()
        }
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }
}
