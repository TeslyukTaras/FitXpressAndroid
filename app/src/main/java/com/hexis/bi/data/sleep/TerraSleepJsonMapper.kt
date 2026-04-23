package com.hexis.bi.data.sleep

import com.hexis.bi.data.terra.float
import com.hexis.bi.data.terra.int
import com.hexis.bi.data.terra.parseTerraDateTimeField
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import timber.log.Timber

internal object TerraSleepJsonMapper {

    fun sessionOrNull(row: JsonElement): TerraSleepSession? {
        val obj = row as? JsonObject ?: return null
        val metadata = obj["metadata"]?.jsonObject ?: return null
        val bedtime = metadata.parseTerraDateTimeField("start_time") ?: run {
            Timber.w("Terra sleep: missing/unparsed start_time in metadata")
            return null
        }
        val wakeTime = metadata.parseTerraDateTimeField("end_time") ?: run {
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
            addAll(stageData.periods("light_periods", TerraSleepStage.Light))
            addAll(stageData.periods("deep_periods", TerraSleepStage.Deep))
            addAll(stageData.periods("rem_periods", TerraSleepStage.REM))
            addAll(stageData.periods("wake_periods", TerraSleepStage.Awake))
        }.sortedBy { it.start }
    }

    private fun JsonObject.periods(key: String, stage: TerraSleepStage): List<TerraSleepStageInterval> {
        val arr = this[key]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val start = o.parseTerraDateTimeField("start") ?: return@mapNotNull null
            val end = o.parseTerraDateTimeField("end") ?: return@mapNotNull null
            TerraSleepStageInterval(stage = stage, start = start, end = end)
        }
    }
}
