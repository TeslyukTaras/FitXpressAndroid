package com.hexis.bi.ui.avatar

import com.hexis.bi.domain.body.BodyMeasurementRegion
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Boundary-loop tracing and scoring for mesh cross-sections: from plane∩mesh segments, builds closed
 * cycles and picks the best one (torso bands vs limbs) by area / perimeter / side-balance heuristics.
 */

internal fun polygonAreaUV(
    indices: List<Int>,
    coords: List<FloatArray>,
    planeNormal: FloatArray,
): Float {
    val (u, v) = inPlaneBasis(planeNormal)
    if (indices.size < 3) return 0f
    var sum = 0f
    val n = indices.size
    for (i in 0 until n) {
        val j = (i + 1) % n
        val pi = coords[indices[i]]
        val pj = coords[indices[j]]
        val ui = pi[0] * u[0] + pi[1] * u[1] + pi[2] * u[2]
        val vi = pi[0] * v[0] + pi[1] * v[1] + pi[2] * v[2]
        val uj = pj[0] * u[0] + pj[1] * u[1] + pj[2] * u[2]
        val vj = pj[0] * v[0] + pj[1] * v[1] + pj[2] * v[2]
        sum += ui * vj - uj * vi
    }
    return abs(sum) * 0.5f
}

private val BoundaryLimbKeys: Set<String> = BodyMeasurementRegion.bilateralCircumferenceKeys

private fun pickBestBoundaryCycle(
    cycles: List<List<Int>>,
    coords: List<FloatArray>,
    measurementKey: String,
    anchor: FloatArray,
    planeNormal: FloatArray,
): List<Int>? {
    if (cycles.isEmpty()) return null
    if (cycles.size == 1) return cycles[0]
    fun centroidDistSq(ids: List<Int>): Float {
        val c = cycleCentroid3d(ids, coords)
        val dx = c[0] - anchor[0]
        val dy = c[1] - anchor[1]
        val dz = c[2] - anchor[2]
        return dx * dx + dy * dy + dz * dz
    }
    return when (measurementKey) {
        in BoundaryLimbKeys ->
            cycles.minWithOrNull(
                compareBy<List<Int>> { centroidDistSq(it) }
                    .thenByDescending { polygonAreaUV(it, coords, planeNormal) },
            )

        in TorsoCircumferenceKeys ->
            cycles.minByOrNull { cycle ->
                torsoCycleScore(cycle, coords, measurementKey, anchor, planeNormal)
            }

        else ->
            cycles.minByOrNull { cycle ->
                genericCycleScore(cycle, coords, anchor, planeNormal)
            }
    }
}

private fun cycleCentroid3d(cycle: List<Int>, coords: List<FloatArray>): FloatArray {
    var x = 0f
    var y = 0f
    var z = 0f
    cycle.forEach { index ->
        val p = coords[index]
        x += p[0]
        y += p[1]
        z += p[2]
    }
    val inv = 1f / cycle.size.coerceAtLeast(1)
    return floatArrayOf(x * inv, y * inv, z * inv)
}

internal fun perimeterIn3d(cycle: List<Int>, coords: List<FloatArray>): Float {
    var sum = 0f
    for (i in cycle.indices) {
        val a = coords[cycle[i]]
        val b = coords[cycle[(i + 1) % cycle.size]]
        val dx = a[0] - b[0]
        val dy = a[1] - b[1]
        val dz = a[2] - b[2]
        sum += hypot(hypot(dx, dy), dz)
    }
    return sum
}

private fun torsoSideBalancePenalty(cycle: List<Int>, coords: List<FloatArray>): Float {
    var left = 0
    var right = 0
    var absXSum = 0f
    cycle.forEach { index ->
        val x = coords[index][0]
        if (x < 0f) left++ else right++
        absXSum += abs(x)
    }
    val count = cycle.size.coerceAtLeast(1)
    val balance = abs(left - right).toFloat() / count.toFloat()
    val avgAbsX = absXSum / count.toFloat()
    return balance + avgAbsX * 0.35f
}

