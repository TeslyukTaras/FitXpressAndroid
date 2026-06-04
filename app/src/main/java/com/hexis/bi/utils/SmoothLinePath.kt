package com.hexis.bi.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.hypot

/**
 * Monotone-cubic smoothing for line charts. Shared by the Longevity trend chart and the Home
 * scan sparkline so a single smoothing implementation backs every smooth line in the app.
 */
object SmoothLinePath {

    private const val DEFAULT_TANGENT_LIMIT = 3f

    /**
     * Builds a monotone-cubic [Path] through [points] (already in pixel space). The curve never
     * overshoots between samples, so it reads as a clean trend without wobble.
     */
    fun build(points: List<Offset>, tangentLimit: Float = DEFAULT_TANGENT_LIMIT): Path {
        val path = Path()
        if (points.isEmpty()) return path
        if (points.size == 1) {
            path.moveTo(points[0].x, points[0].y)
            return path
        }
        val tangents = monotoneTangents(points, tangentLimit)
        path.moveTo(points[0].x, points[0].y)
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]
            val dx = end.x - start.x
            path.cubicTo(
                start.x + dx / 3f, start.y + tangents[i] * dx / 3f,
                end.x - dx / 3f, end.y - tangents[i + 1] * dx / 3f,
                end.x, end.y,
            )
        }
        return path
    }

    private fun monotoneTangents(points: List<Offset>, tangentLimit: Float): FloatArray {
        val n = points.size
        val tangent = FloatArray(n)
        if (n < 2) return tangent

        val dx = FloatArray(n - 1)
        val secant = FloatArray(n - 1)
        for (i in 0 until n - 1) {
            dx[i] = points[i + 1].x - points[i].x
            secant[i] = if (dx[i] != 0f) (points[i + 1].y - points[i].y) / dx[i] else 0f
        }

        tangent[0] = secant[0]
        tangent[n - 1] = secant[n - 2]
        for (i in 1 until n - 1) {
            tangent[i] = if (secant[i - 1] * secant[i] <= 0f) 0f else (secant[i - 1] + secant[i]) / 2f
        }
        for (i in 0 until n - 1) {
            if (secant[i] == 0f) {
                tangent[i] = 0f
                tangent[i + 1] = 0f
            } else {
                val a = tangent[i] / secant[i]
                val b = tangent[i + 1] / secant[i]
                val h = hypot(a, b)
                if (h > tangentLimit) {
                    val scale = tangentLimit / h
                    tangent[i] = scale * a * secant[i]
                    tangent[i + 1] = scale * b * secant[i]
                }
            }
        }
        return tangent
    }
}
