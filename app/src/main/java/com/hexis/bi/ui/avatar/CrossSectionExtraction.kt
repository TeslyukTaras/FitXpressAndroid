package com.hexis.bi.ui.avatar

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Low-level cross-section extraction: intersect the mesh with a slice plane, dedupe / order the points,
 * filter them down to the measurement band, and pack to a flat XYZ float array.
 */

internal fun computeCrossSectionPacked(
    vertices: List<FloatArray>,
    usedVertices: BooleanArray,
    faces: List<IntArray>,
    measurementKey: String,
    anchor: FloatArray,
    planeNormal: FloatArray,
    flipLateralFilter: Boolean = false,
): FloatArray? {
    val sliceAnchor = stabilizedSliceAnchorForMeasurement(measurementKey, anchor)
    val offsetsAlongNormal =
        floatArrayOf(0f, 0.004f, -0.004f, 0.008f, -0.008f, 0.012f, -0.012f)
    for (d in offsetsAlongNormal) {
        val planePoint = floatArrayOf(
            sliceAnchor[0] + planeNormal[0] * d,
            sliceAnchor[1] + planeNormal[1] * d,
            sliceAnchor[2] + planeNormal[2] * d,
        )
        /* Mesh topology slice where possible — same path for torso bands and oblique limb cuts */
        if (measurementKey in CircumferenceVisualKeys) {
            buildSliceBoundaryLoopFromMesh(
                vertices,
                usedVertices,
                faces,
                planePoint,
                planeNormal,
                measurementKey,
                sliceAnchor,
            )?.let { boundary ->
                if (boundary.size >= MIN_CROSS_SECTION_POINTS / 2) {
                    return packXYZ(boundary)
                }
            }
        }
        val raw = collectSlicePoints(vertices, usedVertices, faces, planePoint, planeNormal)
        val rawForFilter =
            if (measurementKey in TorsoGentleFallbackKeys) {
                val gentle = filterTorsoFallbackPoints(raw, sliceAnchor)
                if (gentle.size >= MIN_CROSS_SECTION_POINTS / 2) gentle else raw
            } else {
                raw
            }
        var filtered =
            filterSlicePointsForMeasurement(
                rawForFilter,
                measurementKey,
                sliceAnchor,
                flipLateralFilter = flipLateralFilter,
            )
        if (filtered.size < MIN_CROSS_SECTION_POINTS) {
            filtered = filterSlicePointsForMeasurement(
                rawForFilter,
                measurementKey,
                sliceAnchor,
                relaxed = true,
                flipLateralFilter = flipLateralFilter,
            )
        }
        if (filtered.size < MIN_CROSS_SECTION_POINTS / 2) {
            if (measurementKey !in TorsoCircumferenceKeys) {
                filtered = rawForFilter
            }
        }
        if (filtered.size >= MIN_CROSS_SECTION_POINTS) {
            val ordered = orderAroundSliceCentroid(filtered, planeNormal)
            return packXYZ(ordered)
        }
    }
    return null
}

/**
 * Key-specific cleanup: **shoulders** keep full lateral breadth (no tight median).
 * **Chest** tight median + drop wide lateral arm hits. **Waist** looser median so torso rim isn’t clipped.
 * **Left limbs** (bicep, thigh): negative **x**. **Right** forearm / calf: positive **x**.
 * [flipLateralFilter] selects the opposite lateral when building the mirror limb ring.
 */