private fun torsoCycleScore(
    cycle: List<Int>,
    coords: List<FloatArray>,
    measurementKey: String,
    anchor: FloatArray,
    planeNormal: FloatArray,
): Float {
    val centroid = cycleCentroid3d(cycle, coords)
    val area = polygonAreaUV(cycle, coords, planeNormal).coerceAtLeast(1e-6f)
    val perimeter = perimeterIn3d(cycle, coords).coerceAtLeast(1e-6f)
    val target = stabilizedSliceAnchorForMeasurement(measurementKey, anchor)
    val dx = centroid[0] - target[0]
    val dy = centroid[1] - target[1]
    val dz = centroid[2] - target[2]
    val anchorDistSq = dx * dx + dy * dy + dz * dz
    val midlinePenalty = abs(centroid[0])
    val compactnessPenalty = (perimeter * perimeter) / area
    val sideBalancePenalty = torsoSideBalancePenalty(cycle, coords)
    return anchorDistSq * 18f +
            midlinePenalty * 8f +
            sideBalancePenalty * 5f +
            compactnessPenalty * 0.015f -
            area * 0.35f
}

private fun genericCycleScore(
    cycle: List<Int>,
    coords: List<FloatArray>,
    anchor: FloatArray,
    planeNormal: FloatArray,
): Float {
    val centroid = cycleCentroid3d(cycle, coords)
    val area = polygonAreaUV(cycle, coords, planeNormal).coerceAtLeast(1e-6f)
    val dx = centroid[0] - anchor[0]
    val dy = centroid[1] - anchor[1]
    val dz = centroid[2] - anchor[2]
    return (dx * dx + dy * dy + dz * dz) * 10f - area * 0.25f
}

/** Quantile-based narrowing for torso raw slice points before polar sort (avoids fixed radii only). */
internal fun filterTorsoFallbackPoints(
    points: List<FloatArray>,
    anchor: FloatArray,
): List<FloatArray> {
    if (points.size < MIN_CROSS_SECTION_POINTS) return points
    val sortedByX = points.sortedBy { it[0] }
    val last = sortedByX.lastIndex
    val q10 = sortedByX[(last * 0.10f).toInt().coerceIn(0, last)][0]
    val q90 = sortedByX[(last * 0.90f).toInt().coerceIn(0, last)][0]
    val width = (q90 - q10).coerceAtLeast(1e-6f)
    val centerX = (q10 + q90) * 0.5f
    val maxHalfWidth = width * 0.62f
    return points.filter { p ->
        abs(p[0] - centerX) <= maxHalfWidth &&
                abs(p[2] - anchor[2]) <= width * 0.65f
    }
}

internal fun sliceVertexKey(p: FloatArray): String {
    val q = 1e5f
    return "${(p[0] * q).toLong()}_${(p[1] * q).toLong()}_${(p[2] * q).toLong()}"
}

/** One segment per triangle/plane hit — topology for chaining (not polar sorting). */
internal fun collectPlaneFaceSegments(
    vertices: List<FloatArray>,
    usedVertices: BooleanArray,
    faces: List<IntArray>,
    planePoint: FloatArray,
    planeNormal: FloatArray,
): List<Pair<FloatArray, FloatArray>> {
    val out = ArrayList<Pair<FloatArray, FloatArray>>(faces.size.coerceAtMost(4096))
    for (face in faces) {
        val n = face.size
        if (n < 2) continue
        // A planar face crosses the slice plane in exactly two points; track them without a
        // per-face list, and reuse the arrays intersectEdgeWithPlane already allocated (no copy).
        var hit0: FloatArray? = null
        var hit1: FloatArray? = null
        var hitCount = 0
        for (i in 0 until n) {
            val ia = face[i]
            val ib = face[(i + 1) % n]
            if (!usedVertices[ia] || !usedVertices[ib]) continue
            val hit = intersectEdgeWithPlane(vertices[ia], vertices[ib], planePoint, planeNormal)
                ?: continue
            when (hitCount) {
                0 -> hit0 = hit
                1 -> hit1 = hit
            }
            hitCount++
        }
        if (hitCount == 2) {
            out.add(hit0!! to hit1!!)
        }
    }
    return out
}

