package com.hexis.bi.data.sleep

import com.hexis.bi.data.terra.arrayOrNull
import com.hexis.bi.data.terra.float
import com.hexis.bi.data.terra.int
import com.hexis.bi.data.terra.objectOrNull
import com.hexis.bi.data.terra.parseTerraDateTimeField
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.roundToInt
import kotlin.math.sqrt

private object TerraSleepJsonKeys {

    /** Leaf key present under [Durations.NODE] and [Efficiency.NODE]. */
    const val SLEEP_EFFICIENCY = "sleep_efficiency"

    object Metadata {
        const val NODE = "metadata"
        const val START_TIME = "start_time"
        const val END_TIME = "end_time"
        const val IS_NAP = "is_nap"
    }

    object Durations {
        const val NODE = "sleep_durations_data"
        const val ASLEEP = "asleep"
        const val IN_BED = "in_bed"
        const val OTHER = "other"
        const val HYPNOGRAM_SAMPLES = "hypnogram_samples"
        const val HYPNOGRAM_LEVEL = "level"
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

    private const val TERRA_AWAKE_LEVEL = 1
    private const val TERRA_LIGHT_SLEEP_LEVEL = 4
    private const val TERRA_DEEP_SLEEP_LEVEL = 5
    private const val TERRA_REM_SLEEP_LEVEL = 6

    fun sessionOrNull(row: JsonElement): SleepSession? {
        val obj = row as? JsonObject ?: return null
        val metadata = obj.objectOrNull(TerraSleepJsonKeys.Metadata.NODE)
        val bedtime = metadata?.parseTerraDateTimeField(TerraSleepJsonKeys.Metadata.START_TIME)
            ?: obj.parseTerraDateTimeField(TerraSleepJsonKeys.Metadata.START_TIME)
            ?: metadata?.parseTerraDateTimeField("start_time_local")
            ?: obj.parseTerraDateTimeField("start_time_local")
            ?: run {
                Timber.w(
                    "Terra sleep: missing/unparsed %s in metadata",
                    TerraSleepJsonKeys.Metadata.START_TIME
                )
                return null
            }
        val wakeTime = metadata?.parseTerraDateTimeField(TerraSleepJsonKeys.Metadata.END_TIME)
            ?: obj.parseTerraDateTimeField(TerraSleepJsonKeys.Metadata.END_TIME)
            ?: metadata?.parseTerraDateTimeField("end_time_local")
            ?: obj.parseTerraDateTimeField("end_time_local")
            ?: run {
                Timber.w(
                    "Terra sleep: missing/unparsed %s in metadata",
                    TerraSleepJsonKeys.Metadata.END_TIME
                )
                return null
            }

        val durations = obj.objectOrNull(TerraSleepJsonKeys.Durations.NODE)
        val asleep = durations?.objectOrNull(TerraSleepJsonKeys.Durations.ASLEEP)
        val inBed = durations?.objectOrNull(TerraSleepJsonKeys.Durations.IN_BED)
        val other = durations?.objectOrNull(TerraSleepJsonKeys.Durations.OTHER)

        val spanSeconds = Duration.between(bedtime, wakeTime).seconds.coerceAtLeast(1)
        var durationSec = asleep?.float(TerraSleepJsonKeys.Durations.DURATION_ASLEEP_STATE_SECONDS)
            ?: inBed?.float(TerraSleepJsonKeys.Durations.DURATION_IN_BED_SECONDS)
            ?: other?.float(TerraSleepJsonKeys.Durations.DURATION_IN_BED_SECONDS)
            ?: spanSeconds.toFloat()

        if (durationSec < 300f && spanSeconds >= 300) {
            durationSec = spanSeconds.toFloat()
        }
        val durationMinutes = (durationSec / 60f).toInt().coerceAtLeast(1)

        val efficiency = normalizeEfficiencyPercent(
            durations?.float(TerraSleepJsonKeys.SLEEP_EFFICIENCY)
                ?: obj.objectOrNull(TerraSleepJsonKeys.Efficiency.NODE)
                    ?.float(TerraSleepJsonKeys.SLEEP_EFFICIENCY)
        )

        val heartRateNode = obj.objectOrNull(TerraSleepJsonKeys.HeartRate.NODE)
        val heartRate = heartRateNode?.objectOrNull(TerraSleepJsonKeys.HeartRate.SUMMARY)
        val restingHr = heartRate?.int(TerraSleepJsonKeys.HeartRate.RESTING_HR_BPM)
            ?: heartRate?.int(TerraSleepJsonKeys.HeartRate.AVG_HR_BPM)
            ?: 0
        val hrvMs = heartRate?.float(TerraSleepJsonKeys.HeartRate.AVG_HRV_RMSSD)?.toInt() ?: 0

        val detailed = heartRateNode?.objectOrNull(TerraSleepJsonKeys.HeartRate.DETAILED)
        val heartRateSamples = detailed.parseSamples(
            TerraSleepJsonKeys.HeartRate.HR_SAMPLES,
            TerraSleepJsonKeys.HeartRate.SAMPLE_BPM,
        )
        val hrvSamples = detailed.parseSamples(
            TerraSleepJsonKeys.HeartRate.HRV_RMSSD_SAMPLES,
            TerraSleepJsonKeys.HeartRate.SAMPLE_HRV_RMSSD,
        )

        val sdnnMs = heartRate?.float(TerraSleepJsonKeys.HeartRate.AVG_HRV_SDNN)?.toInt()
            ?.takeIf { it > 0 }
            ?: estimateSdnnFromHeartRateSamples(heartRateSamples)

        return SleepSession(
            bedtime = bedtime,
            wakeTime = wakeTime,
            durationMinutes = durationMinutes,
            efficiencyPercent = efficiency,
            restingHeartRateBpm = restingHr,
            hrvMs = hrvMs,
            sdnnMs = sdnnMs,
            stages = parseStages(obj, wakeTime),
            isNap = metadata?.boolean(TerraSleepJsonKeys.Metadata.IS_NAP) ?: false,
            heartRateSamples = heartRateSamples,
            hrvSamples = hrvSamples,
        )
    }

