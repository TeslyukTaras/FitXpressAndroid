package com.hexis.bi.ui.main.scan.results

import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Single place for avatar **model-space math**, projection (matches GL MVP), fallback anchors,
 * mesh slice contours for circumference labels, and guide payload for the Visual overlay.
 */
internal object MetricAvatarCamera {
    const val MATRIX_SIZE = 16
    const val INITIAL_VIEW_DISTANCE = 3.2f
    const val FOV_DEG = 42f
    const val FRUSTUM_NEAR = 0.1f
    const val FRUSTUM_FAR = 100f
    const val MIN_TAN_HALF_FOV = 0.001f
    const val MIN_ASPECT_FOR_FRAMING = 0.6f
    const val VIEW_DISTANCE_SAFETY_MARGIN = 1.25f
    const val EYE_HEIGHT = 0.85f
    /**
     * Neck slice plane vs horizontal (degrees): rotates plane normal from +Y toward +Z so the neck
     * ring tilts forward (chin side slightly lower). Matches [neckSlicePlaneNormal].
     */
    const val NECK_SLICE_FORWARD_TILT_DEG = 15f
    /**
     * After head geometry is clipped, shift the mesh along +Y in model space so the torso uses the
     * freed vertical space; keeps overlay projection aligned with GL ([projectModelPointToViewPx]).
     */
    const val HEADLESS_BODY_FRAMING_OFFSET_Y = 0.12f
    /** Camera distance multiplier — lower = closer camera / larger figure in the preview. */
    const val HEADLESS_PREVIEW_DISTANCE_SCALE = 0.82f
}

internal fun computeMetricAvatarViewDistance(viewWidth: Int, viewHeight: Int): Float {
    val aspect = viewWidth.toFloat() / viewHeight.toFloat().coerceAtLeast(1f)
    val tanHalfFov = kotlin.math.tan(Math.toRadians(MetricAvatarCamera.FOV_DEG / 2.0)).toFloat()
        .coerceAtLeast(MetricAvatarCamera.MIN_TAN_HALF_FOV)
    val distForHeight = 1.0f / tanHalfFov
    val distForWidth =
        1.0f / (tanHalfFov * aspect.coerceAtLeast(MetricAvatarCamera.MIN_ASPECT_FOR_FRAMING))
    return max(distForHeight, distForWidth) *
        MetricAvatarCamera.VIEW_DISTANCE_SAFETY_MARGIN *
        MetricAvatarCamera.HEADLESS_PREVIEW_DISTANCE_SCALE
}

/**
 * Projects a point in model space to view pixel coordinates (top-left origin, Y down),
 * using the same MVP chain as [MetricAvatarRenderer].
 */
internal fun projectModelPointToViewPx(
    modelX: Float,
    modelY: Float,
    modelZ: Float,
    viewWidth: Int,
    viewHeight: Int,
    yawDeg: Float,
    pitchDeg: Float,
): Offset? {
    if (viewWidth <= 0 || viewHeight <= 0) return null

    val projection = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    val view = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    val model = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    val temp = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    val mvp = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    val vec = floatArrayOf(modelX, modelY, modelZ, 1f)
    val clip = FloatArray(4)

    val aspect = viewWidth.toFloat() / viewHeight.toFloat().coerceAtLeast(1f)
    Matrix.perspectiveM(
        projection,
        0,
        MetricAvatarCamera.FOV_DEG,
        aspect,
        MetricAvatarCamera.FRUSTUM_NEAR,
        MetricAvatarCamera.FRUSTUM_FAR,
    )
    val viewDistance = computeMetricAvatarViewDistance(viewWidth, viewHeight)
    Matrix.setLookAtM(
        view,
        0,
        0f,
        MetricAvatarCamera.EYE_HEIGHT,
        viewDistance,
        0f,
        0f,
        0f,
        0f,
        1f,
        0f,
    )

    Matrix.setIdentityM(model, 0)
    Matrix.rotateM(model, 0, pitchDeg, 1f, 0f, 0f)
    Matrix.rotateM(model, 0, yawDeg, 0f, 1f, 0f)
    Matrix.translateM(model, 0, 0f, MetricAvatarCamera.HEADLESS_BODY_FRAMING_OFFSET_Y, 0f)

    Matrix.multiplyMM(temp, 0, view, 0, model, 0)
    Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0)
    Matrix.multiplyMV(clip, 0, mvp, 0, vec, 0)

    val w = clip[3]
    if (w <= 0f) return null
    val ndcX = clip[0] / w
    val ndcY = clip[1] / w
    if (ndcX !in -1.5f..1.5f || ndcY !in -1.5f..1.5f) return null

    val px = (ndcX + 1f) * 0.5f * viewWidth
    val py = (1f - ndcY) * 0.5f * viewHeight
    return Offset(px, py)
}

