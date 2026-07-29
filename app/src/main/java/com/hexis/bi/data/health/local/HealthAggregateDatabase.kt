package com.hexis.bi.data.health.local

import com.hexis.bi.data.health.model.CANONICAL_HEALTH_AGGREGATE_VERSION
import com.hexis.bi.data.health.model.CanonicalBodyScanAggregate
import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.hexis.bi.utils.constants.CanonicalCacheConstants
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

internal data class CachedCanonicalDay(
    val aggregate: CanonicalDailyAggregate?,
    val confirmedEmpty: Boolean,
    val fetchedAt: Instant,
) {
    init {
        require(confirmedEmpty.xor(aggregate != null)) {
            "A cached day must contain exactly one of aggregate or confirmed-empty"
        }
    }
}

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
        db.delete("canonical_days", "schema_version!=?", arrayOf(CANONICAL_HEALTH_AGGREGATE_VERSION.toString()))
        db.delete("canonical_scans", "schema_version!=?", arrayOf(CANONICAL_HEALTH_AGGREGATE_VERSION.toString()))
    }

    fun putDay(environment: String, userId: String, fingerprint: String, aggregate: CanonicalDailyAggregate) {
        require(aggregate.schemaVersion == CANONICAL_HEALTH_AGGREGATE_VERSION)
        putDayRow(
            environment, userId, fingerprint, aggregate.source, aggregate.day,
            json.encodeToString(aggregate), false,
        )
    }

    fun putConfirmedEmpty(
        environment: String,
        userId: String,
        fingerprint: String,
        source: String,
        days: List<LocalDate>,
    ): Int {
        if (days.isEmpty()) return 0
        val holdingData = daysHoldingData(environment, userId, fingerprint, source, days)
        var written = 0
        for (day in days) {
            if (day in holdingData) continue
            putDayRow(environment, userId, fingerprint, source, day.toString(), null, true)
            written++
        }
        if (written < days.size) {
            Timber.i(
                "Canonical cache kept %d existing %s day(s) the provider stopped returning",
                days.size - written, source,
            )
        }
        return written
    }

    private fun daysHoldingData(
        environment: String,
        userId: String,
        fingerprint: String,
        source: String,
        days: List<LocalDate>,
    ): Set<LocalDate> {
        val placeholders = days.joinToString(",") { "?" }
        val args = arrayOf(environment, userId, fingerprint, source) + days.map { it.toString() }
        return readableDatabase.query(
            "canonical_days", arrayOf("day"),
            "environment=? AND user_id=? AND provider_fingerprint=? AND source=? " +
                "AND payload IS NOT NULL AND day IN ($placeholders)",
            args, null, null, null,
        ).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(LocalDate.parse(cursor.getString(0))) }
        }
    }

    fun getDay(
        environment: String,
        userId: String,
        fingerprint: String,
        source: String,
        day: LocalDate,
    ): CachedCanonicalDay? {
        val row = readableDatabase.query(
            "canonical_days",
            arrayOf("payload", "confirmed_empty", "fetched_at_ms", "schema_version", "provider_fingerprint"),
            "environment=? AND user_id=? AND provider_fingerprint=? AND source=? AND day=?",
            arrayOf(environment, userId, fingerprint, source, day.toString()), null, null, null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else DayRow(
                payload = if (cursor.isNull(0)) null else cursor.getString(0),
                confirmedEmpty = cursor.getInt(1) == 1,
                fetchedAtMs = cursor.getLong(2),
                schemaVersion = cursor.getInt(3),
                fingerprint = cursor.getString(4),
            )
        } ?: return null
        return runCatching {
            require(row.schemaVersion == CANONICAL_HEALTH_AGGREGATE_VERSION)
            CachedCanonicalDay(
                aggregate = row.payload?.let { json.decodeFromString<CanonicalDailyAggregate>(it) },
                confirmedEmpty = row.confirmedEmpty,
                fetchedAt = Instant.ofEpochMilli(row.fetchedAtMs),
            )
        }.getOrElse { error ->
            quarantineDay(environment, userId, row.fingerprint, source, day, error)
            null
        }
    }

    fun latestPartition(environment: String, userId: String, source: String): String? =
        readableDatabase.query(
            "canonical_days", arrayOf("provider_fingerprint"),
            "environment=? AND user_id=? AND source=?", arrayOf(environment, userId, source),
            null, null, "fetched_at_ms DESC", "1",
        ).use { if (it.moveToFirst()) it.getString(0) else null }

    fun getFreshDays(
        environment: String,
        userId: String,
        fingerprint: String,
        source: String,
        days: Iterable<LocalDate>,
        todayTtl: Duration,
        recentRefreshDays: Long = CanonicalCacheConstants.RECENT_REFRESH_DAYS,
        recentTtl: Duration = CanonicalCacheConstants.RECENT_TTL,
    ): Map<LocalDate, CachedCanonicalDay> = days.mapNotNull { day ->
        val cached = getDay(environment, userId, fingerprint, source, day) ?: return@mapNotNull null
        val fresh = isCanonicalDayFresh(
            day, cached.fetchedAt, clock.instant(), todayTtl,
            recentRefreshDays, recentTtl, cached.confirmedEmpty,
        )
        if (fresh) day to cached else null
    }.toMap()

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
                .onFailure { Timber.e(it, "Canonical cache quarantined malformed scan id=%s", id) }
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

    fun pruneExpired(environment: String, userId: String, source: String, keepPartition: String): Int {
        val now = clock.instant()
        val partitionCutoff = now.minus(CanonicalCacheConstants.PARTITION_RETENTION).toEpochMilli()
        var removed = deleteDaysOlderThanRetention(environment, userId)
        removed += writableDatabase.delete(
            "canonical_days",
            "environment=? AND user_id=? AND source=? AND provider_fingerprint!=? AND fetched_at_ms<?",
            arrayOf(environment, userId, source, keepPartition, partitionCutoff.toString()),
        )
        if (removed > 0) Timber.i("Canonical cache pruned %d expired %s row(s)", removed, source)
        return removed
    }

    fun pruneExpiredDays(environment: String, userId: String): Int {
        val removed = deleteDaysOlderThanRetention(environment, userId)
        if (removed > 0) Timber.i("Canonical cache startup sweep pruned %d expired day row(s)", removed)
        return removed
    }

    private fun deleteDaysOlderThanRetention(environment: String, userId: String): Int =
        writableDatabase.delete(
            "canonical_days", "environment=? AND user_id=? AND day<?",
            arrayOf(environment, userId, canonicalRetentionFloor(clock).toString()),
        )

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
        environment: String, userId: String, fingerprint: String, source: String, day: String,
        payload: String?, confirmedEmpty: Boolean,
    ) {
        val values = ContentValues().apply {
            put("environment", environment); put("user_id", userId)
            put("provider_fingerprint", fingerprint)
            put("source", source); put("day", day)
            if (payload == null) putNull("payload") else put("payload", payload)
            put("confirmed_empty", if (confirmedEmpty) 1 else 0)
            put("schema_version", CANONICAL_HEALTH_AGGREGATE_VERSION)
            put("fetched_at_ms", clock.millis())
        }
        writableDatabase.insertWithOnConflict("canonical_days", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun quarantineDay(
        environment: String, userId: String, fingerprint: String,
        source: String, day: LocalDate, error: Throwable,
    ) {
        Timber.e(error, "Canonical cache quarantined malformed %s day=%s", source, day)
        writableDatabase.delete(
            "canonical_days",
            "environment=? AND user_id=? AND provider_fingerprint=? AND source=? AND day=?",
            arrayOf(environment, userId, fingerprint, source, day.toString()),
        )
    }

    private fun rebuild(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS canonical_days")
        db.execSQL("DROP TABLE IF EXISTS canonical_scans")
        db.execSQL("DROP TABLE IF EXISTS canonical_sync")
        onCreate(db)
    }

    private data class DayRow(
        val payload: String?, val confirmedEmpty: Boolean,
        val fetchedAtMs: Long, val schemaVersion: Int, val fingerprint: String,
    )

    private companion object {
        const val DATABASE_NAME = "canonical_health_cache.db"
        const val DATABASE_VERSION = 5
        const val CREATE_DAYS = """CREATE TABLE canonical_days (
            environment TEXT NOT NULL, user_id TEXT NOT NULL, provider_fingerprint TEXT NOT NULL,
            source TEXT NOT NULL, day TEXT NOT NULL,
            payload TEXT, confirmed_empty INTEGER NOT NULL, schema_version INTEGER NOT NULL,
            fetched_at_ms INTEGER NOT NULL,
            PRIMARY KEY(environment,user_id,provider_fingerprint,source,day),
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
