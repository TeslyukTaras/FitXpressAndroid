package com.hexis.bi.ui.avatar

import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Torso cross-section **sample selection**: builds candidate horizontal slice samples down the torso
 * and picks the best one per measurement band (shoulders / chest / waist …), rejecting arm-wing
 * contamination. Feeds [buildMeasurementGuide].
 */

/** Horizontal / near-horizontal torso bands — slice loop choice uses scoring, not max area alone. */
internal val TorsoCircumferenceKeys: Set<String> = BodyMeasurementRegion.torsoCircumferenceKeys

/** Torso keys where raw-slice fallback uses quantile-based narrowing instead of only fixed thresholds. */
internal val TorsoGentleFallbackKeys: Set<String> = BodyMeasurementRegion.torsoGentleFallbackKeys

internal data class TorsoSliceSample(
    val y: Float,
    val loop: List<FloatArray>,
    val centerX: Float,
    val centerZ: Float,
    val widthX: Float,
    val depthZ: Float,
    val area: Float,
    val perimeter: Float,
) {
    val aspectXZ: Float get() = widthX / depthZ.coerceAtLeast(1e-5f)
}

internal data class TorsoProfile(
    val shoulders: TorsoSliceSample?,
    val chest: TorsoSliceSample?,
    val upperWaist: TorsoSliceSample?,
    val waist: TorsoSliceSample?,
    val lowerWaist: TorsoSliceSample?,
)

private const val TORSO_PROFILE_SLICE_COUNT = 96
private const val TORSO_MIN_LOOP_POINTS = 8
private const val TORSO_ARM_ASPECT_LIMIT = 4.1f
private const val TORSO_MIN_AREA = 0.005f

/** Chest slice width vs waist / lower torso — tight to reject arm-inclusive contours (stricter set). */
private const val CHEST_MAX_WIDTH_TO_WAIST_RATIO = 1.42f
private const val CHEST_MAX_WIDTH_TO_LOWER_WAIST_RATIO = 1.32f
private const val CHEST_MAX_CENTER_X_ABS = 0.045f
private const val CHEST_ARM_WING_RATIO = 0.12f

private const val OBESE_WAIST_FLATNESS_RATIO = 0.52f

/** Normalized-space nudge below template waist Y when area profile is flat (smaller = less drop). */
private const val OBESE_WAIST_DOWN_SHIFT = 0.038f
private const val WAIST_MIN_BELOW_CHEST = 0.18f

/** Upper-waist ring must sit clearly above the natural (mid) waist band. */
private const val TORSO_UPPER_ABOVE_WAIST_MIN = 0.055f

/** Midline X + Y/Z from anchors (torso Y/Z come from [buildTorsoProfile] when available). */
internal fun stabilizedSliceAnchorForMeasurement(
    key: String,
    anchor: FloatArray,
): FloatArray =
    if (key in TorsoCircumferenceKeys) {
        floatArrayOf(0f, anchor[1], anchor[2])
    } else {
        anchor
    }

internal fun dynamicMeasurementAnchor(
    key: String,
    fallback: FloatArray,
    torsoProfile: TorsoProfile?,
): FloatArray {
    val sample = when (key) {
        BodyMeasurementKeys.Shoulders -> torsoProfile?.shoulders
        BodyMeasurementKeys.Chest -> torsoProfile?.chest
        BodyMeasurementKeys.UpperWaist -> torsoProfile?.upperWaist
        BodyMeasurementKeys.Waist -> torsoProfile?.waist
        BodyMeasurementKeys.LowerWaist -> torsoProfile?.lowerWaist
        else -> null
    } ?: return fallback

    return floatArrayOf(
        0f,
        sample.y,
        sample.centerZ,
    )
}