private fun filterSlicePointsForMeasurement(
    points: List<FloatArray>,
    key: String,
    anchor: FloatArray,
    relaxed: Boolean = false,
    flipLateralFilter: Boolean = false,
): List<FloatArray> {
    if (points.size < 8) return points
    var p = points
    val ax = anchor[0]
    val az = anchor[2]
    when (key) {
        "bicep" -> {
            val ay = anchor[1]
            val tubeR = if (relaxed) 0.26f else 0.19f
            val tube = filterPointsNearAnchorXYZ(p, ax, ay, az, tubeR)
            if (tube.size >= MIN_CROSS_SECTION_POINTS / 2) p = tube
            val ballR = if (relaxed) 0.28f else 0.22f
            val nearArm = filterPointsNearAnchorXZ(p, ax, az, ballR)
            if (nearArm.size >= MIN_CROSS_SECTION_POINTS / 2) p = nearArm
            val thr = if (relaxed) -0.065f else -0.115f
            if (!flipLateralFilter) {
                val left = p.filter { it[0] < thr }
                p = when {
                    left.size >= MIN_CROSS_SECTION_POINTS / 2 -> left
                    relaxed && left.isNotEmpty() -> left
                    else -> p
                }
                if (!relaxed) {
                    val outer = p.filter { it[0] < -0.095f }
                    if (outer.size >= 8) p = outer
                }
            } else {
                val right = p.filter { it[0] > -thr }
                p = when {
                    right.size >= MIN_CROSS_SECTION_POINTS / 2 -> right
                    relaxed && right.isNotEmpty() -> right
                    else -> p
                }
                if (!relaxed) {
                    val outer = p.filter { it[0] > 0.095f }
                    if (outer.size >= 8) p = outer
                }
            }
        }

        "forearm" -> {
            val ay = anchor[1]
            /* Larger inclusion than bicep — forearm slice often elongates; strict tube was clipping the loop */
            val tubeR = if (relaxed) 0.38f else 0.31f
            val tube = filterPointsNearAnchorXYZ(p, ax, ay, az, tubeR)
            if (tube.size >= MIN_CROSS_SECTION_POINTS / 2) p = tube
            val ballR = if (relaxed) 0.40f else 0.34f
            val nearArm = filterPointsNearAnchorXZ(p, ax, az, ballR)
            if (nearArm.size >= MIN_CROSS_SECTION_POINTS / 2) p = nearArm
            val thr = if (relaxed) 0.048f else 0.082f
            if (!flipLateralFilter) {
                val right = p.filter { it[0] > thr }
                p = when {
                    right.size >= MIN_CROSS_SECTION_POINTS / 2 -> right
                    relaxed && right.isNotEmpty() -> right
                    else -> p
                }
            } else {
                val left = p.filter { it[0] < -thr }
                p = when {
                    left.size >= MIN_CROSS_SECTION_POINTS / 2 -> left
                    relaxed && left.isNotEmpty() -> left
                    else -> p
                }
            }
        }

        "thigh" -> {
            val ballR = if (relaxed) 0.42f else 0.38f
            val nearLeg = filterPointsNearAnchorXZ(p, ax, az, ballR)
            if (nearLeg.size >= MIN_CROSS_SECTION_POINTS / 2) p = nearLeg
            val thr = if (relaxed) -0.035f else -0.048f
            if (!flipLateralFilter) {
                val leg = p.filter { it[0] < thr }
                p = when {
                    leg.size >= MIN_CROSS_SECTION_POINTS / 2 -> leg
                    relaxed && leg.isNotEmpty() -> leg
                    else -> p
                }
            } else {
                val leg = p.filter { it[0] > -thr }
                p = when {
                    leg.size >= MIN_CROSS_SECTION_POINTS / 2 -> leg
                    relaxed && leg.isNotEmpty() -> leg
                    else -> p
                }
            }
        }

        "calf" -> {
            val ballR = if (relaxed) 0.42f else 0.38f
            val nearLeg = filterPointsNearAnchorXZ(p, ax, az, ballR)
            if (nearLeg.size >= MIN_CROSS_SECTION_POINTS / 2) p = nearLeg
            val thr = if (relaxed) 0.035f else 0.048f
            if (!flipLateralFilter) {
                val leg = p.filter { it[0] > thr }
                p = when {
                    leg.size >= MIN_CROSS_SECTION_POINTS / 2 -> leg
                    relaxed && leg.isNotEmpty() -> leg
                    else -> p
                }
            } else {
                val leg = p.filter { it[0] < -thr }
                p = when {
                    leg.size >= MIN_CROSS_SECTION_POINTS / 2 -> leg
                    relaxed && leg.isNotEmpty() -> leg
                    else -> p
                }
            }
        }

        "neck" -> {
            val prox = filterPointsNearAnchorXZ(p, ax, az, if (relaxed) 0.26f else 0.22f)
            if (prox.size >= MIN_CROSS_SECTION_POINTS / 2) p = prox
            val narrow = p.filter { abs(it[0]) < 0.27f }
            if (narrow.size >= MIN_CROSS_SECTION_POINTS / 2) p = narrow
            if (!relaxed) p = filterTorsoMedianRadius(p, factor = 1.14f)
        }
        /* Keep full acromion breadth — do not median-trim like torso bands */
        "shoulders" -> Unit
        "chest" -> {
            /*
             * Disk centered on **midline** (x=0): blended anchors often drift in +x / −x and asymmetrically
             * clip the far lateral chest (classically the **right** side when anchor skews left).
             */
            val prox = filterPointsNearAnchorXZ(p, 0f, az, if (relaxed) 0.68f else 0.62f)
            if (prox.size >= MIN_CROSS_SECTION_POINTS / 2) p = prox
            val f1 = if (relaxed) 1.34f else 1.28f
            p = filterTorsoMedianRadiusAboutMidline(p, factor = f1)
            /* Symmetric strip of extreme lateral lobes (arms on horizontal chest cut) — rules use abs(x), no anchor bias */
            if (!relaxed) p = filterChestStripSymmetricArmWings(p)
        }

        "upperWaist", "waist", "lowerWaist" -> {
            val prox = filterPointsNearAnchorXZ(p, 0f, az, if (relaxed) 0.58f else 0.52f)
            if (prox.size >= MIN_CROSS_SECTION_POINTS / 2) p = prox
            val f1 = if (relaxed) 1.42f else 1.36f
            p = filterTorsoMedianRadiusAboutMidline(p, factor = f1)
            if (!relaxed) p = filterTorsoMedianRadiusAboutMidline(p, factor = 1.22f)
        }

        else -> Unit
    }
    return p
}

