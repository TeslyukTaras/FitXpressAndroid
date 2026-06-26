package com.hexis.bi.ui.avatar

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Avatar **model-space math**: torso slice sampling, mesh cross-section contours for circumference
 * labels, and the guide payload for the Visual overlay. Camera/projection constants and the static
 * leader anchors live in [MetricAvatarCamera] / [MeasurementVisualAnchors].
 */

/** Horizontal / near-horizontal torso bands — slice loop choice uses scoring, not max area alone. */
private val TorsoCircumferenceKeys = setOf(
    "neck",
    "shoulders",
    "chest",
    "upperWaist",
    "waist",
    "lowerWaist",
)

/** Torso keys where raw-slice fallback uses quantile-based narrowing instead of only fixed thresholds. */
private val TorsoGentleFallbackKeys = setOf("chest", "upperWaist", "waist", "lowerWaist")

private data class TorsoSliceSample(
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

private data class TorsoProfile(
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
private fun stabilizedSliceAnchorForMeasurement(
    key: String,
    anchor: FloatArray,
): FloatArray =
    if (key in TorsoCircumferenceKeys) {
        floatArrayOf(0f, anchor[1], anchor[2])
    } else {
        anchor
    }

private fun dynamicMeasurementAnchor(
    key: String,
    fallback: FloatArray,
    torsoProfile: TorsoProfile?,
): FloatArray {
    val sample = when (key) {
        "shoulders" -> torsoProfile?.shoulders
        "chest" -> torsoProfile?.chest
        "upperWaist" -> torsoProfile?.upperWaist
        "waist" -> torsoProfile?.waist
        "lowerWaist" -> torsoProfile?.lowerWaist
        else -> null
    } ?: return fallback

    return floatArrayOf(
        0f,
        sample.y,
        sample.centerZ,
    )
}

private fun buildTorsoProfile(
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
            MeasurementVisualAnchors.fallbackAnchorPosition("upperWaist")?.get(1) ?: 0.38f
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
            MeasurementVisualAnchors.fallbackAnchorPosition("lowerWaist")?.get(1) ?: 0.07f
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
    val fallbackY = MeasurementVisualAnchors.fallbackAnchorPosition("shoulders")?.get(1) ?: 1.0f
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
    val fallbackY = MeasurementVisualAnchors.fallbackAnchorPosition("chest")?.get(1) ?: 0.67f
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
    val fallbackY = MeasurementVisualAnchors.fallbackAnchorPosition("chest")?.get(1) ?: 0.67f
    val shoulderY = shoulders?.y ?: (fallbackY + 0.22f)

    val waistRef = waist ?: pickNearestValidSample(
        samples,
        MeasurementVisualAnchors.fallbackAnchorPosition("waist")?.get(1) ?: 0.24f,
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
    val fallbackY = MeasurementVisualAnchors.fallbackAnchorPosition("waist")?.get(1) ?: 0.24f
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

private val measurementAnchorKeys = listOf(
    "neck", "shoulders", "chest", "forearm", "bicep",
    "upperWaist", "waist", "lowerWaist", "thigh", "calf",
)

private const val MIN_CROSS_SECTION_POINTS = 12

/**
 * Everything the Visual tab overlay needs from the parsed OBJ: point anchors + optional slice polylines.
 */
internal data class MetricAvatarMeasurementGuide(
    val anchorPoints: Map<String, FloatArray>,
    /** Circumference keys: packed model-space [x,y,z, x,y,z, …] ordered around the slice. */
    val crossSectionPolylines: Map<String, FloatArray>,
    /**
     * [BilateralCircumferenceKeys] only: packed polyline for the **opposite** limb (mirror lateral).
     */
    val crossSectionPolylinesOpposite: Map<String, FloatArray> = emptyMap(),
    /** Neck slice plane in model space; used for guide math and parser detail protection. */
    val neckClipPlane: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MetricAvatarMeasurementGuide

        if (anchorPoints != other.anchorPoints) return false
        if (crossSectionPolylines != other.crossSectionPolylines) return false
        if (crossSectionPolylinesOpposite != other.crossSectionPolylinesOpposite) return false
        if (!neckClipPlane.contentEquals(other.neckClipPlane)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = anchorPoints.hashCode()
        result = 31 * result + crossSectionPolylines.hashCode()
        result = 31 * result + crossSectionPolylinesOpposite.hashCode()
        result = 31 * result + neckClipPlane.contentHashCode()
        return result
    }
}

internal fun neckSlicePlaneNormal(): FloatArray {
    val rad = Math.toRadians(MetricAvatarCamera.NECK_SLICE_FORWARD_TILT_DEG.toDouble())
    val ny = cos(rad).toFloat()
    val nz = sin(rad).toFloat()
    return floatArrayOf(0f, ny, nz)
}

private fun computeNeckClipPlane(anchors: Map<String, FloatArray>): FloatArray {
    val n = neckSlicePlaneNormal()
    val p = anchors["neck"]
        ?: MeasurementVisualAnchors.fallbackAnchorPosition("neck")
        ?: floatArrayOf(0f, 1.06f, 0.13f)
    val d = -(n[0] * p[0] + n[1] * p[1] + n[2] * p[2])
    return floatArrayOf(n[0], n[1], n[2], d)
}

internal fun buildMeasurementGuide(
    vertices: List<FloatArray>,
    usedVertices: BooleanArray,
    faces: List<IntArray>,
): MetricAvatarMeasurementGuide {
    val rawAnchors = computeMeasurementModelAnchors(vertices, usedVertices)
    val baseAnchors = LinkedHashMap<String, FloatArray>()
    for (key in measurementAnchorKeys) {
        baseAnchors[key] = blendAnchorWithFallback(key, rawAnchors[key])
    }

    val torsoProfile = buildTorsoProfile(vertices, usedVertices, faces)

    val anchors = LinkedHashMap<String, FloatArray>()
    for (key in measurementAnchorKeys) {
        val base = baseAnchors[key] ?: continue
        anchors[key] = dynamicMeasurementAnchor(key, base, torsoProfile)
    }

    val slices = LinkedHashMap<String, FloatArray>()
    for (key in CircumferenceVisualKeys) {
        val anchor = anchors[key] ?: continue
        val planeNormal = slicePlaneNormalFor(key, anchors)
        val packed = computeCrossSectionPacked(
            vertices = vertices,
            usedVertices = usedVertices,
            faces = faces,
            measurementKey = key,
            anchor = anchor,
            planeNormal = planeNormal,
            flipLateralFilter = false,
        )
        if (packed != null && packed.size >= MetricAvatarPackedGeometry.MIN_PACKED_POLYLINE_FLOATS) {
            slices[key] = packed
        }
    }
    val oppositeSlices = LinkedHashMap<String, FloatArray>()
    for (key in BilateralCircumferenceKeys) {
        val primary = anchors[key] ?: continue
        val anchorOpp = mirrorX(primary)
        val planeNormalOpp = slicePlaneNormalForOpposite(key, anchors, anchorOpp)
        val packed = computeCrossSectionPacked(
            vertices = vertices,
            usedVertices = usedVertices,
            faces = faces,
            measurementKey = key,
            anchor = anchorOpp,
            planeNormal = planeNormalOpp,
            flipLateralFilter = true,
        )
        if (packed != null && packed.size >= MetricAvatarPackedGeometry.MIN_PACKED_POLYLINE_FLOATS) {
            oppositeSlices[key] = packed
        }
    }
    val neckClipPlane = computeNeckClipPlane(anchors)
    return MetricAvatarMeasurementGuide(anchors, slices, oppositeSlices, neckClipPlane)
}

/**
 * Slice plane normal per measurement, in normalized model space.
 * **Neck** uses a slight forward tilt ([neckSlicePlaneNormal]); shoulders/torso/waists cut horizontally (body axis Y).
 * Limb bands cut **perpendicular to the limb axis**.
 */
private fun slicePlaneNormalFor(
    key: String,
    anchors: Map<String, FloatArray>,
): FloatArray = when (key) {
    "neck" -> neckSlicePlaneNormal()
    "shoulders", "chest",
    "upperWaist", "waist", "lowerWaist" -> floatArrayOf(0f, 1f, 0f)
    // Keep biceps on the mid-upper-arm level; an arm-axis plane drops visibly to the elbow.
    "bicep" -> floatArrayOf(0f, 1f, 0f)
    /* Right forearm: mirrored upper-arm root → forearm so cut lies in the arm plane, not across torso. */
    "forearm" -> {
        val bi = anchors["bicep"]
        val fo = anchors[key]
        if (bi != null && fo != null) limbAxis(mirrorX(bi), fo) else floatArrayOf(0f, 1f, 0f)
    }

    "thigh" -> limbAxis(virtualHipFor(anchors), anchors[key])
    /* Right calf: axis from mirrored left-thigh → right calf. */
    "calf" -> {
        val th = anchors["thigh"]
        val ca = anchors[key]
        if (th != null && ca != null) limbAxis(mirrorX(th), ca) else floatArrayOf(0f, 1f, 0f)
    }

    else -> floatArrayOf(0f, 1f, 0f)
}

/**
 * Slice normal for the **mirror** limb ring ([mirrorX] of the primary band anchor).
 * Primary [slicePlaneNormalFor] targets one lateral side per key; this targets the other.
 */
private fun slicePlaneNormalForOpposite(
    key: String,
    anchors: Map<String, FloatArray>,
    oppositeBandAnchor: FloatArray,
): FloatArray = when (key) {
    "bicep" -> floatArrayOf(0f, 1f, 0f)
    "forearm" -> {
        val bi = anchors["bicep"]
        if (bi != null) limbAxis(bi, oppositeBandAnchor) else floatArrayOf(0f, 1f, 0f)
    }

    "thigh" -> {
        val hip = virtualHipMirrored(anchors)
        if (hip != null) limbAxis(hip, oppositeBandAnchor) else floatArrayOf(0f, 1f, 0f)
    }

    "calf" -> {
        val th = anchors["thigh"]
        if (th != null) limbAxis(th, oppositeBandAnchor) else floatArrayOf(0f, 1f, 0f)
    }

    else -> slicePlaneNormalFor(key, anchors)
}

/** Hip root above the **right** leg when the scanned thigh anchor is on the left (mirrored X). */
private fun virtualHipMirrored(anchors: Map<String, FloatArray>): FloatArray? {
    val lw = anchors["lowerWaist"] ?: return null
    val th = anchors["thigh"] ?: return null
    return floatArrayOf(-th[0], lw[1], th[2])
}

private fun mirrorX(a: FloatArray): FloatArray = floatArrayOf(-a[0], a[1], a[2])

/** Unit vector from `parent` to `band`, falling back to vertical when either is missing. */
private fun limbAxis(parent: FloatArray?, band: FloatArray?): FloatArray {
    if (parent == null || band == null) return floatArrayOf(0f, 1f, 0f)
    val dx = band[0] - parent[0]
    val dy = band[1] - parent[1]
    val dz = band[2] - parent[2]
    val len = sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    if (len < 1e-5f) return floatArrayOf(0f, 1f, 0f)
    return floatArrayOf(dx / len, dy / len, dz / len)
}

/** Synthetic hip joint above the thigh anchor, at lowerWaist height — there's no scanned anchor for it. */
private fun virtualHipFor(anchors: Map<String, FloatArray>): FloatArray? {
    val lw = anchors["lowerWaist"] ?: return null
    val th = anchors["thigh"] ?: return null
    return floatArrayOf(th[0], lw[1], th[2])
}

/**
 * Mesh centroids are biased; blend toward curated fallbacks — heavier on **Y** so band heights match anatomy.
 */
private fun blendAnchorWithFallback(key: String, computed: FloatArray?): FloatArray {
    val ref =
        MeasurementVisualAnchors.fallbackAnchorPosition(key) ?: return computed ?: floatArrayOf(
            0f,
            0f,
            0f
        )
    // These bands identify a specific limb zone; centroid blending visibly slides them
    // toward an adjacent joint on differently proportioned scans.
    if (key == "forearm" || key == "bicep" || key == "thigh") return ref.copyOf()
    if (computed == null) return ref.copyOf()
    val yWeight = 0.68f
    val xzWeight = 0.42f
    return floatArrayOf(
        computed[0] * (1f - xzWeight) + ref[0] * xzWeight,
        computed[1] * (1f - yWeight) + ref[1] * yWeight,
        computed[2] * (1f - xzWeight) + ref[2] * xzWeight,
    )
}

private fun computeMeasurementModelAnchors(
    vertices: List<FloatArray>,
    usedVertices: BooleanArray,
): Map<String, FloatArray> {
    val out = LinkedHashMap<String, FloatArray>()
    for (key in measurementAnchorKeys) {
        val computed = centroidNearFallbackReference(vertices, usedVertices, key)
        val resolved = computed ?: MeasurementVisualAnchors.fallbackAnchorPosition(key)
        if (resolved != null) out[key] = resolved.copyOf()
    }
    return out
}

private fun centroidNearFallbackReference(
    vertices: List<FloatArray>,
    usedVertices: BooleanArray,
    key: String,
): FloatArray? {
    val ref = MeasurementVisualAnchors.fallbackAnchorPosition(key) ?: return null
    val rx = ref[0]
    val ry = ref[1]
    val rz = ref[2]

    fun centroidWithin(maxDistSq: Float, minVerts: Int): FloatArray? {
        var sx = 0f
        var sy = 0f
        var sz = 0f
        var n = 0
        for (i in vertices.indices) {
            if (!usedVertices[i]) continue
            val v = vertices[i]
            val dx = v[0] - rx
            val dy = v[1] - ry
            val dz = v[2] - rz
            if (dx * dx + dy * dy + dz * dz <= maxDistSq) {
                sx += v[0]
                sy += v[1]
                sz += v[2]
                n++
            }
        }
        if (n >= minVerts) return floatArrayOf(sx / n, sy / n, sz / n)
        return null
    }

    centroidWithin(0.45f * 0.45f, minVerts = 4)?.let { return it }
    centroidWithin(0.72f * 0.72f, minVerts = 3)?.let { return it }
    centroidWithin(1.05f * 1.05f, minVerts = 1)?.let { return it }
    return null
}

private fun polygonAreaUV(
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

private val BoundaryLimbKeys = setOf("bicep", "forearm", "thigh", "calf")

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

private fun perimeterIn3d(cycle: List<Int>, coords: List<FloatArray>): Float {
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
private fun filterTorsoFallbackPoints(
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

private fun sliceVertexKey(p: FloatArray): String {
    val q = 1e5f
    return "${(p[0] * q).toLong()}_${(p[1] * q).toLong()}_${(p[2] * q).toLong()}"
}

/** One segment per triangle/plane hit — topology for chaining (not polar sorting). */
private fun collectPlaneFaceSegments(
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
        val hits = ArrayList<FloatArray>(6)
        for (i in 0 until n) {
            val ia = face[i]
            val ib = face[(i + 1) % n]
            if (!usedVertices[ia] || !usedVertices[ib]) continue
            intersectEdgeWithPlane(
                vertices[ia],
                vertices[ib],
                planePoint,
                planeNormal
            )?.let { hits.add(it) }
        }
        if (hits.size == 2) {
            out.add(
                floatArrayOf(hits[0][0], hits[0][1], hits[0][2]) to
                        floatArrayOf(hits[1][0], hits[1][1], hits[1][2]),
            )
        }
    }
    return out
}

/**
 * Chain plane∩triangle segments into closed loops. **Torso** bands: best cycle by [torsoCycleScore]
 * (midline, anchor, compactness). **Limbs**: loop whose centroid is closest to the band anchor
 * (then largest area as tie-break).
 */
private fun buildSliceBoundaryLoopFromMesh(
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
private fun traceDegreeTwoCycle(
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

private fun computeCrossSectionPacked(
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
private fun intersectEdgeWithPlane(
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
private fun inPlaneBasis(normal: FloatArray): Pair<FloatArray, FloatArray> {
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