internal fun buildTorsoProfile(
    vertices: List<FloatArray>,
    usedVertices: BooleanArray,
    faces: List<IntArray>,
): TorsoProfile? {
    val samples = buildTorsoSliceSamples(vertices, usedVertices, faces)
    if (samples.size < 8) return null

    val shoulders = pickShouldersSample(samples)
    val roughChest = pickChestSample(samples, shoulders)
    val waist = pickWaistSample(samples, roughChest)
    val chest = pickChestSampleStrict(samples, shoulders, waist) ?: roughChest
    // Upper waist = abdomen band between chest and natural waist — bias toward chest so it
    // does not collapse onto the same height as [waist] ("Mid Waist" / narrowest torso).
    var upperWaist = pickNearestValidSample(
        samples,
        targetY = if (chest != null && waist != null) {
            chest.y * 0.62f + waist.y * 0.38f
        } else {
            MeasurementVisualAnchors.fallbackAnchorPosition(BodyMeasurementKeys.UpperWaist)?.get(1) ?: 0.38f
        },
    )
    if (waist != null && chest != null) {
        if (upperWaist == null || upperWaist.y <= waist.y + TORSO_UPPER_ABOVE_WAIST_MIN) {
            val replTarget = chest.y * 0.70f + waist.y * 0.30f
            upperWaist = pickNearestValidSample(samples, replTarget)
                ?: samples
                    .filter {
                        it.aspectXZ <= TORSO_ARM_ASPECT_LIMIT &&
                                it.y >= waist.y + TORSO_UPPER_ABOVE_WAIST_MIN &&
                                it.y <= chest.y - 0.04f
                    }
                    .minByOrNull { abs(it.y - replTarget) }
        }
    }
    val lowerWaist = pickNearestValidSample(
        samples,
        targetY = if (waist != null) {
            waist.y - 0.16f
        } else {
            MeasurementVisualAnchors.fallbackAnchorPosition(BodyMeasurementKeys.LowerWaist)?.get(1) ?: 0.07f
        },
    )

    return TorsoProfile(
        shoulders = shoulders,
        chest = chest,
        upperWaist = upperWaist,
        waist = waist,
        lowerWaist = lowerWaist,
    )
}

private fun buildTorsoSliceSamples(
    vertices: List<FloatArray>,
    usedVertices: BooleanArray,
    faces: List<IntArray>,
): List<TorsoSliceSample> {
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    vertices.forEachIndexed { index, v ->
        if (!usedVertices[index]) return@forEachIndexed
        minY = min(minY, v[1])
        maxY = max(maxY, v[1])
    }
    if (minY >= maxY) return emptyList()

    val out = ArrayList<TorsoSliceSample>(TORSO_PROFILE_SLICE_COUNT)
    val range = maxY - minY
    for (i in 0 until TORSO_PROFILE_SLICE_COUNT) {
        val y = minY + range * (i + 0.5f) / TORSO_PROFILE_SLICE_COUNT.toFloat()
        buildCentralTorsoSliceSample(vertices, usedVertices, faces, y)?.let { sample ->
            if (sample.area >= TORSO_MIN_AREA && sample.loop.size >= TORSO_MIN_LOOP_POINTS) {
                out.add(sample)
            }
        }
    }
    return out
}

private fun buildCentralTorsoSliceSample(
    vertices: List<FloatArray>,
    usedVertices: BooleanArray,
    faces: List<IntArray>,
    y: Float,
): TorsoSliceSample? {
    val planeNormal = floatArrayOf(0f, 1f, 0f)
    val planePoint = floatArrayOf(0f, y, 0f)
    val segments = collectPlaneFaceSegments(vertices, usedVertices, faces, planePoint, planeNormal)
    if (segments.size < TORSO_MIN_LOOP_POINTS) return null

    val keyToId = LinkedHashMap<String, Int>()
    val coords = ArrayList<FloatArray>()
    fun idFor(p: FloatArray): Int {
        val key = sliceVertexKey(p)
        val existing = keyToId[key]
        if (existing != null) return existing
        val id = coords.size
        keyToId[key] = id
        coords.add(floatArrayOf(p[0], p[1], p[2]))
        return id
    }

    val graph = HashMap<Int, MutableSet<Int>>()
    fun addEdge(a: Int, b: Int) {
        if (a == b) return
        graph.getOrPut(a) { mutableSetOf() }.add(b)
        graph.getOrPut(b) { mutableSetOf() }.add(a)
    }
    for ((a, b) in segments) {
        addEdge(idFor(a), idFor(b))
    }

    val cycles = ArrayList<List<Int>>()
    val assigned = HashSet<Int>()
    for (seed in graph.keys.sorted()) {
        if (seed in assigned) continue
        val component = HashSet<Int>()
        val stack = ArrayList<Int>()
        stack.add(seed)
        while (stack.isNotEmpty()) {
            val v = stack.removeAt(stack.lastIndex)
            if (!component.add(v)) continue
            assigned.add(v)
            graph[v]?.forEach { n -> stack.add(n) }
        }
        val cycle = traceDegreeTwoCycle(graph, component) ?: continue
        if (cycle.size >= TORSO_MIN_LOOP_POINTS) cycles.add(cycle)
    }
    if (cycles.isEmpty()) return null

    val best = cycles.minByOrNull { cycle ->
        centralTorsoCycleProfileScore(y, cycle, coords, planeNormal)
    } ?: return null

    val loop = best.map { index -> coords[index].clone() }
    return makeTorsoSliceSample(y, loop, planeNormal)
}