/** Keeps samples whose horizontal distance to the band anchor matches the torso cylinder, dropping arm lobes. */
private fun filterPointsNearAnchorXZ(
    points: List<FloatArray>,
    ax: Float,
    az: Float,
    maxDist: Float,
): List<FloatArray> {
    val md = maxDist * maxDist
    val out = ArrayList<FloatArray>(points.size)
    for (p in points) {
        val dx = p[0] - ax
        val dz = p[2] - az
        if (dx * dx + dz * dz <= md) out.add(p)
    }
    return out
}

/** Tight 3D ball around limb anchor — strips torso hits on oblique arm slices. */
private fun filterPointsNearAnchorXYZ(
    points: List<FloatArray>,
    ax: Float,
    ay: Float,
    az: Float,
    maxDist: Float,
): List<FloatArray> {
    val md = maxDist * maxDist
    val out = ArrayList<FloatArray>(points.size)
    for (p in points) {
        val dx = p[0] - ax
        val dy = p[1] - ay
        val dz = p[2] - az
        if (dx * dx + dy * dy + dz * dz <= md) out.add(p)
    }
    return out
}

/** Radial distance from **origin** in XZ — OK when mesh is centered; can skew one lateral side if not. */
private fun filterTorsoMedianRadius(points: List<FloatArray>, factor: Float): List<FloatArray> {
    if (points.size < 6) return points
    val rs = points.map { hypot(it[0].toDouble(), it[2].toDouble()).toFloat() }.sorted()
    val medianR = rs[rs.size / 2].coerceAtLeast(0.055f)
    val cutoff = medianR * factor
    val out = points.filter { hypot(it[0].toDouble(), it[2].toDouble()).toFloat() <= cutoff }
    return if (out.size >= 8) out else points
}

