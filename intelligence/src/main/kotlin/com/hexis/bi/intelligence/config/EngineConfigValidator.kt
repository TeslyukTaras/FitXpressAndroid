package com.hexis.bi.intelligence.config

private val KNOWN_ESTIMATORS = setOf("linear_ols", TrendConfig.ESTIMATOR_THEIL_SEN)
private val KNOWN_AREAS = setOf("sleep", "recovery", "activity", "aging", "body", "stress", "metabolic")
private const val MAX_WINSORIZE_PCT = 0.5
private const val ANCHOR_PAIR_SIZE = 2
private const val INTERPOLATION_ANCHOR_SIZE = 4

class EngineConfigInvalid(val problems: List<String>) :
    IllegalArgumentException("engine config rejected: ${problems.joinToString("; ")}")

object EngineConfigValidator {

    fun validate(config: EngineConfig): Result<EngineConfig> {
        val problems = problemsIn(config)
        return if (problems.isEmpty()) Result.success(config) else Result.failure(EngineConfigInvalid(problems))
    }

    fun problemsIn(config: EngineConfig): List<String> = buildList {
        if (config.schemaVersion != SUPPORTED_CONFIG_SCHEMA_VERSION) {
            add("schema_version ${config.schemaVersion} != $SUPPORTED_CONFIG_SCHEMA_VERSION")
        }
        if (config.configVersion.isBlank()) add("config_version is blank")
        if (config.rulesetVersion.isBlank()) add("ruleset_version is blank")

        validateWindows(config.windows)
        validateTrend(config.trend)
        validateThresholds(config.thresholds)
        validateConfidence(config.confidence)
        validateQuality(config.quality)
        validateComposites(config.composites)

        config.domains.forEach { (metric, area) ->
            if (area !in KNOWN_AREAS) add("domains.$metric maps to unknown area '$area'")
        }
        if (config.priority.none { it.key != EngineConfig.FEATURED_LIMIT_KEY }) {
            add("priority defines no areas")
        }
        config.priority.forEach { (key, value) ->
            if (key != EngineConfig.FEATURED_LIMIT_KEY && key !in KNOWN_AREAS) {
                add("priority.$key is not a known area")
            }
            if (value < 0.0) add("priority.$key is negative")
        }
        if (config.featuredLimit < 0) add("priority.featured_limit is negative")
    }

    private fun MutableList<String>.validateWindows(windows: WindowsConfig) {
        if (windows.baselineDays <= 0) add("windows.baseline_days must be > 0")
        if (windows.analysisDays <= 0) add("windows.analysis_days must be > 0")
        if (windows.trendDays.isEmpty()) add("windows.trend_days is empty")
        if (windows.trendDays.any { it <= 0 }) add("windows.trend_days contains a non-positive value")
        if (windows.trendDays != windows.trendDays.sorted() ||
            windows.trendDays.distinct().size != windows.trendDays.size
        ) {
            add("windows.trend_days must be strictly ascending")
        }
        if (windows.minScansForDrift <= 0) add("windows.min_scans_for_drift must be > 0")
        if (windows.driftCoverageScans <= 0) add("windows.drift_coverage_scans must be > 0")
    }

    private fun MutableList<String>.validateTrend(trend: TrendConfig) {
        if (trend.estimator !in KNOWN_ESTIMATORS) add("trend.estimator '${trend.estimator}' is unknown")
        if (trend.winsorizePct < 0.0 || trend.winsorizePct >= MAX_WINSORIZE_PCT) {
            add("trend.winsorize_pct must be in [0, $MAX_WINSORIZE_PCT)")
        }
        if (trend.minPersistence !in 0.0..1.0) add("trend.min_persistence must be in [0, 1]")
        if (trend.stableDeadbandFraction < 0.0) add("trend.stable_deadband_frac is negative")
        if (trend.decisiveChangeMultiple < 1.0) add("trend.decisive_change_multiple must be >= 1")
        trend.minPersistDays.forEach { (domain, days) ->
            if (days < 0) add("trend.min_persist_days.$domain is negative")
        }
    }

    private fun MutableList<String>.validateThresholds(thresholds: ThresholdsConfig) {
        if (thresholds.defaultMode.isBlank()) add("thresholds.default_mode is blank")
        thresholds.absoluteMetrics.forEach { (metric, value) ->
            if (value <= 0.0) add("thresholds.absolute_metrics.$metric must be > 0")
        }
    }

