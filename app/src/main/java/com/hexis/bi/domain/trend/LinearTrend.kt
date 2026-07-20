package com.hexis.bi.domain.trend

data class TrendPoint(
    val timeDays: Double,
    val value: Float,
)

fun linearTrendChange(points: List<TrendPoint>): Float {
    if (points.size < 2) return 0f
    val sorted = points.sortedBy { it.timeDays }
    val endpointChange = sorted.last().value - sorted.first().value
    val spanDays = sorted.last().timeDays - sorted.first().timeDays
    if (spanDays <= 0.0) return endpointChange

    val meanX = sorted.sumOf { it.timeDays } / sorted.size
    val meanY = sorted.sumOf { it.value.toDouble() } / sorted.size
    var numerator = 0.0
    var denominator = 0.0
    for (point in sorted) {
        val dx = point.timeDays - meanX
        numerator += dx * (point.value - meanY)
        denominator += dx * dx
    }
    if (denominator == 0.0) return endpointChange
    return ((numerator / denominator) * spanDays).toFloat()
}

fun winsorized(points: List<TrendPoint>, percentile: Float = DEFAULT_WINSOR_PERCENTILE): List<TrendPoint> {
    if (points.size < MIN_POINTS_TO_WINSORIZE) return points
    val values = points.map { it.value }.sorted()
    val lowIndex = ((values.size - 1) * percentile).toInt()
    val highIndex = values.size - 1 - lowIndex
    val low = values[lowIndex]
    val high = values[highIndex]
    return points.map { it.copy(value = it.value.coerceIn(low, high)) }
}

fun trendPersistence(points: List<TrendPoint>, change: Float): Float {
    if (points.size < MIN_POINTS_FOR_PERSISTENCE || change == 0f) return 1f
    val sorted = points.sortedBy { it.timeDays }
    val steps = sorted.zipWithNext { previous, next -> next.value - previous.value }
        .filter { it != 0f }
    if (steps.isEmpty()) return 1f
    val agreeing = steps.count { (it > 0f) == (change > 0f) }
    return agreeing.toFloat() / steps.size
}

private const val DEFAULT_WINSOR_PERCENTILE = 0.1f
private const val MIN_POINTS_TO_WINSORIZE = 5
private const val MIN_POINTS_FOR_PERSISTENCE = 3
