package com.hexis.bi.data.health.local

import com.hexis.bi.data.health.model.CANONICAL_HEALTH_AGGREGATE_VERSION
import com.hexis.bi.data.health.model.CanonicalBodyScanAggregate
import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import com.hexis.bi.data.health.model.CanonicalDaySamples
import com.hexis.bi.data.health.model.attachSamples
import com.hexis.bi.data.health.model.detachSamples
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.hexis.bi.utils.constants.CanonicalCacheConstants
import com.hexis.bi.utils.redactSensitiveId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

internal data class CanonicalCacheVolume(val records: Int, val bytes: Long)

internal data class CanonicalCacheStats(
    val bySource: Map<String, CanonicalCacheVolume>,
    val total: CanonicalCacheVolume,
)

internal class HealthAggregateDatabase(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
    private val json: Json = Json { ignoreUnknownKeys = false },
) : SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_DAYS)
        db.execSQL(CREATE_SCANS)
        db.execSQL(CREATE_SYNC)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This DB contains only reproducible cache data. An incompatible canonical schema is
        // safely rebuilt; Firestore/Terra remain the source of truth.
        rebuild(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = rebuild(db)

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        val version = arrayOf(CANONICAL_HEALTH_AGGREGATE_VERSION.toString())
        if (db.delete("canonical_days", "schema_version!=?", version) > 0) {
            db.delete("canonical_sync", "source LIKE ?", arrayOf("$SYNCED_RANGE_PREFIX%"))
        }
        if (db.delete("canonical_scans", "schema_version!=?", version) > 0) {
            db.delete("canonical_sync", "source=?", arrayOf(HealthLocalDataSource.SOURCE_SCAN))
        }
    }

    fun putDay(environment: String, userId: String, terraUserId: String, aggregate: CanonicalDailyAggregate) {
        require(aggregate.schemaVersion == CANONICAL_HEALTH_AGGREGATE_VERSION)
        val (slim, samples) = aggregate.detachSamples()
        putDayRow(
            environment, userId, terraUserId, aggregate.source, aggregate.day,
            json.encodeToString(slim), false,
            if (samples.isEmpty) null else json.encodeToString(samples),
        )
    }

    fun putConfirmedEmpty(
        environment: String,
        userId: String,
        terraUserId: String,
        source: String,
        days: List<LocalDate>,
    ): Int {
        if (days.isEmpty()) return 0
        val holdingData = daysHoldingData(environment, userId, terraUserId, source, days)
        var written = 0
        for (day in days) {
            if (day in holdingData) continue
            putDayRow(environment, userId, terraUserId, source, day.toString(), null, true)
            written++
        }
        if (written < days.size) {
            Timber.i(
                "Health cache kept %d existing %s day(s) the provider stopped returning",
                days.size - written, source,
            )
        }
        return written
    }

    private fun daysHoldingData(
        environment: String,
        userId: String,
        terraUserId: String,
        source: String,
        days: List<LocalDate>,
    ): Set<LocalDate> {
        val placeholders = days.joinToString(",") { "?" }
        val args = arrayOf(environment, userId, terraUserId, source) + days.map { it.toString() }
        return readableDatabase.query(
            "canonical_days", arrayOf("day"),
            "environment=? AND user_id=? AND terra_user_id=? AND source=? " +
                "AND payload IS NOT NULL AND day IN ($placeholders)",
            args, null, null, null,
        ).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(LocalDate.parse(cursor.getString(0))) }
        }
    }

    fun storedDays(
        environment: String,
        userId: String,
        terraUserId: String,
        source: String,
        days: Collection<LocalDate>,
    ): Set<LocalDate> {
        if (days.isEmpty()) return emptySet()
        val wanted = days.toSet()
        return readableDatabase.query(
            "canonical_days", arrayOf("day"),
            "environment=? AND user_id=? AND terra_user_id=? AND source=? " +
                "AND day BETWEEN ? AND ? AND schema_version=?",
            arrayOf(
                environment, userId, terraUserId, source,
                wanted.min().toString(), wanted.max().toString(),
                CANONICAL_HEALTH_AGGREGATE_VERSION.toString(),
            ),
            null, null, null,
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    val day = runCatching { LocalDate.parse(cursor.getString(0)) }.getOrNull() ?: continue
                    if (day in wanted) add(day)
                }
            }
        }
    }

    fun getDays(
        environment: String,
        userId: String,
        terraUserId: String,
        source: String,
        days: Collection<LocalDate>,
        withSamples: Boolean = false,
    ): Map<LocalDate, CanonicalDailyAggregate> {
        if (days.isEmpty()) return emptyMap()
        val wanted = days.toSet()
        val malformed = mutableListOf<Pair<LocalDate, Throwable>>()
        val columns = if (withSamples) arrayOf("day", "payload", "samples") else arrayOf("day", "payload")
        val decoded = readableDatabase.query(
            "canonical_days",
            columns,
            "environment=? AND user_id=? AND terra_user_id=? AND source=? " +
                "AND payload IS NOT NULL AND day BETWEEN ? AND ? AND schema_version=?",
            arrayOf(
                environment, userId, terraUserId, source,
                wanted.min().toString(), wanted.max().toString(),
                CANONICAL_HEALTH_AGGREGATE_VERSION.toString(),
            ),
            null, null, null,
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    val day = runCatching { LocalDate.parse(cursor.getString(0)) }.getOrNull() ?: continue
                    if (day !in wanted) continue
                    runCatching {
                        val slim = json.decodeFromString<CanonicalDailyAggregate>(cursor.getString(1))
                        if (!withSamples || cursor.isNull(2)) slim
                        else slim.attachSamples(json.decodeFromString<CanonicalDaySamples>(cursor.getString(2)))
                    }
                        .onSuccess { put(day, it) }
                        .onFailure { malformed += day to it }
                }
            }
        }
        malformed.forEach { (day, error) ->
            quarantineDay(environment, userId, terraUserId, source, day, error)
        }
        return decoded
    }

    fun freshDays(
        environment: String,
        userId: String,
        terraUserId: String,
        source: String,
        days: Collection<LocalDate>,
        todayTtl: Duration,
        recentRefreshDays: Long = CanonicalCacheConstants.RECENT_REFRESH_DAYS,
        recentTtl: Duration = CanonicalCacheConstants.RECENT_TTL,
    ): Set<LocalDate> {
        if (days.isEmpty()) return emptySet()
        val wanted = days.toSet()
        val now = clock.instant()
        return readableDatabase.query(
            "canonical_days",
            arrayOf("day", "confirmed_empty", "fetched_at_ms"),
            "environment=? AND user_id=? AND terra_user_id=? AND source=? " +
                "AND day BETWEEN ? AND ? AND schema_version=?",
            arrayOf(
                environment, userId, terraUserId, source,
                wanted.min().toString(), wanted.max().toString(),
                CANONICAL_HEALTH_AGGREGATE_VERSION.toString(),
            ),
            null, null, null,
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    val day = runCatching { LocalDate.parse(cursor.getString(0)) }.getOrNull() ?: continue
                    if (day !in wanted) continue
                    val fresh = isCanonicalDayFresh(
                        day, Instant.ofEpochMilli(cursor.getLong(2)), now, todayTtl,
                        recentRefreshDays, recentTtl, cursor.getInt(1) == 1,
                    )
                    if (fresh) add(day)
                }
            }
        }
    }

    fun putScan(environment: String, userId: String, scan: CanonicalBodyScanAggregate) {
        val values = ContentValues().apply {
            put("environment", environment)
            put("user_id", userId)
            put("document_id", scan.documentId)
            put("saved_at", scan.savedAt)
            put("payload", json.encodeToString(scan))
            put("schema_version", CANONICAL_HEALTH_AGGREGATE_VERSION)
            put("fetched_at_ms", clock.millis())
        }
        writableDatabase.insertWithOnConflict("canonical_scans", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun replaceScans(environment: String, userId: String, scans: List<CanonicalBodyScanAggregate>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete(
                "canonical_scans", "environment=? AND user_id=?", arrayOf(environment, userId),
            )
            scans.forEach { putScan(environment, userId, it) }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun getScans(environment: String, userId: String): List<CanonicalBodyScanAggregate> {
        val rows = readableDatabase.query(
            "canonical_scans", arrayOf("document_id", "payload"),
            "environment=? AND user_id=? AND schema_version=?",
            arrayOf(environment, userId, CANONICAL_HEALTH_AGGREGATE_VERSION.toString()),
            null, null, "saved_at ASC",
        ).use { cursor -> buildList {
            while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1))
        } }
        val invalid = mutableListOf<String>()
        val decoded = rows.mapNotNull { (id, payload) ->
            runCatching { json.decodeFromString<CanonicalBodyScanAggregate>(payload) }
                .onFailure { Timber.e(it, "Health cache quarantined malformed scan id=%s", id) }
                .getOrNull() ?: run { invalid += id; null }
        }
        invalid.forEach { id ->
            writableDatabase.delete(
                "canonical_scans", "environment=? AND user_id=? AND document_id=?",
                arrayOf(environment, userId, id),
            )
        }
        if (invalid.isNotEmpty()) {
            writableDatabase.delete(
                "canonical_sync", "environment=? AND user_id=? AND source=?",
                arrayOf(environment, userId, "scan"),
            )
        }
        return decoded
    }

    fun getSyncedRanges(
        environment: String,
        userId: String,
        terraUserId: String,
        source: String,
    ): List<ClosedRange<LocalDate>> =
        getSyncCursor(environment, userId, syncedRangeKey(source, terraUserId))
            ?.split(SYNCED_RANGE_LIST_SEPARATOR)
            ?.mapNotNull(::parseSyncedRange)
            ?.sortedBy { it.start }
            .orEmpty()

    fun extendSyncedRange(
        environment: String,
        userId: String,
        terraUserId: String,
        source: String,
        window: ClosedRange<LocalDate>,
    ) {
        val existing = getSyncedRanges(environment, userId, terraUserId, source)
        if (existing.any {
                !window.start.isBefore(it.start) && !window.endInclusive.isAfter(it.endInclusive)
            }
        ) {
            return
        }
        val merged = mergeDateRanges(existing + window)
        Timber.d(
            "Health sync range %s/%s: %d range(s), %d day(s) total, widest [%s..%s], after window [%s..%s]",
            source, redactSensitiveId(terraUserId), merged.size,
            merged.sumOf { ChronoUnit.DAYS.between(it.start, it.endInclusive) + 1 },
            merged.first().start, merged.last().endInclusive,
            window.start, window.endInclusive,
        )
        setSyncCursor(
            environment, userId, syncedRangeKey(source, terraUserId),
            merged.joinToString(SYNCED_RANGE_LIST_SEPARATOR) {
                "${it.start}$SYNCED_RANGE_SEPARATOR${it.endInclusive}"
            },
        )
    }

    private fun parseSyncedRange(encoded: String): ClosedRange<LocalDate>? {
        val separator = encoded.indexOf(SYNCED_RANGE_SEPARATOR)
        if (separator < 0) return null
        return runCatching {
            LocalDate.parse(encoded.substring(0, separator))..
                LocalDate.parse(encoded.substring(separator + SYNCED_RANGE_SEPARATOR.length))
        }.getOrNull()?.takeIf { !it.start.isAfter(it.endInclusive) }
    }

    private fun syncedRangeKey(source: String, terraUserId: String) =
        "$SYNCED_RANGE_PREFIX$source:$terraUserId"

    fun setSyncCursor(environment: String, userId: String, source: String, cursor: String) {
        val values = ContentValues().apply {
            put("environment", environment); put("user_id", userId); put("source", source)
            put("cursor_value", cursor); put("updated_at_ms", clock.millis())
        }
        writableDatabase.insertWithOnConflict("canonical_sync", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getSyncCursor(environment: String, userId: String, source: String): String? =
        readableDatabase.query(
            "canonical_sync", arrayOf("cursor_value"),
            "environment=? AND user_id=? AND source=?", arrayOf(environment, userId, source),
            null, null, null,
        ).use { if (it.moveToFirst()) it.getString(0) else null }

    fun clearUser(environment: String, userId: String) {
        writableDatabase.beginTransaction()
        try {
            val args = arrayOf(environment, userId)
            writableDatabase.delete("canonical_days", "environment=? AND user_id=?", args)
            writableDatabase.delete("canonical_scans", "environment=? AND user_id=?", args)
            writableDatabase.delete("canonical_sync", "environment=? AND user_id=?", args)
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun pruneExpiredDays(environment: String, userId: String): Int {
        val removed = writableDatabase.delete(
            "canonical_days", "environment=? AND user_id=? AND day<?",
            arrayOf(environment, userId, canonicalRetentionFloor(clock).toString()),
        )
        if (removed > 0) Timber.i("Health cache pruned %d expired day row(s)", removed)
        return removed
    }

    fun stats(environment: String, userId: String): CanonicalCacheStats {
        val volumes = linkedMapOf<String, CanonicalCacheVolume>()
        readableDatabase.rawQuery(
            """SELECT source, COUNT(*), COALESCE(SUM(LENGTH(payload)),0)
               FROM canonical_days WHERE environment=? AND user_id=? GROUP BY source""",
            arrayOf(environment, userId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                volumes[cursor.getString(0)] = CanonicalCacheVolume(cursor.getInt(1), cursor.getLong(2))
            }
        }
        readableDatabase.rawQuery(
            """SELECT COUNT(*), COALESCE(SUM(LENGTH(payload)),0)
               FROM canonical_scans WHERE environment=? AND user_id=?""",
            arrayOf(environment, userId),
        ).use { cursor ->
            if (cursor.moveToFirst()) volumes["scan"] = CanonicalCacheVolume(cursor.getInt(0), cursor.getLong(1))
        }
        return CanonicalCacheStats(
            bySource = volumes,
            total = CanonicalCacheVolume(volumes.values.sumOf { it.records }, volumes.values.sumOf { it.bytes }),
        )
    }

    private fun putDayRow(
        environment: String, userId: String, terraUserId: String, source: String, day: String,
        payload: String?, confirmedEmpty: Boolean, samples: String? = null,
    ) {
        val values = ContentValues().apply {
            put("environment", environment); put("user_id", userId)
            put("terra_user_id", terraUserId)
            put("source", source); put("day", day)
            if (payload == null) putNull("payload") else put("payload", payload)
            if (samples == null) putNull("samples") else put("samples", samples)
            put("confirmed_empty", if (confirmedEmpty) 1 else 0)
            put("schema_version", CANONICAL_HEALTH_AGGREGATE_VERSION)
            put("fetched_at_ms", clock.millis())
        }
        writableDatabase.insertWithOnConflict("canonical_days", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun quarantineDay(
        environment: String, userId: String, terraUserId: String,
        source: String, day: LocalDate, error: Throwable,
    ) {
        Timber.e(error, "Health cache quarantined malformed %s day=%s", source, day)
        writableDatabase.delete(
            "canonical_days",
            "environment=? AND user_id=? AND terra_user_id=? AND source=? AND day=?",
            arrayOf(environment, userId, terraUserId, source, day.toString()),
        )
        val remaining = removeDayFromRanges(
            getSyncedRanges(environment, userId, terraUserId, source), day,
        )
        setSyncCursor(
            environment, userId, syncedRangeKey(source, terraUserId),
            remaining.joinToString(SYNCED_RANGE_LIST_SEPARATOR) {
                "${it.start}$SYNCED_RANGE_SEPARATOR${it.endInclusive}"
            },
        )
    }

    private fun rebuild(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS canonical_days")
        db.execSQL("DROP TABLE IF EXISTS canonical_scans")
        db.execSQL("DROP TABLE IF EXISTS canonical_sync")
        onCreate(db)
    }

    private companion object {
        const val SYNCED_RANGE_PREFIX = "range:"
        const val SYNCED_RANGE_SEPARATOR = ".."
        const val SYNCED_RANGE_LIST_SEPARATOR = ","
        const val DATABASE_NAME = "canonical_health_cache.db"
        const val DATABASE_VERSION = 8
        const val CREATE_DAYS = """CREATE TABLE canonical_days (
            environment TEXT NOT NULL, user_id TEXT NOT NULL, terra_user_id TEXT NOT NULL,
            source TEXT NOT NULL, day TEXT NOT NULL,
            payload TEXT, confirmed_empty INTEGER NOT NULL, schema_version INTEGER NOT NULL,
            fetched_at_ms INTEGER NOT NULL, samples TEXT,
            PRIMARY KEY(environment,user_id,terra_user_id,source,day),
            CHECK ((confirmed_empty=1 AND payload IS NULL) OR (confirmed_empty=0 AND payload IS NOT NULL)))"""
        const val CREATE_SCANS = """CREATE TABLE canonical_scans (
            environment TEXT NOT NULL, user_id TEXT NOT NULL, document_id TEXT NOT NULL, saved_at TEXT NOT NULL,
            payload TEXT NOT NULL, schema_version INTEGER NOT NULL, fetched_at_ms INTEGER NOT NULL,
            PRIMARY KEY(environment,user_id,document_id))"""
        const val CREATE_SYNC = """CREATE TABLE canonical_sync (
            environment TEXT NOT NULL, user_id TEXT NOT NULL, source TEXT NOT NULL, cursor_value TEXT NOT NULL,
            updated_at_ms INTEGER NOT NULL, PRIMARY KEY(environment,user_id,source))"""
    }
}

internal fun isCanonicalDayFresh(
    day: LocalDate,
    fetchedAt: Instant,
    now: Instant,
    todayTtl: Duration,
    recentRefreshDays: Long = CanonicalCacheConstants.RECENT_REFRESH_DAYS,
    recentTtl: Duration = CanonicalCacheConstants.RECENT_TTL,
    confirmedEmpty: Boolean = false,
): Boolean {
    val today = LocalDate.ofInstant(now, ZoneOffset.UTC)
    if (day > today) return false
    val ageDays = java.time.temporal.ChronoUnit.DAYS.between(day, today)
    val ttl = when {
        ageDays == 0L -> todayTtl
        ageDays <= recentRefreshDays -> recentTtl
        confirmedEmpty && ageDays <= CanonicalCacheConstants.CONFIRMED_EMPTY_RECHECK_DAYS -> recentTtl
        else -> return true
    }
    return Duration.between(fetchedAt, now) < ttl
}

internal fun mergeDateRanges(ranges: List<ClosedRange<LocalDate>>): List<ClosedRange<LocalDate>> {
    val sorted = ranges.filterNot { it.start.isAfter(it.endInclusive) }.sortedBy { it.start }
    if (sorted.isEmpty()) return emptyList()
    val merged = mutableListOf<ClosedRange<LocalDate>>()
    var current = sorted.first()
    for (next in sorted.drop(1)) {
        current = if (!next.start.isAfter(current.endInclusive.plusDays(1))) {
            current.start..maxOf(current.endInclusive, next.endInclusive)
        } else {
            merged += current
            next
        }
    }
    merged += current
    return merged
}

internal fun removeDayFromRanges(
    ranges: List<ClosedRange<LocalDate>>,
    day: LocalDate,
): List<ClosedRange<LocalDate>> = ranges.flatMap { range ->
    if (day !in range) listOf(range) else listOfNotNull(
        (range.start..day.minusDays(1)).takeIf { !it.start.isAfter(it.endInclusive) },
        (day.plusDays(1)..range.endInclusive).takeIf { !it.start.isAfter(it.endInclusive) },
    )
}
