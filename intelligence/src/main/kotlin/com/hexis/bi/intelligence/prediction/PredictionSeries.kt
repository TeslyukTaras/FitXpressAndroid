package com.hexis.bi.intelligence.prediction

enum class PredictionSeries(
    val key: String,
    val persisted: Boolean,
    val minValue: Double,
    val maxValue: Double,
    val negligibleDelta: Double,
) {
    LEAN_KG("leanKg", true, 0.0, Double.MAX_VALUE, 0.10),
    FAT_KG("fatKg", true, 0.0, Double.MAX_VALUE, 0.10),
    LEAN_PCT("leanPct", true, 0.0, 100.0, 0.10),
    FAT_PCT("fatPct", true, 0.0, 100.0, 0.10),
    SCORE("score", true, 1.0, 10.0, 0.05),
    COMPARABLE_SCORE("comparableScore", false, 1.0, 10.0, 0.05);

    fun clamp(value: Double): Double = value.coerceIn(minValue, maxValue)

    companion object {
        val PERSISTED: List<PredictionSeries> = entries.filter { it.persisted }

        fun fromKey(key: String): PredictionSeries? = entries.firstOrNull { it.key == key }
    }
}