/**
 * Torso slice trim using radius √(x² + (z−zMid)²) with **zMid** = slice median depth — symmetric left/right
 * so an off-center mesh origin does not shave only one breast side.
 */
private fun filterTorsoMedianRadiusAboutMidline(
    points: List<FloatArray>,
    factor: Float
): List<FloatArray> {
    if (points.size < 6) return points
    val zs = points.map { it[2] }.sorted()
    val zMid = zs[zs.size / 2]
    val rs = points.map {
        hypot(it[0].toDouble(), (it[2] - zMid).toDouble()).toFloat()
    }.sorted()
    val medianR = rs[rs.size / 2].coerceAtLeast(0.055f)
    val cutoff = medianR * factor
    val out = points.filter {
        hypot(it[0].toDouble(), (it[2] - zMid).toDouble()).toFloat() <= cutoff
    }
    return if (out.size >= 8) out else points
}

/**
 * On a horizontal chest cut, arms appear as **symmetric** far-lateral lobes with radius much larger than
 * the ribcage interior. Uses only **abs(x)** and midline-centered radius — does not reintroduce left/right skew.
 */
private fun filterChestStripSymmetricArmWings(points: List<FloatArray>): List<FloatArray> {
    if (points.size < 10) return points
    val zs = points.map { it[2] }.sorted()
    val zMid = zs[zs.size / 2]
    fun rSym(p: FloatArray): Float =
        hypot(p[0].toDouble(), (p[2] - zMid).toDouble()).toFloat()

    val band = points.filter { abs(it[0]) < 0.19f }
    if (band.size < 6) return points
    val refRs = band.map { rSym(it) }.sorted()
    /* ~upper quartile of “core” torso — generous shell reference */
    val qIdx = ((refRs.size - 1) * 3) / 4
    val refR = refRs[qIdx].coerceAtLeast(0.065f)
    val out = points.filter { p ->
        val ax = abs(p[0])
        val r = rSym(p)
        !(ax > 0.31f && r > refR * 1.48f)
    }
    return if (out.size >= 8) out else points
}

private fun collectSlicePoints(
    vertices: List<FloatArray>,
    usedVertices: BooleanArray,
    faces: List<IntArray>,
    planePoint: FloatArray,
    planeNormal: FloatArray,
): List<FloatArray> {
    val list = ArrayList<FloatArray>(256)
    for (face in faces) {
        val n = face.size
        if (n < 2) continue
        for (i in 0 until n) {
            val ia = face[i]
            val ib = face[(i + 1) % n]
            if (!usedVertices[ia] || !usedVertices[ib]) continue
            intersectEdgeWithPlane(vertices[ia], vertices[ib], planePoint, planeNormal)
                ?.let { list.add(it) }
        }
    }
    return dedupeSlicePoints(list)
}

/**
 * Intersection of segment `p`→`q` with the plane through `planePoint` with unit `planeNormal`.
 * Returns null when the segment doesn't cross the plane. Endpoints exactly on the plane are
 * returned verbatim; double on-plane edges are dropped to avoid duplicates with adjacent edges.
 */
