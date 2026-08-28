package com.hexis.bi.intelligence.model

data class MetricPoint(
    val date: String,
    val metric: String,
    val value: Double,
    val source: String,
)

data class MetricSeries(
    val metric: String,
    val domain: String,
    val unit: String,
    val points: List<MetricPoint>,
    val coverage: Double = 0.0,
)

data class Baseline(
    val metric: String,
    val median: Double,
    val mad: Double,
    val n: Int,
) {
    fun z(value: Double): Double {
        val scale = MAD_TO_STD_DEV * mad
        return if (scale == 0.0) 0.0 else (value - median) / scale
    }

    companion object {
        const val MAD_TO_STD_DEV = 1.4826
    }
}

data class Trend(
    val metric: String,
    val domain: String,
    val windowDays: Int,
    val direction: String,
    val slope: Double,
    val velocity: Double,
    val persistenceDays: Int,
    val absChange: Double,
    val relChange: Double,
    val trendStrength: Double,
    val coverage: Double,
    val zNow: Double,
    val lastDate: String = "",
    val priorPeriodChange: Double? = null,
    val changeVsPrior: Double? = null,
)

data class QualityVerdict(
    val metric: String,
    val domain: String,
    val ok: Boolean,
    val status: String,
    val reasons: List<String>,
    val coverage: Double,
    val lastDate: String,
)

data class ConfidenceBreakdown(
    val factors: Map<String, Double>,
    val score: Double,
    val bucket: String,
)

data class Finding(
    val insightId: String,
    val area: String,
    val direction: String,
    val period: String,
    val interpretation: String,
    val facts: List<String>,
    val supportingValues: Map<String, String>,
    val evidenceStage: String,
    val confidence: String,
    val confidenceScore: Double,
    val priorityRank: Int = 0,
    val rulesetVersion: String = "",
    val source: String = "pattern",
    val featured: Boolean = false,
    val informational: Boolean = false,
    val confidenceFactors: Map<String, Double> = emptyMap(),
)

data class EngineInput(
    val runDate: String,
    val pullDays: Int,
    val points: List<MetricPoint>,
    val heightCm: Double? = null,
)