internal data class ModelLeaderSegment(
    val ax: Float,
    val ay: Float,
    val az: Float,
    val ex: Float,
    val ey: Float,
    val ez: Float,
)

internal object MeasurementVisualAnchors {

    /** Reference surface points in bbox-normalized model space (front-of-body attachment targets). */
    private val segmentsByKey: Map<String, ModelLeaderSegment> = mapOf(
        /* Neck: below cranium, front midline */
        "neck" to ModelLeaderSegment(0f, 1.06f, 0.13f, 0.42f, 1.22f, 0.18f),
        /* Shoulder line — higher than chest, lateral */
        "shoulders" to ModelLeaderSegment(0.48f, 1.00f, 0.11f, 0.72f, 0.98f, 0.16f),
        /* Mid chest */
        "chest" to ModelLeaderSegment(0f, 0.67f, 0.17f, 0.38f, 0.76f, 0.22f),
        /* Mid forearm — right arm (mirror of former left defaults) */
        "forearm" to ModelLeaderSegment(0.70f, 0.46f, 0.11f, 1.05f, 0.36f, 0.18f),
        /* Upper arm — mid-biceps (above elbow crease; higher Y avoids sliding to elbow in scans) */
        "bicep" to ModelLeaderSegment(-0.58f, 0.63f, 0.11f, -0.88f, 0.56f, 0.18f),
        "upperWaist" to ModelLeaderSegment(0f, 0.38f, 0.15f, 0.38f, 0.38f, 0.20f),
        /* Natural waist — above iliac crest */
        "waist" to ModelLeaderSegment(0f, 0.24f, 0.15f, 0.42f, 0.18f, 0.22f),
        "lowerWaist" to ModelLeaderSegment(0f, 0.07f, 0.14f, 0.38f, 0.04f, 0.20f),
        /* Left leg — slightly raised */
        "thigh" to ModelLeaderSegment(-0.24f, -0.42f, 0.11f, -0.55f, -0.48f, 0.18f),
        /* Calf — right leg */
        "calf" to ModelLeaderSegment(0.20f, -0.95f, 0.10f, 0.52f, -1.02f, 0.16f),
    )

    fun segmentForKey(key: String): ModelLeaderSegment? = segmentsByKey[key]

    fun fallbackAnchorPosition(key: String): FloatArray? =
        segmentsByKey[key]?.let { floatArrayOf(it.ax, it.ay, it.az) }
}

internal data class VisualAvatarTransform(
    val yawDeg: Float,
    val pitchDeg: Float,
    val widthPx: Int,
    val heightPx: Int,
)

/** Keys where the UI draws a **mesh cross-section** slice (horizontal plane), not a single point. */
internal val CircumferenceVisualKeys = setOf(
    "neck",
    "shoulders",
    "chest",
    "forearm",
    "bicep",
    "upperWaist",
    "waist",
    "lowerWaist",
    "thigh",
    "calf",
)