private fun centralTorsoCycleProfileScore(
    planeY: Float,
    cycle: List<Int>,
    coords: List<FloatArray>,
    planeNormal: FloatArray,
): Float {
    val loopPts = cycle.map { coords[it] }
    val sample = makeTorsoSliceSample(planeY, loopPts, planeNormal) ?: return Float.MAX_VALUE

    val midlinePenalty = abs(sample.centerX) * 10f
    val armAspectPenalty = max(0f, sample.aspectXZ - TORSO_ARM_ASPECT_LIMIT) * 8f
    val compactness = (sample.perimeter * sample.perimeter) / sample.area.coerceAtLeast(1e-6f)

    return midlinePenalty +
            armAspectPenalty +
            compactness * 0.015f -
            sample.area * 0.25f
}

private fun makeTorsoSliceSample(
    y: Float,
    loop: List<FloatArray>,
    planeNormal: FloatArray,
): TorsoSliceSample? {
    if (loop.size < 3) return null

    val xs = loop.map { it[0] }.sorted()
    val zs = loop.map { it[2] }.sorted()
    val li = loop.lastIndex
    val q05 = (li * 0.05f).toInt().coerceIn(0, li)
    val q95 = (li * 0.95f).toInt().coerceIn(0, li)
    val q10 = (li * 0.10f).toInt().coerceIn(0, li)
    val q90 = (li * 0.90f).toInt().coerceIn(0, li)

    var cx = 0f
    var cz = 0f
    loop.forEach { p ->
        cx += p[0]
        cz += p[2]
    }
    cx /= loop.size
    cz /= loop.size

    val ids = loop.indices.toList()
    val area = polygonAreaUV(ids, loop, planeNormal)
    val perimeter = perimeterIn3d(ids, loop)

    return TorsoSliceSample(
        y = y,
        loop = loop,
        centerX = cx,
        centerZ = cz,
        widthX = xs[q95] - xs[q05],
        depthZ = zs[q90] - zs[q10],
        area = area,
        perimeter = perimeter,
    )
}

private fun hasArmWingContamination(
    sample: TorsoSliceSample,
    torsoReferenceWidth: Float,
): Boolean {
    val xs = sample.loop.map { it[0] }
    val left = xs.minOrNull() ?: return false
    val right = xs.maxOrNull() ?: return false
    val halfRef = torsoReferenceWidth * 0.5f

    val leftWing = abs(left) - halfRef
    val rightWing = abs(right) - halfRef
    val allowedWing = torsoReferenceWidth * CHEST_ARM_WING_RATIO

    return leftWing > allowedWing || rightWing > allowedWing
}

private fun pickShouldersSample(samples: List<TorsoSliceSample>): TorsoSliceSample? {
    val fallbackY = MeasurementVisualAnchors.fallbackAnchorPosition(BodyMeasurementKeys.Shoulders)?.get(1) ?: 1.0f
    val candidates = samples
        .filter { abs(it.y - fallbackY) <= 0.20f }
        .filter { it.aspectXZ <= TORSO_ARM_ASPECT_LIMIT }
    // Selecting maximum width pulls this band down into the arms; keep it at the shelf height.
    return candidates.minByOrNull { sample ->
        abs(sample.y - fallbackY) * 8f +
                abs(sample.centerX) * 2f -
                sample.widthX * 0.20f -
                sample.area * 0.10f
    } ?: samples.minByOrNull { abs(it.y - fallbackY) }
}

private fun pickChestSample(
    samples: List<TorsoSliceSample>,
    shoulders: TorsoSliceSample?,
): TorsoSliceSample? {
    val fallbackY = MeasurementVisualAnchors.fallbackAnchorPosition(BodyMeasurementKeys.Chest)?.get(1) ?: 0.67f
    val shoulderY = shoulders?.y ?: (fallbackY + 0.22f)
    val candidates = samples
        .filter { it.y < shoulderY - 0.06f }
        .filter { abs(it.y - fallbackY) <= 0.30f }
        .filter { it.aspectXZ <= TORSO_ARM_ASPECT_LIMIT }
    return candidates.minByOrNull { sample ->
        abs(sample.y - fallbackY) * 6f +
                abs(sample.centerX) * 4f -
                sample.area * 0.35f -
                sample.depthZ * 0.20f
    } ?: samples
        .filter { it.aspectXZ <= TORSO_ARM_ASPECT_LIMIT }
        .filter { abs(it.centerX) <= CHEST_MAX_CENTER_X_ABS * 1.8f }
        .minByOrNull { abs(it.y - fallbackY) }
}

