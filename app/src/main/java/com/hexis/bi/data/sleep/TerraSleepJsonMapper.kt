package com.hexis.bi.data.sleep

import com.hexis.bi.data.terra.float
import com.hexis.bi.data.terra.int
import com.hexis.bi.data.terra.parseTerraDateTimeField
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import timber.log.Timber
import java.time.Duration

private object TerraSleepJsonKeys {

    /** Leaf key present under [Durations.NODE] and [Efficiency.NODE]. */
    const val SLEEP_EFFICIENCY = "sleep_efficiency"

    object Metadata {
        const val NODE = "metadata"
        const val START_TIME = "start_time"
        const val END_TIME = "end_time"
    }

    object Durations {
        const val NODE = "sleep_durations_data"
        const val ASLEEP = "asleep"
        const val IN_BED = "in_bed"
        const val OTHER = "other"
        const val DURATION_IN_BED_SECONDS = "duration_in_bed_seconds"
        const val DURATION_ASLEEP_STATE_SECONDS = "duration_asleep_state_seconds"
    }

    object Efficiency {
        const val NODE = "efficiency_data"
    }

    object HeartRate {
        const val NODE = "heart_rate_data"
        const val SUMMARY = "summary"
        const val RESTING_HR_BPM = "resting_hr_bpm"
        const val AVG_HR_BPM = "avg_hr_bpm"
        const val AVG_HRV_RMSSD = "avg_hrv_rmssd"
        const val AVG_HRV_SDNN = "avg_hrv_sdnn"

        const val DETAILED = "detailed"
        const val HR_SAMPLES = "hr_samples"
        const val HRV_RMSSD_SAMPLES = "hrv_samples_rmssd"
        const val SAMPLE_BPM = "bpm"
        const val SAMPLE_HRV_RMSSD = "hrv_rmssd"
        const val SAMPLE_TIMESTAMP = "timestamp"
        const val SAMPLE_TIMESTAMP_LOCAL = "timestamp_local"
    }

    object Stages {
        const val NODE = "sleep_stage_data"
        const val LIGHT_PERIODS = "light_periods"
        const val DEEP_PERIODS = "deep_periods"
        const val REM_PERIODS = "rem_periods"
        const val WAKE_PERIODS = "wake_periods"
        const val PERIOD_START = "start"
        const val PERIOD_END = "end"
    }
}

internal object TerraSleepJsonMapper {