internal fun intersectEdgeWithPlane(
    p: FloatArray,
    q: FloatArray,
    planePoint: FloatArray,
    planeNormal: FloatArray,
): FloatArray? {
    val nx = planeNormal[0]
    val ny = planeNormal[1]
    val nz = planeNormal[2]
    val dp = (p[0] - planePoint[0]) * nx +
            (p[1] - planePoint[1]) * ny +
            (p[2] - planePoint[2]) * nz
    val dq = (q[0] - planePoint[0]) * nx +
            (q[1] - planePoint[1]) * ny +
            (q[2] - planePoint[2]) * nz
    val eps = 1e-5f
    return when {
        abs(dp) < eps && abs(dq) < eps -> null
        abs(dp) < eps -> floatArrayOf(p[0], p[1], p[2])
        abs(dq) < eps -> floatArrayOf(q[0], q[1], q[2])
        dp * dq > 0f -> null
        else -> {
            val denom = dp - dq
            if (abs(denom) < 1e-12f) return null
            val t = (dp / denom).coerceIn(0f, 1f)
            floatArrayOf(
                p[0] + t * (q[0] - p[0]),
                p[1] + t * (q[1] - p[1]),
                p[2] + t * (q[2] - p[2]),
            )
        }
    }
}

private fun dedupeSlicePoints(points: List<FloatArray>): List<FloatArray> {
    if (points.isEmpty()) return points
    val seen = HashSet<String>()
    val out = ArrayList<FloatArray>(points.size)
    val q = 1e4f
    for (p in points) {
        val k = "${(p[0] * q).toInt()}_${(p[2] * q).toInt()}"
        if (seen.add(k)) out.add(p)
    }
    return out
}

/**
 * Sort the slice points so the polyline traces the contour without diagonal chord jumps.
 * Points are projected onto the slice plane's local 2D basis (u, v) and sorted by `atan2(v, u)`
 * around the slice's own centroid. This is correct for any plane orientation — a horizontal
 * plane reduces to an XZ polar sort, a tilted limb plane sorts in the limb's own cross-section.
 */
private fun orderAroundSliceCentroid(
    points: List<FloatArray>,
    planeNormal: FloatArray,
): List<FloatArray> {
    if (points.size < 3) return points
    var sx = 0f
    var sy = 0f
    var sz = 0f
    for (p in points) {
        sx += p[0]; sy += p[1]; sz += p[2]
    }
    val n = points.size.toFloat()
    val cx = sx / n;
    val cy = sy / n;
    val cz = sz / n
    val (u, v) = inPlaneBasis(planeNormal)
    return points.sortedBy {
        val dx = it[0] - cx
        val dy = it[1] - cy
        val dz = it[2] - cz
        val pu = dx * u[0] + dy * u[1] + dz * u[2]
        val pv = dx * v[0] + dy * v[1] + dz * v[2]
        atan2(pv.toDouble(), pu.toDouble())
    }
}

/**
 * Builds an orthonormal basis (u, v) lying in the plane with the given `normal`.
 * Picks a non-parallel reference axis to avoid degeneracy when normal ~ Y.
 */
internal fun inPlaneBasis(normal: FloatArray): Pair<FloatArray, FloatArray> {
    val ref = if (abs(normal[1]) < 0.9f) floatArrayOf(0f, 1f, 0f) else floatArrayOf(1f, 0f, 0f)
    var ux = ref[1] * normal[2] - ref[2] * normal[1]
    var uy = ref[2] * normal[0] - ref[0] * normal[2]
    var uz = ref[0] * normal[1] - ref[1] * normal[0]
    val ulen = sqrt((ux * ux + uy * uy + uz * uz).toDouble()).toFloat()
    if (ulen < 1e-6f) return floatArrayOf(1f, 0f, 0f) to floatArrayOf(0f, 0f, 1f)
    ux /= ulen; uy /= ulen; uz /= ulen
    val vx = normal[1] * uz - normal[2] * uy
    val vy = normal[2] * ux - normal[0] * uz
    val vz = normal[0] * uy - normal[1] * ux
    return floatArrayOf(ux, uy, uz) to floatArrayOf(vx, vy, vz)
}

private fun packXYZ(points: List<FloatArray>): FloatArray {
    val out = FloatArray(points.size * 3)
    var o = 0
    for (p in points) {
        out[o++] = p[0]
        out[o++] = p[1]
        out[o++] = p[2]
    }
    return out
}
