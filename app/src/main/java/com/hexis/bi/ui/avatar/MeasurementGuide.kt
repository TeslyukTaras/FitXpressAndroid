package com.hexis.bi.ui.avatar

import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Builds the [MetricAvatarMeasurementGuide] — model-space anchors, per-key slice-plane normals, and
 * the fallback/virtual anchors — that the renderer and frame solver consume for the Visual overlay.
 */

private val measurementAnchorKeys: List<String> = BodyMeasurementRegion.measurementAnchorKeys

internal const val MIN_CROSS_SECTION_POINTS = 12

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
    val p = anchors[BodyMeasurementKeys.Neck]
        ?: MeasurementVisualAnchors.fallbackAnchorPosition(BodyMeasurementKeys.Neck)
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
    BodyMeasurementKeys.Neck -> neckSlicePlaneNormal()
    BodyMeasurementKeys.Shoulders, BodyMeasurementKeys.Chest,
    BodyMeasurementKeys.UpperWaist, BodyMeasurementKeys.Waist, BodyMeasurementKeys.LowerWaist -> floatArrayOf(0f, 1f, 0f)
    // Keep biceps on the mid-upper-arm level; an arm-axis plane drops visibly to the elbow.
    BodyMeasurementKeys.Bicep -> floatArrayOf(0f, 1f, 0f)
    /* Right forearm: mirrored upper-arm root → forearm so cut lies in the arm plane, not across torso. */
    BodyMeasurementKeys.Forearm -> {
        val bi = anchors[BodyMeasurementKeys.Bicep]
        val fo = anchors[key]
        if (bi != null && fo != null) limbAxis(mirrorX(bi), fo) else floatArrayOf(0f, 1f, 0f)
    }

    BodyMeasurementKeys.Thigh -> limbAxis(virtualHipFor(anchors), anchors[key])
    /* Right calf: axis from mirrored left-thigh → right calf. */
    BodyMeasurementKeys.Calf -> {
        val th = anchors[BodyMeasurementKeys.Thigh]
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
    BodyMeasurementKeys.Bicep -> floatArrayOf(0f, 1f, 0f)
    BodyMeasurementKeys.Forearm -> {
        val bi = anchors[BodyMeasurementKeys.Bicep]
        if (bi != null) limbAxis(bi, oppositeBandAnchor) else floatArrayOf(0f, 1f, 0f)
    }

    BodyMeasurementKeys.Thigh -> {
        val hip = virtualHipMirrored(anchors)
        if (hip != null) limbAxis(hip, oppositeBandAnchor) else floatArrayOf(0f, 1f, 0f)
    }

    BodyMeasurementKeys.Calf -> {
        val th = anchors[BodyMeasurementKeys.Thigh]
        if (th != null) limbAxis(th, oppositeBandAnchor) else floatArrayOf(0f, 1f, 0f)
    }

    else -> slicePlaneNormalFor(key, anchors)
}

/** Hip root above the **right** leg when the scanned thigh anchor is on the left (mirrored X). */
private fun virtualHipMirrored(anchors: Map<String, FloatArray>): FloatArray? {
    val lw = anchors[BodyMeasurementKeys.LowerWaist] ?: return null
    val th = anchors[BodyMeasurementKeys.Thigh] ?: return null
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
    val lw = anchors[BodyMeasurementKeys.LowerWaist] ?: return null
    val th = anchors[BodyMeasurementKeys.Thigh] ?: return null
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
    if (key == BodyMeasurementKeys.Forearm || key == BodyMeasurementKeys.Bicep || key == BodyMeasurementKeys.Thigh) return ref.copyOf()
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
