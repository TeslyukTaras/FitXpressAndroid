package com.hexis.bi.intelligence.engine

import kotlin.math.abs
import kotlin.math.sqrt

internal data class LinearFit(val slope: Double, val change: Double)

internal data class WinsorizeResult(val values: List<Double>, val clamped: Int)

internal fun compensatedSum(xs: Iterable<Double>): Double {
    var total = 0.0
    var compensation = 0.0
    for (x in xs) {
        val next = total + x
        compensation += if (abs(total) >= abs(x)) (total - next) + x else (x - next) + total
        total = next
    }
    return total + compensation
}

internal fun median(xs: List<Double>): Double {
    if (xs.isEmpty()) return 0.0
    val sorted = xs.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
}

internal fun mad(xs: List<Double>, med: Double? = null): Double {
    val centre = med ?: median(xs)
    return median(xs.map { abs(it - centre) })
}

internal fun mean(xs: List<Double>): Double {
    if (xs.isEmpty()) return 0.0
    return compensatedSum(xs) / xs.size
}

internal fun olsChange(days: List<Double>, values: List<Double>): LinearFit {
    if (days.size < 2) return LinearFit(0.0, 0.0)
    val meanX = mean(days)
    val meanY = mean(values)
    val denominator = compensatedSum(days.map { val dx = it - meanX; dx * dx })
    if (denominator == 0.0) return LinearFit(0.0, values.last() - values.first())
    val numerator = compensatedSum(
        (0 until minOf(days.size, values.size)).map { (days[it] - meanX) * (values[it] - meanY) },
    )
    val slope = numerator / denominator
    return LinearFit(slope, slope * (days.last() - days.first()))
}

internal fun theilSenChange(days: List<Double>, values: List<Double>): LinearFit {
    val slopes = ArrayList<Double>()
    for (i in days.indices) {
        for (j in i + 1 until days.size) {
            if (days[j] != days[i]) slopes += (values[j] - values[i]) / (days[j] - days[i])
        }
    }
    val slope = if (slopes.isEmpty()) 0.0 else median(slopes)
    val span = if (days.isEmpty()) 0.0 else days.last() - days.first()
    return LinearFit(slope, slope * span)
}

internal fun averageRanks(xs: List<Double>): List<Double> {
    val order = xs.indices.sortedBy { xs[it] }
    val ranks = DoubleArray(xs.size)
    var start = 0
    while (start < order.size) {
        var end = start
        while (end + 1 < order.size && xs[order[end + 1]] == xs[order[start]]) end++
        val average = (start + end) / 2.0 + 1.0
        for (k in start..end) ranks[order[k]] = average
        start = end + 1
    }
    return ranks.toList()
}

internal fun spearmanAbs(xs: List<Double>, ys: List<Double>): Double =
    abs(pearson(averageRanks(xs), averageRanks(ys)))

private fun pearson(a: List<Double>, b: List<Double>): Double {
    if (a.size < 2) return 0.0
    val meanA = mean(a)
    val meanB = mean(b)
    val covariance = compensatedSum(
        (0 until minOf(a.size, b.size)).map { (a[it] - meanA) * (b[it] - meanB) },
    )
    val deviationA = sqrt(compensatedSum(a.map { val d = it - meanA; d * d }))
    val deviationB = sqrt(compensatedSum(b.map { val d = it - meanB; d * d }))
    return if (deviationA > 0.0 && deviationB > 0.0) covariance / (deviationA * deviationB) else 0.0
}

internal fun winsorize(values: List<Double>, pct: Double): WinsorizeResult {
    if (values.size < MIN_POINTS_TO_WINSORIZE || pct <= 0.0) return WinsorizeResult(values, 0)
    val sorted = values.sorted()
    val lowIndex = ((sorted.size - 1) * pct).toInt()
    val low = sorted[lowIndex]
    val high = sorted[sorted.size - 1 - lowIndex]
    val clamped = values.map { minOf(maxOf(it, low), high) }
    return WinsorizeResult(clamped, values.indices.count { values[it] != clamped[it] })
}

private const val MIN_POINTS_TO_WINSORIZE = 5