    private fun MutableList<String>.validateConfidence(confidence: ConfidenceConfig) {
        confidence.weights.forEach { (factor, weight) ->
            if (weight < 0.0) add("confidence.weights.$factor is negative")
        }
        val missing = setOf(
            "signal_magnitude", "persistence", "agreement", "coverage",
            "recency", "source_quality", "contradiction",
        ) - confidence.weights.keys
        if (missing.isNotEmpty()) add("confidence.weights is missing ${missing.sorted()}")

        val high = confidence.buckets["high"]
        val moderate = confidence.buckets["moderate"]
        if (high == null) add("confidence.buckets.high is missing")
        if (moderate == null) add("confidence.buckets.moderate is missing")
        if (high != null && moderate != null) {
            if (high !in 0.0..1.0) add("confidence.buckets.high must be in [0, 1]")
            if (moderate !in 0.0..1.0) add("confidence.buckets.moderate must be in [0, 1]")
            if (high < moderate) add("confidence.buckets.high is below moderate")
        }
        if (confidence.recencyFreshDays <= 0) add("confidence.recency_fresh_days must be > 0")
    }

    private fun MutableList<String>.validateQuality(quality: QualityConfig) {
        quality.minCoverage.forEach { (domain, floor) ->
            if (floor !in 0.0..1.0) add("quality.min_coverage.$domain must be in [0, 1]")
        }
        if (quality.minScansForBody <= 0) add("quality.min_scans_for_body must be > 0")
        if (QualityConfig.DEFAULT_KEY !in quality.freshnessDays) {
            add("quality.freshness_days has no 'default' entry")
        }
        quality.freshnessDays.forEach { (key, days) ->
            if (days <= 0) add("quality.freshness_days.$key must be > 0")
        }
        quality.plausibilityPerWeek.forEach { (metric, bound) ->
            if (bound <= 0.0) add("quality.plausibility_per_week.$metric must be > 0")
        }
        if (quality.stillLearningCoverage !in 0.0..1.0) {
            add("quality.still_learning_coverage must be in [0, 1]")
        }
    }

    private fun MutableList<String>.validateComposites(composites: CompositesConfig) {
        if (composites.requiredDailyInputs.isEmpty()) add("composites.required_daily_inputs is empty")
        composites.carryForwardDays.forEach { (metric, days) ->
            if (days < 0) add("composites.carry_forward_days.$metric is negative")
        }

        if (composites.longevity.weights.values.sum() <= 0.0) {
            add("composites.longevity.weights sum to zero")
        }
        composites.longevity.anchors.forEach { (signal, anchor) ->
            when {
                anchor.size != ANCHOR_PAIR_SIZE ->
                    add("composites.longevity.anchors.$signal needs exactly 2 values")
                anchor[0] == anchor[1] ->
                    add("composites.longevity.anchors.$signal has identical endpoints")
            }
        }

        val pace = composites.paceOfAging
        if (pace.min >= pace.max) add("composites.pace_of_aging.min is not below max")
        if (pace.baseline !in pace.min..pace.max) {
            add("composites.pace_of_aging.baseline is outside [min, max]")
        }
        if (pace.neutralScore <= 0.0) add("composites.pace_of_aging.neutral_score must be > 0")

        val foundations = composites.foundations
        if (foundations.windowDays <= 0) add("composites.foundations.window_days must be > 0")
        if (foundations.weights.values.sum() <= 0.0) add("composites.foundations.weights sum to zero")
        if (foundations.majorLeanLossKg <= 0.0) add("composites.foundations.major_lean_loss_kg must be > 0")

        if (composites.stress.hrvAnchors.size != ANCHOR_PAIR_SIZE) {
            add("composites.stress.hrv_anchors needs exactly 2 values")
        } else if (composites.stress.hrvAnchors[0] == composites.stress.hrvAnchors[1]) {
            add("composites.stress.hrv_anchors has identical endpoints")
        }

        validatePhysique(composites.physique)
    }

    private fun MutableList<String>.validatePhysique(physique: PhysiqueConfig) {
        if (physique.min >= physique.max) add("composites.physique.min is not below max")
        if (physique.weights.values.sum() <= 0.0) add("composites.physique.weights sum to zero")
        if (physique.driftMeaningfulDelta <= 0.0) add("composites.physique.drift_meaningful_delta must be > 0")
        if (physique.driftSignalReference <= 0.0) add("composites.physique.drift_signal_reference must be > 0")

        if (physique.bodyFatAnchors.size < ANCHOR_PAIR_SIZE) {
            add("composites.physique.body_fat_anchors needs at least 2 anchors")
        }
        if (physique.bodyFatAnchors.any { it.size != ANCHOR_PAIR_SIZE }) {
            add("composites.physique.body_fat_anchors entries must be [input, score] pairs")
        } else {
            val inputs = physique.bodyFatAnchors.map { it[0] }
            if (inputs != inputs.sorted() || inputs.distinct().size != inputs.size) {
                add("composites.physique.body_fat_anchors must ascend by input")
            }
        }
        listOf(
            "lean_mass" to physique.leanMass,
            "waist_shape" to physique.waistShape,
            "proportion" to physique.proportion,
        ).forEach { (name, anchor) ->
            if (anchor.size != INTERPOLATION_ANCHOR_SIZE) {
                add("composites.physique.$name needs 4 values [lowInput, lowScore, highInput, highScore]")
            } else if (anchor[0] == anchor[2]) {
                add("composites.physique.$name has identical input endpoints")
            }
        }
    }
}