    fun sessionOrNull(row: JsonElement): SleepSession? {
        val obj = row as? JsonObject ?: return null
        val metadata = obj[TerraSleepJsonKeys.Metadata.NODE]?.jsonObject
        val bedtime = metadata?.parseTerraDateTimeField(TerraSleepJsonKeys.Metadata.START_TIME)
            ?: obj.parseTerraDateTimeField(TerraSleepJsonKeys.Metadata.START_TIME)
            ?: metadata?.parseTerraDateTimeField("start_time_local")
            ?: obj.parseTerraDateTimeField("start_time_local")
            ?: run {
            Timber.w("Terra sleep: missing/unparsed %s in metadata", TerraSleepJsonKeys.Metadata.START_TIME)
            return null
        }
        val wakeTime = metadata?.parseTerraDateTimeField(TerraSleepJsonKeys.Metadata.END_TIME)
            ?: obj.parseTerraDateTimeField(TerraSleepJsonKeys.Metadata.END_TIME)
            ?: metadata?.parseTerraDateTimeField("end_time_local")
            ?: obj.parseTerraDateTimeField("end_time_local")
            ?: run {
            Timber.w("Terra sleep: missing/unparsed %s in metadata", TerraSleepJsonKeys.Metadata.END_TIME)
            return null
        }

        val durations = obj[TerraSleepJsonKeys.Durations.NODE]?.jsonObject
        val asleep = durations?.get(TerraSleepJsonKeys.Durations.ASLEEP)?.jsonObject
        val inBed = durations?.get(TerraSleepJsonKeys.Durations.IN_BED)?.jsonObject
        val other = durations?.get(TerraSleepJsonKeys.Durations.OTHER)?.jsonObject

        val spanSeconds = Duration.between(bedtime, wakeTime).seconds.coerceAtLeast(1)
        var durationSec = inBed?.float(TerraSleepJsonKeys.Durations.DURATION_IN_BED_SECONDS)
            ?: other?.float(TerraSleepJsonKeys.Durations.DURATION_IN_BED_SECONDS)
            ?: asleep?.float(TerraSleepJsonKeys.Durations.DURATION_ASLEEP_STATE_SECONDS)
            ?: spanSeconds.toFloat()

        if (durationSec < 300f && spanSeconds >= 300) {
            durationSec = spanSeconds.toFloat()
        }
        val durationMinutes = (durationSec / 60f).toInt().coerceAtLeast(1)

        val efficiency = durations?.float(TerraSleepJsonKeys.SLEEP_EFFICIENCY)
            ?: obj[TerraSleepJsonKeys.Efficiency.NODE]?.jsonObject?.float(TerraSleepJsonKeys.SLEEP_EFFICIENCY)
            ?: 0f

        val heartRateNode = obj[TerraSleepJsonKeys.HeartRate.NODE]?.jsonObject
        val heartRate = heartRateNode?.get(TerraSleepJsonKeys.HeartRate.SUMMARY)?.jsonObject
        val restingHr = heartRate?.int(TerraSleepJsonKeys.HeartRate.RESTING_HR_BPM)
            ?: heartRate?.int(TerraSleepJsonKeys.HeartRate.AVG_HR_BPM)
            ?: 0
        val hrvMs = heartRate?.float(TerraSleepJsonKeys.HeartRate.AVG_HRV_RMSSD)?.toInt() ?: 0
        val sdnnMs = heartRate?.float(TerraSleepJsonKeys.HeartRate.AVG_HRV_SDNN)?.toInt() ?: 0

        val detailed = heartRateNode?.get(TerraSleepJsonKeys.HeartRate.DETAILED)?.jsonObject
        val heartRateSamples = detailed.parseSamples(
            TerraSleepJsonKeys.HeartRate.HR_SAMPLES,
            TerraSleepJsonKeys.HeartRate.SAMPLE_BPM,
        )
        val hrvSamples = detailed.parseSamples(
            TerraSleepJsonKeys.HeartRate.HRV_RMSSD_SAMPLES,
            TerraSleepJsonKeys.HeartRate.SAMPLE_HRV_RMSSD,
        )

        return SleepSession(
            bedtime = bedtime,
            wakeTime = wakeTime,
            durationMinutes = durationMinutes,
            efficiencyPercent = efficiency,
            restingHeartRateBpm = restingHr,
            hrvMs = hrvMs,
            sdnnMs = sdnnMs,
            stages = parseStages(obj),
            heartRateSamples = heartRateSamples,
            hrvSamples = hrvSamples,
        )
    }

    /** Maps a Terra detailed sample array (`[{ timestamp, <valueField> }, …]`) to sorted samples. */
    private fun JsonObject?.parseSamples(arrayField: String, valueField: String): List<SleepSample> {
        val arr = this?.get(arrayField)?.jsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val time = o.parseTerraDateTimeField(TerraSleepJsonKeys.HeartRate.SAMPLE_TIMESTAMP)
                ?: o.parseTerraDateTimeField(TerraSleepJsonKeys.HeartRate.SAMPLE_TIMESTAMP_LOCAL)
                ?: return@mapNotNull null
            val value = o.float(valueField)?.toInt() ?: return@mapNotNull null
            SleepSample(time = time, value = value)
        }.sortedBy { it.time }
    }

    private fun parseStages(root: JsonObject): List<SleepStageInterval> {
        val stageData = root[TerraSleepJsonKeys.Stages.NODE]?.jsonObject ?: return emptyList()
        return buildList {
            addAll(stageData.periods(TerraSleepJsonKeys.Stages.LIGHT_PERIODS, SleepStage.Light))
            addAll(stageData.periods(TerraSleepJsonKeys.Stages.DEEP_PERIODS, SleepStage.Deep))
            addAll(stageData.periods(TerraSleepJsonKeys.Stages.REM_PERIODS, SleepStage.REM))
            addAll(stageData.periods(TerraSleepJsonKeys.Stages.WAKE_PERIODS, SleepStage.Awake))
        }.sortedBy { it.start }
    }

    private fun JsonObject.periods(periodField: String, stage: SleepStage): List<SleepStageInterval> {
        val arr = this[periodField]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val start = o.parseTerraDateTimeField(TerraSleepJsonKeys.Stages.PERIOD_START) ?: return@mapNotNull null
            val end = o.parseTerraDateTimeField(TerraSleepJsonKeys.Stages.PERIOD_END) ?: return@mapNotNull null
            SleepStageInterval(stage = stage, start = start, end = end)
        }
    }
}
