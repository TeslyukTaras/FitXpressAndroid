package com.hexis.bi.intelligence.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SUPPORTED_CONFIG_SCHEMA_VERSION = 1

@Serializable
data class EngineConfig(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("config_version") val configVersion: String,
    @SerialName("ruleset_version") val rulesetVersion: String,
    val windows: WindowsConfig,
    val trend: TrendConfig,
    val thresholds: ThresholdsConfig,
    val confidence: ConfidenceConfig,
    val priority: Map<String, Double>,
    val quality: QualityConfig,
    val composites: CompositesConfig,
    val domains: Map<String, String>,
) {
    fun domainOf(metric: String): String = domains[metric] ?: UNKNOWN_DOMAIN

    fun priorityOf(area: String): Double = priority[area] ?: DEFAULT_AREA_PRIORITY

    val featuredLimit: Int get() = priority[FEATURED_LIMIT_KEY]?.toInt() ?: DEFAULT_FEATURED_LIMIT

    companion object {
        const val UNKNOWN_DOMAIN = "unknown"
        const val FEATURED_LIMIT_KEY = "featured_limit"
        const val DEFAULT_FEATURED_LIMIT = 3
        const val DEFAULT_AREA_PRIORITY = 1.0
    }
}

@Serializable
data class WindowsConfig(
    @SerialName("baseline_days") val baselineDays: Int,
    @SerialName("analysis_days") val analysisDays: Int = DEFAULT_ANALYSIS_DAYS,
    @SerialName("trend_days") val trendDays: List<Int>,
    @SerialName("short_avgs") val shortAverages: List<Int> = emptyList(),
    @SerialName("display_windows") val displayWindows: List<Int> = emptyList(),
    @SerialName("prior_period") val priorPeriod: Boolean = false,
    @SerialName("min_scans_for_drift") val minScansForDrift: Int = 3,
    @SerialName("drift_coverage_scans") val driftCoverageScans: Int = 6,
) {
    val observationDays: Int get() = analysisDays + baselineDays

    companion object {
        const val DEFAULT_ANALYSIS_DAYS = 30
    }
}

@Serializable
data class TrendConfig(
    val estimator: String,
    @SerialName("winsorize_pct") val winsorizePct: Double,
    @SerialName("stable_deadband_frac") val stableDeadbandFraction: Double,
    @SerialName("decisive_change_multiple") val decisiveChangeMultiple: Double,
    @SerialName("min_persistence") val minPersistence: Double,
    @SerialName("min_persist_days") val minPersistDays: Map<String, Int>,
) {
    fun minPersistDaysFor(domain: String): Int = minPersistDays[domain] ?: DEFAULT_MIN_PERSIST_DAYS

    companion object {
        const val ESTIMATOR_THEIL_SEN = "theil_sen"
        const val DEFAULT_MIN_PERSIST_DAYS = 7
    }
}

@Serializable
data class ThresholdsConfig(
    @SerialName("default_mode") val defaultMode: String,
    @SerialName("absolute_metrics") val absoluteMetrics: Map<String, Double>,
) {
    companion object {
        const val MODE_FORCE_RELATIVE = "force_relative"
    }
}

@Serializable
data class ConfidenceConfig(
    val weights: Map<String, Double>,
    val buckets: Map<String, Double>,
    @SerialName("recency_fresh_days") val recencyFreshDays: Int,
)

@Serializable
data class QualityConfig(
    @SerialName("min_coverage") val minCoverage: Map<String, Double>,
    @SerialName("min_scans_for_body") val minScansForBody: Int,
    @SerialName("freshness_days") val freshnessDays: Map<String, Int>,
    @SerialName("plausibility_per_week") val plausibilityPerWeek: Map<String, Double>,
    @SerialName("still_learning_coverage") val stillLearningCoverage: Double,
) {
    fun minCoverageFor(domain: String): Double = minCoverage[domain] ?: DEFAULT_MIN_COVERAGE

    fun freshnessDaysFor(metric: String, domain: String): Int =
        freshnessDays[metric] ?: freshnessDays[domain] ?: freshnessDays[DEFAULT_KEY] ?: DEFAULT_FRESHNESS_DAYS

    companion object {
        const val DEFAULT_KEY = "default"
        const val DEFAULT_MIN_COVERAGE = 0.5
        const val DEFAULT_FRESHNESS_DAYS = 3
    }
}

@Serializable
data class CompositesConfig(
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("required_daily_inputs") val requiredDailyInputs: List<String>,
    @SerialName("carry_forward_days") val carryForwardDays: Map<String, Int>,
    val longevity: LongevityConfig,
    @SerialName("pace_of_aging") val paceOfAging: PaceOfAgingConfig,
    val foundations: FoundationsConfig,
    val stress: StressConfig,
    val physique: PhysiqueConfig,
)

@Serializable
data class LongevityConfig(
    val weights: Map<String, Double>,
    val anchors: Map<String, List<Double>>,
)

@Serializable
data class PaceOfAgingConfig(
    val baseline: Double,
    val min: Double,
    val max: Double,
    @SerialName("neutral_score") val neutralScore: Double,
    val effects: Map<String, Double>,
)

@Serializable
data class FoundationsConfig(
    @SerialName("window_days") val windowDays: Int,
    val weights: Map<String, Double>,
    @SerialName("strengthening_weight_min") val strengtheningWeightMin: Double,
    @SerialName("weakening_weight_min") val weakeningWeightMin: Double,
    @SerialName("major_lean_loss_kg") val majorLeanLossKg: Double,
)

@Serializable
data class StressConfig(
    @SerialName("hrv_anchors") val hrvAnchors: List<Double>,
)

@Serializable
data class PhysiqueConfig(
    val min: Double,
    val max: Double,
    val weights: Map<String, Double>,
    @SerialName("body_fat_anchors") val bodyFatAnchors: List<List<Double>>,
    @SerialName("lean_mass") val leanMass: List<Double>,
    @SerialName("waist_shape") val waistShape: List<Double>,
    val proportion: List<Double>,
    @SerialName("enable_proportion") val enableProportion: Boolean = false,
    @SerialName("drift_meaningful_delta") val driftMeaningfulDelta: Double,
    @SerialName("drift_signal_reference") val driftSignalReference: Double,
)