/** Limb circumferences: UI draws **two** rings (left + right); leader picks the nearer ring in screen space. */
internal val BilateralCircumferenceKeys = setOf("bicep", "forearm", "thigh", "calf")

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
    /**
     * Head clip in model space: plane `nx*x + ny*y + nz*z + d = 0`; discard where `dot(p,n)+d > 0`
     * (toward head). Matches the neck slice plane + anchor.
     */
    val neckClipPlane: FloatArray,
)

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
    val anchors = LinkedHashMap<String, FloatArray>()
    for (key in measurementAnchorKeys) {
        anchors[key] = blendAnchorWithFallback(key, rawAnchors[key])
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
        if (packed != null && packed.size >= 9) slices[key] = packed
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
        if (packed != null && packed.size >= 9) oppositeSlices[key] = packed
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
    /*
     * Plane ⟂ upper arm: shoulder-side hint → bicep (not torso centroid→bicep, which tilts through chest/elbow).
     */
    "bicep" -> limbAxis(lateralShoulderTowardLimb(anchors, leftSide = true), anchors[key])
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
    "bicep" -> limbAxis(lateralShoulderTowardLimb(anchors, leftSide = false), oppositeBandAnchor)
    "forearm" -> limbAxis(lateralShoulderTowardLimb(anchors, leftSide = true), oppositeBandAnchor)
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

/**
 * Approximate lateral shoulder on one side so limb slice normals aim **down the arm**, not toward mid-chest.
 */
private fun lateralShoulderTowardLimb(anchors: Map<String, FloatArray>, leftSide: Boolean): FloatArray {
    val sh = anchors["shoulders"] ?: MeasurementVisualAnchors.fallbackAnchorPosition("shoulders")!!
    val lateral = max(abs(sh[0]), 0.34f)
    val x = if (leftSide) -lateral else lateral
    return floatArrayOf(x, sh[1], sh[2])
}

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
    val ref = MeasurementVisualAnchors.fallbackAnchorPosition(key) ?: return computed ?: floatArrayOf(0f, 0f, 0f)
    if (computed == null) return ref.copyOf()
    val yWeight: Float
    val xzWeight: Float
    when (key) {
        /* Stronger pull to canonical limb band height/lateral — reduces centroid drift to elbow/torso */
        "bicep", "forearm" -> {
            yWeight = 0.82f
            xzWeight = 0.52f
        }
        else -> {
            yWeight = 0.68f
            xzWeight = 0.42f
        }
    }
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
    fun area(ids: List<Int>) = polygonAreaUV(ids, coords, planeNormal)
    fun centroidDistSq(ids: List<Int>): Float {
        var sx = 0f
        var sy = 0f
        var sz = 0f
        for (i in ids) {
            val p = coords[i]
            sx += p[0]
            sy += p[1]
            sz += p[2]
        }
        val nf = ids.size.toFloat()
        val dx = sx / nf - anchor[0]
        val dy = sy / nf - anchor[1]
        val dz = sz / nf - anchor[2]
        return dx * dx + dy * dy + dz * dz
    }
    return when (measurementKey) {
        in BoundaryLimbKeys ->
            cycles.minWithOrNull(
                compareBy<List<Int>> { centroidDistSq(it) }.thenByDescending { area(it) },
            )
        else ->
            cycles.maxByOrNull { area(it) }
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
            intersectEdgeWithPlane(vertices[ia], vertices[ib], planePoint, planeNormal)?.let { hits.add(it) }
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
 * Chain plane∩triangle segments into closed loops. **Torso** bands: largest area in the slice plane.
 * **Limbs**: loop whose centroid is closest to the band anchor (then largest area as tie-break).
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
    val best = pickBestBoundaryCycle(cycles, coords, measurementKey, anchor, planeNormal) ?: return null
    if (best.size < MIN_CROSS_SECTION_POINTS) return null
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
    val offsetsAlongNormal =
        floatArrayOf(0f, 0.004f, -0.004f, 0.008f, -0.008f, 0.012f, -0.012f)
    for (d in offsetsAlongNormal) {
        val planePoint = floatArrayOf(
            anchor[0] + planeNormal[0] * d,
            anchor[1] + planeNormal[1] * d,
            anchor[2] + planeNormal[2] * d,
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
                anchor,
            )?.let { boundary ->
                if (boundary.size >= MIN_CROSS_SECTION_POINTS) {
                    return packXYZ(boundary)
                }
            }
        }
        val raw = collectSlicePoints(vertices, usedVertices, faces, planePoint, planeNormal)
        var filtered = filterSlicePointsForMeasurement(raw, measurementKey, anchor, flipLateralFilter = flipLateralFilter)
        if (filtered.size < MIN_CROSS_SECTION_POINTS) {
            filtered = filterSlicePointsForMeasurement(
                raw,
                measurementKey,
                anchor,
                relaxed = true,
                flipLateralFilter = flipLateralFilter,
            )
        }
        if (filtered.size < MIN_CROSS_SECTION_POINTS / 2) {
            filtered = raw
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
private fun filterTorsoMedianRadiusAboutMidline(points: List<FloatArray>, factor: Float): List<FloatArray> {
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
    val cx = sx / n; val cy = sy / n; val cz = sz / n
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

/** Projects packed model polyline to overlay space; skips points behind camera. */
internal fun projectPackedPolylineToOverlay(
    packed: FloatArray,
    transform: VisualAvatarTransform,
    scaleX: Float,
    scaleY: Float,
): List<Offset> {
    val out = ArrayList<Offset>(packed.size / 3)
    var i = 0
    while (i + 2 < packed.size) {
        val p = projectModelPointToViewPx(
            packed[i],
            packed[i + 1],
            packed[i + 2],
            transform.widthPx,
            transform.heightPx,
            transform.yawDeg,
            transform.pitchDeg,
        )
        if (p != null) out.add(Offset(p.x * scaleX, p.y * scaleY))
        i += 3
    }
    return out
}

internal fun distSq(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy
}

internal fun closestPointOnSegment(a: Offset, b: Offset, p: Offset): Offset {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val apx = p.x - a.x
    val apy = p.y - a.y
    val ab2 = abx * abx + aby * aby
    val t = if (ab2 <= 1e-12f) 0f else ((apx * abx + apy * aby) / ab2).coerceIn(0f, 1f)
    return Offset(a.x + abx * t, a.y + aby * t)
}

internal fun closestPointOnClosedPolyline(poly: List<Offset>, p: Offset): Offset {
    if (poly.isEmpty()) return p
    var best = poly[0]
    var bestD = distSq(p, best)
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        val q = closestPointOnSegment(a, b, p)
        val d = distSq(p, q)
        if (d < bestD) {
            bestD = d
            best = q
        }
    }
    return best
}

/**
 * Where the leader meets the slice: the **closest point** on the closed contour (screen space) to the
 * pill anchor — shortest straight segment from label to the drawn ring. Uses edge perpendiculars,
 * not a fixed mesh vertex.
 */
internal fun leaderAttachPointForCircumferenceSlice(
    poly: List<Offset>,
    pillStart: Offset,
): Offset = closestPointOnClosedPolyline(poly, pillStart)

/** Leader meets the closer of several limb rings (screen space), e.g. left vs right biceps while rotating. */
internal fun leaderAttachPointForCircumferenceSlices(
    polylines: List<List<Offset>>,
    pillStart: Offset,
): Offset {
    var best = pillStart
    var bestD = Float.MAX_VALUE
    for (poly in polylines) {
        if (poly.size < 4) continue
        val q = closestPointOnClosedPolyline(poly, pillStart)
        val d = distSq(pillStart, q)
        if (d < bestD) {
            bestD = d
            best = q
        }
    }
    return best
}