private fun pickChestSampleStrict(
    samples: List<TorsoSliceSample>,
    shoulders: TorsoSliceSample?,
    waist: TorsoSliceSample?,
): TorsoSliceSample? {
    val fallbackY = MeasurementVisualAnchors.fallbackAnchorPosition(BodyMeasurementKeys.Chest)?.get(1) ?: 0.67f
    val shoulderY = shoulders?.y ?: (fallbackY + 0.22f)

    val waistRef = waist ?: pickNearestValidSample(
        samples,
        MeasurementVisualAnchors.fallbackAnchorPosition(BodyMeasurementKeys.Waist)?.get(1) ?: 0.24f,
    )
    val torsoRefWidth = waistRef?.widthX ?: samples
        .filter { abs(it.y - fallbackY) <= 0.35f }
        .minByOrNull { abs(it.centerX) }
        ?.widthX
    ?: return null

    val lowerTorsoRefWidth = samples
        .filter { it.y < fallbackY - 0.12f }
        .filter { it.aspectXZ <= TORSO_ARM_ASPECT_LIMIT }
        .maxByOrNull { it.widthX }
        ?.widthX
        ?: torsoRefWidth

    val candidates = samples
        .asSequence()
        .filter { it.y < shoulderY - 0.12f }
        .filter { abs(it.y - fallbackY) <= 0.34f }
        .filter { it.aspectXZ <= TORSO_ARM_ASPECT_LIMIT }
        .filter { abs(it.centerX) <= CHEST_MAX_CENTER_X_ABS }
        .filter { it.widthX <= torsoRefWidth * CHEST_MAX_WIDTH_TO_WAIST_RATIO }
        .filter { it.widthX <= lowerTorsoRefWidth * CHEST_MAX_WIDTH_TO_LOWER_WAIST_RATIO }
        .filter { !hasArmWingContamination(it, lowerTorsoRefWidth) }
        .toList()

    // After excluding arm-contaminated loops, preserve the intended upper-chest height.
    return candidates.minByOrNull { sample ->
        abs(sample.y - fallbackY) * 8f +
                abs(sample.centerX) * 8f -
                sample.area * 0.20f -
                sample.depthZ * 0.20f
    }
}

private fun pickWaistSample(
    samples: List<TorsoSliceSample>,
    chest: TorsoSliceSample?,
): TorsoSliceSample? {
    val fallbackY = MeasurementVisualAnchors.fallbackAnchorPosition(BodyMeasurementKeys.Waist)?.get(1) ?: 0.24f
    val chestY = chest?.y ?: (fallbackY + 0.35f)

    val candidates = samples
        .filter { it.y < chestY - WAIST_MIN_BELOW_CHEST }
        .filter { abs(it.y - fallbackY) <= 0.42f }
        .filter { it.aspectXZ <= TORSO_ARM_ASPECT_LIMIT }
        .filter { abs(it.centerX) <= 0.07f }

    if (candidates.isEmpty()) {
        return samples
            .filter { it.aspectXZ <= TORSO_ARM_ASPECT_LIMIT }
            .minByOrNull { abs(it.y - fallbackY) }
    }

    val minArea = candidates.minOf { it.area }
    val maxArea = candidates.maxOf { it.area }
    val areaRange = (maxArea - minArea).coerceAtLeast(1e-6f)

    val isFlatObeseProfile = areaRange / maxArea.coerceAtLeast(1e-6f) < OBESE_WAIST_FLATNESS_RATIO

    return if (isFlatObeseProfile) {
        val targetY = fallbackY - OBESE_WAIST_DOWN_SHIFT
        candidates.minByOrNull { sample ->
            abs(sample.y - targetY) * 2.4f +
                    abs(sample.centerX) * 4f +
                    sample.aspectXZ * 0.08f
        }
    } else {
        candidates.minByOrNull { sample ->
            val normalizedArea = (sample.area - minArea) / areaRange
            normalizedArea * 1.35f +
                    abs(sample.y - fallbackY) * 0.65f +
                    abs(sample.centerX) * 4f
        }
    }
}

private fun pickNearestValidSample(
    samples: List<TorsoSliceSample>,
    targetY: Float,
): TorsoSliceSample? =
    samples
        .filter { it.aspectXZ <= TORSO_ARM_ASPECT_LIMIT }
        .minByOrNull { abs(it.y - targetY) }
        ?: samples.minByOrNull { abs(it.y - targetY) }