/**
 * Chain plane∩triangle segments into closed loops. **Torso** bands: best cycle by [torsoCycleScore]
 * (midline, anchor, compactness). **Limbs**: loop whose centroid is closest to the band anchor
 * (then largest area as tie-break).
 */
internal fun buildSliceBoundaryLoopFromMesh(
    vertices: List<FloatArray>,
    usedVertices: BooleanArray,
    faces: List<IntArray>,
    planePoint: FloatArray,
    planeNormal: FloatArray,
    measurementKey: String,
    anchor: FloatArray,
): List<FloatArray>? {
    val segments = collectPlaneFaceSegments(vertices, usedVertices, faces, planePoint, planeNormal)
    if (segments.size < 8) return null
    val keyToId = LinkedHashMap<String, Int>()
    val coords = ArrayList<FloatArray>()
    fun idFor(p: FloatArray): Int {
        val k = sliceVertexKey(p)
        val id = keyToId[k]
        if (id != null) return id
        val newId = coords.size
        keyToId[k] = newId
        coords.add(floatArrayOf(p[0], p[1], p[2]))
        return newId
    }

    val graph = HashMap<Int, MutableSet<Int>>()
    fun addEdge(a: Int, b: Int) {
        if (a == b) return
        graph.getOrPut(a) { mutableSetOf() }.add(b)
        graph.getOrPut(b) { mutableSetOf() }.add(a)
    }
    for ((p, q) in segments) {
        addEdge(idFor(p), idFor(q))
    }
    val cycles = ArrayList<List<Int>>()
    val assigned = mutableSetOf<Int>()
    for (seed in graph.keys.sorted()) {
        if (seed in assigned) continue
        val comp = mutableSetOf<Int>()
        val stack = ArrayList<Int>()
        stack.add(seed)
        while (stack.isNotEmpty()) {
            val v = stack.removeAt(stack.lastIndex)
            if (v in comp) continue
            comp.add(v)
            assigned.add(v)
            graph[v]?.forEach { stack.add(it) }
        }
        val cyc = traceDegreeTwoCycle(graph, comp) ?: continue
        if (cyc.size >= MIN_CROSS_SECTION_POINTS / 2) {
            cycles.add(cyc)
        }
    }
    if (cycles.isEmpty()) return null
    val minFull = MIN_CROSS_SECTION_POINTS
    val largeEnough = cycles.filter { it.size >= minFull }
    val candidateCycles = largeEnough.ifEmpty { cycles }
    val best = pickBestBoundaryCycle(candidateCycles, coords, measurementKey, anchor, planeNormal)
        ?: return null
    val minAccept = if (largeEnough.isNotEmpty()) minFull else MIN_CROSS_SECTION_POINTS / 2
    if (best.size < minAccept) return null
    return List(best.size) { idx ->
        coords[best[idx]].clone()
    }
}

/** Every vertex in component has degree 2 ⇒ simple cycle; trace deterministically. */
internal fun traceDegreeTwoCycle(
    graph: Map<Int, Set<Int>>,
    comp: Set<Int>,
): List<Int>? {
    for (v in comp) {
        if ((graph[v]?.size ?: 0) != 2) return null
    }
    val start = comp.minOrNull() ?: return null
    val firstNeighbor = graph[start]!!.minOrNull() ?: return null
    val path = ArrayList<Int>(comp.size + 1)
    path.add(start)
    var prev = start
    var cur = firstNeighbor
    var guard = 0
    while (guard++ < comp.size + 10) {
        path.add(cur)
        val next = graph[cur]!!.single { it != prev }
        if (next == start) {
            return path
        }
        prev = cur
        cur = next
    }
    return null
}