    private fun normalizeEfficiencyPercent(value: Float?): Float {
        val raw = value ?: return 0f
        return if (raw in 0f..1f) raw * 100f else raw
    }

    /** Maps a Terra detailed sample array (`[{ timestamp, <valueField> }, …]`) to sorted samples. */
    private fun JsonObject?.parseSamples(
        arrayField: String,
        valueField: String
    ): List<SleepSample> {
        val arr = this?.arrayOrNull(arrayField) ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val time = o.parseTerraDateTimeField(TerraSleepJsonKeys.HeartRate.SAMPLE_TIMESTAMP)
                ?: o.parseTerraDateTimeField(TerraSleepJsonKeys.HeartRate.SAMPLE_TIMESTAMP_LOCAL)
                ?: return@mapNotNull null
            val value = o.float(valueField)?.toInt() ?: return@mapNotNull null
            SleepSample(time = time, value = value)
        }.sortedBy { it.time }
    }

    private fun estimateSdnnFromHeartRateSamples(samples: List<SleepSample>): Int {
        val nnIntervalsMs = samples
            .mapNotNull { sample ->
                sample.value
                    .takeIf { it > 0 }
                    ?.let { bpm -> 60_000.0 / bpm }
            }
        if (nnIntervalsMs.size < 2) return 0

        val mean = nnIntervalsMs.average()
        val variance = nnIntervalsMs.sumOf { interval ->
            val delta = interval - mean
            delta * delta
        } / nnIntervalsMs.size
        return sqrt(variance).roundToInt()
    }

    private fun parseStages(root: JsonObject, wakeTime: LocalDateTime): List<SleepStageInterval> {
        val periodIntervals = root.objectOrNull(TerraSleepJsonKeys.Stages.NODE)?.let { stageData ->
            buildList {
                addAll(stageData.periods(TerraSleepJsonKeys.Stages.LIGHT_PERIODS, SleepStage.Light))
                addAll(stageData.periods(TerraSleepJsonKeys.Stages.DEEP_PERIODS, SleepStage.Deep))
                addAll(stageData.periods(TerraSleepJsonKeys.Stages.REM_PERIODS, SleepStage.REM))
                addAll(stageData.periods(TerraSleepJsonKeys.Stages.WAKE_PERIODS, SleepStage.Awake))
            }.sortedBy { it.start }
        }.orEmpty()

        if (periodIntervals.isNotEmpty()) return periodIntervals

        return root.objectOrNull(TerraSleepJsonKeys.Durations.NODE)
            ?.hypnogramIntervals(wakeTime)
            .orEmpty()
    }

    private fun JsonObject.periods(
        periodField: String,
        stage: SleepStage
    ): List<SleepStageInterval> {
        val arr = arrayOrNull(periodField) ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val start = o.parseTerraDateTimeField(TerraSleepJsonKeys.Stages.PERIOD_START)
                ?: return@mapNotNull null
            val end = o.parseTerraDateTimeField(TerraSleepJsonKeys.Stages.PERIOD_END)
                ?: return@mapNotNull null
            SleepStageInterval(stage = stage, start = start, end = end)
        }
    }

    private fun JsonObject.boolean(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject.hypnogramIntervals(wakeTime: LocalDateTime): List<SleepStageInterval> {
        val samples = arrayOrNull(TerraSleepJsonKeys.Durations.HYPNOGRAM_SAMPLES)
            ?.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val time = o.parseTerraDateTimeField(TerraSleepJsonKeys.HeartRate.SAMPLE_TIMESTAMP)
                    ?: o.parseTerraDateTimeField(TerraSleepJsonKeys.HeartRate.SAMPLE_TIMESTAMP_LOCAL)
                    ?: return@mapNotNull null
                val stage = o.int(TerraSleepJsonKeys.Durations.HYPNOGRAM_LEVEL)?.toSleepStage()
                    ?: return@mapNotNull null
                stage to time
            }
            ?.sortedBy { it.second }
            .orEmpty()

        return samples.mapIndexedNotNull { index, (stage, start) ->
            val end = samples.getOrNull(index + 1)?.second ?: wakeTime
            if (!end.isAfter(start)) return@mapIndexedNotNull null
            SleepStageInterval(stage = stage, start = start, end = end)
        }
    }

    private fun Int.toSleepStage(): SleepStage? = when (this) {
        TERRA_AWAKE_LEVEL -> SleepStage.Awake
        TERRA_LIGHT_SLEEP_LEVEL -> SleepStage.Light
        TERRA_DEEP_SLEEP_LEVEL -> SleepStage.Deep
        TERRA_REM_SLEEP_LEVEL -> SleepStage.REM
        else -> null
    }
}
