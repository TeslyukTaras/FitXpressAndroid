package com.hexis.bi.data.health.local

import com.hexis.bi.data.health.model.CanonicalBodyScanAggregate
import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import com.hexis.bi.utils.constants.CanonicalCacheConstants
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
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

internal fun canonicalRetentionFloor(clock: Clock): LocalDate =
    LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC)
        .minusDays(CanonicalCacheConstants.DAY_RETENTION_DAYS)

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
    suspend fun aggregates(
        userId: String,
        fingerprint: String,
        source: String,
        days: List<LocalDate>,
        todayTtl: Duration,
    ): Map<LocalDate, CanonicalDailyAggregate> =
        readDays(userId, fingerprint, source, days, todayTtl).mapNotNull { (day, cached) ->
            cached.aggregate?.let { day to it }
        }.toMap()

    /** Every cached day regardless of freshness; [fingerprint] null falls back to the newest partition. */
    suspend fun allAggregates(
        userId: String,
        source: String,
        days: List<LocalDate>,
        fingerprint: String? = null,
    ): Map<LocalDate, CanonicalDailyAggregate> =
        readAnyDays(userId, fingerprint, source, days).mapNotNull { (day, cached) ->
            cached.aggregate?.let { day to it }
        }.toMap()

    private fun fetchableWindow(): ClosedRange<LocalDate> =
        canonicalRetentionFloor(clock)..LocalDate.now(clock)

    suspend fun staleDays(
        userId: String,
        fingerprint: String,
        source: String,
        days: List<LocalDate>,
        todayTtl: Duration,
    ): List<LocalDate> = withContext(io) {
        val window = fetchableWindow()
        val fetchable = days.filter { it in window }
        val fresh = cache.getFreshDays(environment, userId, fingerprint, source, fetchable, todayTtl).keys
        fetchable.filterNot(fresh::contains)
    }

    suspend fun missingDays(
        userId: String,
        fingerprint: String,
        source: String,
        days: List<LocalDate>,
        todayTtl: Duration,
    ): List<LocalDate> = withContext(io) {
        val today = LocalDate.now(clock)
        val window = fetchableWindow()
        val fetchable = days.filter { it in window }
        days.count { it.isBefore(window.start) }.let { expired ->
            if (expired > 0) Timber.d(
                "Skipped %d %s day(s) older than the %d-day cache retention",
                expired, source, CanonicalCacheConstants.DAY_RETENTION_DAYS,
            )
        }
        val cachedFresh = cache.getFreshDays(environment, userId, fingerprint, source, fetchable, todayTtl).keys
        val now = clock.instant()
        val key = "$userId|$source"
        val forced = forcedRefreshDays(fetchable, today, lastForcedRefresh[key], now)
        if (forced.isNotEmpty()) lastForcedRefresh[key] = now
        val present = cachedFresh - forced
        diagnostics.recordLookup(userId, source, present.size, fetchable.size - present.size)
        fetchable.filterNot(present::contains)
    }

    suspend fun storeDays(
        userId: String, fingerprint: String, aggregates: List<CanonicalDailyAggregate>,
    ) = withContext(io) {
        aggregates.forEach { cache.putDay(environment, userId, fingerprint, it) }
        aggregates.groupingBy { it.source }.eachCount().forEach { (source, count) ->
            diagnostics.recordWrites(userId, source, count)
        }
        aggregates.map { it.source }.distinct().forEach { source ->
            cache.pruneExpired(environment, userId, source, fingerprint)
        }
        Timber.i("Canonical cache wrote %d aggregate days for user=%s", aggregates.size, userId.redacted())
        aggregates.map { it.source }.distinct().forEach { _changes.tryEmit(it) }
    }

    suspend fun pruneExpiredDaysOnStartup(userId: String) = withContext(io) {
        cache.pruneExpiredDays(environment, userId)
    }

    suspend fun storeEmptyDays(
        userId: String, fingerprint: String, source: String, days: List<LocalDate>,
    ) = withContext(io) {
        val written = cache.putConfirmedEmpty(environment, userId, fingerprint, source, days)
        cache.pruneExpired(environment, userId, source, fingerprint)
        diagnostics.recordWrites(userId, source, written)
        Timber.i("Canonical cache wrote %d confirmed-empty %s days", written, source)
        if (written > 0) _changes.tryEmit(source)
    }

    suspend fun storeScans(userId: String, scans: List<CanonicalBodyScanAggregate>) = withContext(io) {
        scans.forEach { cache.putScan(environment, userId, it) }
        diagnostics.recordWrites(userId, SOURCE_SCAN, scans.size)
        Timber.i("Canonical cache wrote %d scans for user=%s", scans.size, userId.redacted())
    }

    suspend fun replaceScans(userId: String, scans: List<CanonicalBodyScanAggregate>) = withContext(io) {
        cache.replaceScans(environment, userId, scans)
        diagnostics.recordWrites(userId, SOURCE_SCAN, scans.size)
        cache.setSyncCursor(environment, userId, SOURCE_SCAN, System.currentTimeMillis().toString())
        Timber.i("Canonical cache replaced scan snapshot: %d records", scans.size)
    }

    suspend fun scanSnapshotIsFresh(userId: String, ttl: Duration): Boolean = withContext(io) {
        val syncedAt = cache.getSyncCursor(environment, userId, SOURCE_SCAN)?.toLongOrNull()
            ?: return@withContext false
        (System.currentTimeMillis() - syncedAt < ttl.toMillis()).also { fresh ->
            diagnostics.recordLookup(userId, SOURCE_SCAN, if (fresh) 1 else 0, if (fresh) 0 else 1)
        }
    }

    suspend fun storedFingerprint(userId: String): String? = withContext(io) {
        cache.getSyncCursor(environment, userId, PROVIDER_FINGERPRINT_CURSOR)
    }

    suspend fun rememberFingerprint(userId: String, fingerprint: String) = withContext(io) {
        if (cache.getSyncCursor(environment, userId, PROVIDER_FINGERPRINT_CURSOR) != fingerprint) {
            cache.setSyncCursor(environment, userId, PROVIDER_FINGERPRINT_CURSOR, fingerprint)
        }
    }

    val changes: SharedFlow<String> get() = _changes.asSharedFlow()

    private val _changes = MutableSharedFlow<String>(extraBufferCapacity = 16)

    suspend fun scans(userId: String): List<CanonicalBodyScanAggregate> = withContext(io) {
        cache.getScans(environment, userId)
    }

    fun recordProviderCalls(userId: String, source: String, count: Int) =
        diagnostics.recordProviderCalls(userId, source, count)

    fun recordStaleFallback(userId: String, source: String, records: Int) =
        diagnostics.recordStale(userId, source, records)

    suspend fun logStats(userId: String): CanonicalCacheStats = withContext(io) {
        cache.stats(environment, userId).also { stats ->
            stats.bySource.forEach { (source, volume) ->
                Timber.i(
                    "Canonical cache %-5s: %d records, %.4f MiB",
                    source, volume.records, volume.bytes / BYTES_PER_MIB,
                )
            }
            Timber.i(
                "Canonical cache TOTAL: %d records, %.4f MiB",
                stats.total.records, stats.total.bytes / BYTES_PER_MIB,
            )
            diagnostics.snapshot(userId).forEach { (source, runtime) ->
                Timber.i(
                    "Canonical runtime %-5s: hits=%d misses=%d stale=%d writes=%d provider_calls=%d",
                    source, runtime.hits, runtime.misses, runtime.staleRecords,
                    runtime.writes, runtime.providerCalls,
                )
            }
        }
    }

    override suspend fun clearUser(userId: String) = withContext(io) {
        cache.clearUser(environment, userId)
        diagnostics.clearUser(userId)
    }

    private suspend fun readDays(
        userId: String,
        fingerprint: String,
        source: String,
        days: List<LocalDate>,
        todayTtl: Duration,
    ): Map<LocalDate, CachedCanonicalDay> = withContext(io) {
        cache.getFreshDays(environment, userId, fingerprint, source, days, todayTtl)
    }

    private suspend fun readAnyDays(
        userId: String, fingerprint: String?, source: String, days: List<LocalDate>,
    ): Map<LocalDate, CachedCanonicalDay> = withContext(io) {
        val partition = fingerprint
            ?: cache.latestPartition(environment, userId, source)
            ?: return@withContext emptyMap()
        days.mapNotNull { day ->
            cache.getDay(environment, userId, partition, source, day)?.let { day to it }
        }.toMap()
    }

    private val lastForcedRefresh = ConcurrentHashMap<String, Instant>()

    private fun String.redacted(): String = if (length <= 8) "***" else "${take(4)}…${takeLast(4)}"

    companion object {
        const val SOURCE_DAILY = "daily"
        const val SOURCE_SLEEP = "sleep"
        const val SOURCE_SCAN = "scan"
        private const val PROVIDER_FINGERPRINT_CURSOR = "providers"
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
    if (lastForcedAt != null && Duration.between(lastForcedAt, now) < interval) return emptySet()
    val oldest = today.minusDays(windowDays - 1)
    return days.filterTo(mutableSetOf()) { !it.isBefore(oldest) && !it.isAfter(today) }
}

internal data class CanonicalRuntimeStats(
    val hits: Long,
    val misses: Long,
    val staleRecords: Long,
    val writes: Long,
    val providerCalls: Long,
)

internal class CanonicalCacheDiagnostics {
    private data class Counters(
        val hits: AtomicLong = AtomicLong(), val misses: AtomicLong = AtomicLong(),
        val stale: AtomicLong = AtomicLong(), val writes: AtomicLong = AtomicLong(),
        val providerCalls: AtomicLong = AtomicLong(),
    )

    private val values = ConcurrentHashMap<String, Counters>()
    private fun counters(userId: String, source: String) = values.computeIfAbsent("$userId|$source") { Counters() }

    fun recordLookup(userId: String, source: String, hits: Int, misses: Int) {
        counters(userId, source).also { it.hits.addAndGet(hits.toLong()); it.misses.addAndGet(misses.toLong()) }
    }
    fun recordStale(userId: String, source: String, records: Int) {
        counters(userId, source).stale.addAndGet(records.toLong())
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
            value.hits.get(), value.misses.get(), value.stale.get(),
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
