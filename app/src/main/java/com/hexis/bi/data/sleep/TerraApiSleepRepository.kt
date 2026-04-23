package com.hexis.bi.data.sleep

import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import com.hexis.bi.data.terra.TerraApi
import com.hexis.bi.data.terra.TerraSleepApiResponse
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Single sleep repository for every provider. Picks the most recently added
 * active connection from Firestore (Health Connect, wearable, DUMMY) and pulls
 * its sleep data through Terra's `/v2/sleep` endpoint.
 *
 * Returns `success(null)` when the user has no connection yet, so the UI renders
 * zeros instead of an error.
 */
class TerraApiSleepRepository(
    private val api: TerraApi,
    private val healthConnections: HealthConnectionsRepository,
) : TerraSleepRepository {

    override suspend fun getSessionForNight(date: LocalDate): Result<TerraSleepSession?> =
        getSessionsForRange(date.minusDays(1), date).map { sessions ->
            sessions.firstOrNull { it.wakeTime.toLocalDate() == date }
                ?: sessions.maxByOrNull { it.wakeTime }
        }

    override suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<TerraSleepSession>> {
        val connection = healthConnections.getConnections().getOrNull()
            ?.filter { it.active }
            ?.maxByOrNull { it.connectedAt?.toDate()?.time ?: 0L }
            ?: return Result.success(emptyList())

        Timber.d(
            "Terra /sleep request user_id=%s source=%s range=[%s..%s]",
            connection.terraUserId, connection.provider, start, end,
        )

        val response = api.getSleep(
            terraUserId = connection.terraUserId,
            startDate = start,
            endDate = end,
        ).getOrElse {
            Timber.e(
                it, "Terra /sleep failed user=%s [%s..%s]",
                connection.terraUserId.short(), start, end,
            )
            return Result.failure(it)
        }

        val sessions = response.data.mapNotNull { it.toSleepSession() }
        if (sessions.isEmpty()) {
            Timber.d("Terra /sleep returned no sessions for [%s..%s]", start, end)
        }
        return Result.success(sessions)
    }

    private fun JsonElement.toSleepSession(): TerraSleepSession? {
        val obj = this as? JsonObject ?: return null
        val metadata = obj["metadata"]?.jsonObject ?: return null
        val bedtime = metadata.parseTime("start_time") ?: run {
            Timber.w("Terra sleep: missing/unparsed start_time in metadata")
            return null
        }
        val wakeTime = metadata.parseTime("end_time") ?: run {
            Timber.w("Terra sleep: missing/unparsed end_time in metadata")
            return null
        }

        val durations = obj["sleep_durations_data"]?.jsonObject
        val asleep = durations?.get("asleep")?.jsonObject
        val inBed = durations?.get("in_bed")?.jsonObject
        val other = durations?.get("other")?.jsonObject

        val spanSeconds = java.time.Duration.between(bedtime, wakeTime).seconds.coerceAtLeast(1)
        var durationSec = inBed?.float("duration_in_bed_seconds")
            ?: other?.float("duration_in_bed_seconds")
            ?: asleep?.float("duration_asleep_state_seconds")
            ?: spanSeconds.toFloat()

        // Data generator sometimes emits bogus tiny second values; prefer wall-clock span.
        if (durationSec < 300f && spanSeconds >= 300) {
            durationSec = spanSeconds.toFloat()
        }
        val durationMinutes = (durationSec / 60f).toInt().coerceAtLeast(1)

        val efficiency = durations?.float("sleep_efficiency")
            ?: obj["efficiency_data"]?.jsonObject?.float("sleep_efficiency")
            ?: 0f

        val heartRate = obj["heart_rate_data"]?.jsonObject?.get("summary")?.jsonObject
        val restingHr = heartRate?.int("resting_hr_bpm")
            ?: heartRate?.int("avg_hr_bpm")
            ?: 0
        val hrvMs = heartRate?.float("avg_hrv_rmssd")?.toInt() ?: 0

        return TerraSleepSession(
            bedtime = bedtime,
            wakeTime = wakeTime,
            durationMinutes = durationMinutes,
            efficiencyPercent = efficiency,
            restingHeartRateBpm = restingHr,
            hrvMs = hrvMs,
            stages = parseStages(obj),
        )
    }

    private fun parseStages(root: JsonObject): List<TerraSleepStageInterval> {
        val stageData = root["sleep_stage_data"]?.jsonObject ?: return emptyList()
        return buildList {
            addAll(stageData.parsePeriods("light_periods", TerraSleepStage.Light))
            addAll(stageData.parsePeriods("deep_periods", TerraSleepStage.Deep))
            addAll(stageData.parsePeriods("rem_periods", TerraSleepStage.REM))
            addAll(stageData.parsePeriods("wake_periods", TerraSleepStage.Awake))
        }.sortedBy { it.start }
    }

    private fun JsonObject.parsePeriods(
        key: String,
        stage: TerraSleepStage,
    ): List<TerraSleepStageInterval> {
        val arr = this[key]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val start = o.parseTime("start") ?: return@mapNotNull null
            val end = o.parseTime("end") ?: return@mapNotNull null
            TerraSleepStageInterval(stage = stage, start = start, end = end)
        }
    }

}

private fun JsonObject.parseTime(key: String): LocalDateTime? {
    val raw = this[key]?.jsonPrimitive?.contentOrNullSafe()?.trim() ?: return null
    return parseTerraDateTime(raw)
}

/** Terra returns ISO-like strings with or without offset and with variable fractional seconds. */
private fun parseTerraDateTime(raw: String): LocalDateTime? =
    runCatching { OffsetDateTime.parse(raw).toLocalDateTime() }
        .recoverCatching { LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        .recoverCatching { LocalDateTime.parse(raw) }
        .recoverCatching {
            val withZ = when {
                raw.endsWith('Z') -> raw
                Regex("[+-]\\d{2}:?\\d{2}$").containsMatchIn(raw) -> raw
                else -> raw + 'Z'
            }
            Instant.parse(withZ).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }
        .onFailure { if (it is DateTimeParseException) Timber.w(it, "parseTerraDateTime: %s", raw) }
        .getOrNull()

private fun JsonObject.float(key: String): Float? =
    this[key]?.jsonPrimitive?.floatOrNull

private fun JsonObject.int(key: String): Int? =
    this[key]?.jsonPrimitive?.intOrNull

private fun JsonPrimitive.contentOrNullSafe(): String? =
    if (isString) content else content.takeIf { it.isNotBlank() }

private fun String.short(): String =
    if (length <= 8) "***" else "${take(4)}…${takeLast(4)}"
