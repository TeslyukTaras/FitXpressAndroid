package com.hexis.bi.ui.main.scan.results

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.CopyOnWriteArrayList
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * RGB stops for [MetricAvatarPreviewBackgroundBrush] / GL preview background — keep in sync with
 * [MetricAvatarPreviewBackgroundColors].
 */
private const val PREVIEW_GRADIENT_INNER_R = 254f / 255f
private const val PREVIEW_GRADIENT_INNER_G = 254f / 255f
private const val PREVIEW_GRADIENT_INNER_B = 254f / 255f
private const val PREVIEW_GRADIENT_OUTER_R = 195f / 255f
private const val PREVIEW_GRADIENT_OUTER_G = 192f / 255f
private const val PREVIEW_GRADIENT_OUTER_B = 197f / 255f

private val MetricAvatarPreviewBackgroundColors =
    listOf(Color(0xFFFEFEFE), Color(0xFFC3C0C5))

/** Single brush instance so the preview background is identical in every state. */
internal val MetricAvatarPreviewBackgroundBrush: Brush =
    Brush.radialGradient(
        colors = MetricAvatarPreviewBackgroundColors,
        center = Offset.Unspecified,
        radius = Float.POSITIVE_INFINITY,
    )

internal fun Modifier.metricAvatarPreviewGradientBackground(): Modifier =
    background(MetricAvatarPreviewBackgroundBrush)

/** Default starting orientation for the rotatable mesh, in degrees. */
private const val INITIAL_PITCH_DEG = -12f
private const val MIN_PITCH_DEG = -55f
private const val MAX_PITCH_DEG = 35f

/** Shared yaw/pitch for Compare tab so both models rotate together. */
internal class CompareRotationLink {
    @Volatile
    var yaw: Float = 0f

    @Volatile
    var pitch: Float = INITIAL_PITCH_DEG

    private val invalidateCallbacks = CopyOnWriteArrayList<() -> Unit>()

    fun addInvalidateCallback(callback: () -> Unit) {
        invalidateCallbacks.add(callback)
    }

    fun removeInvalidateCallback(callback: () -> Unit) {
        invalidateCallbacks.remove(callback)
    }

    fun applyRotationDelta(dyaw: Float, dpitch: Float) {
        synchronized(this) {
            yaw += dyaw
            pitch = (pitch + dpitch).coerceIn(MIN_PITCH_DEG, MAX_PITCH_DEG)
        }
        invalidateCallbacks.forEach { it() }
    }

    fun replaceOrientation(yawDeg: Float, pitchDeg: Float) {
        synchronized(this) {
            yaw = yawDeg
            pitch = pitchDeg.coerceIn(MIN_PITCH_DEG, MAX_PITCH_DEG)
        }
        invalidateCallbacks.forEach { it() }
    }
}

/** Initial yaw (degrees) for a side/profile view of the mesh on the Posture tab. */
internal const val MetricAvatarSideProfileYawDegrees = 90f

@Composable
internal fun MetricAvatarPreview(
    modelUrl: String,
    onInteractionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showSkinAreas: Boolean = false,
    /** When false, the parent already drew [metricAvatarPreviewGradientBackground]. */
    useGradientBackground: Boolean = true,
    initialYawDegrees: Float = 0f,
    initialPitchDegrees: Float = INITIAL_PITCH_DEG,
    /** When set (Compare tab), both previews share yaw/pitch via this link. */
    compareRotationLink: CompareRotationLink? = null,
    /** Invoked on the main thread when yaw/pitch or viewport size change (Visual overlay). Ignored when [compareRotationLink] is non-null. */
    onVisualTransformChanged: ((yawDeg: Float, pitchDeg: Float, viewWidthPx: Int, viewHeightPx: Int) -> Unit)? = null,
    /** 3D leader segments (depth-tested in GL). Null or empty clears. */
    leaderSegments: List<ModelLeaderSegment>? = null,
    /** Invoked on the main thread after OBJ parse (anchors + slice polylines for circumference labels). */
    onMeasurementGuideLoaded: ((MetricAvatarMeasurementGuide) -> Unit)? = null,
    /** Invoked on the main thread when the mesh is loaded and the GL surface is visible (opaque). */
    onAvatarReady: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val latestOnInteraction by rememberUpdatedState(onInteractionChanged)
    val latestTransform by rememberUpdatedState(onVisualTransformChanged)
    val latestAnchors by rememberUpdatedState(onMeasurementGuideLoaded)
    val latestOnAvatarReady by rememberUpdatedState(onAvatarReady)
    val view = remember(context) { MetricAvatarSurfaceView(context) { latestOnInteraction(it) } }
    var loadState by remember { mutableStateOf(MetricAvatarLoadState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(view, showSkinAreas) { view.setShowSkinAreas(showSkinAreas) }

    DisposableEffect(view) {
        view.onResume()
        onDispose { view.onPause() }
    }

    LaunchedEffect(view, leaderSegments, loadState, compareRotationLink) {
        when {
            loadState != MetricAvatarLoadState.Ready -> view.setLeaderSegments(null)
            compareRotationLink != null -> view.setLeaderSegments(null)
            else -> view.setLeaderSegments(leaderSegments)
        }
    }

    DisposableEffect(view, latestTransform) {
        view.setVisualTransformListener(latestTransform)
        onDispose { view.setVisualTransformListener(null) }
    }

    DisposableEffect(view, compareRotationLink) {
        view.setCompareRotationLink(compareRotationLink)
        onDispose { view.setCompareRotationLink(null) }
    }

    LaunchedEffect(loadState, modelUrl, reloadKey) {
        if (loadState == MetricAvatarLoadState.Ready) {
            latestOnAvatarReady?.invoke()
        }
    }

    val latestYawAtLoad by rememberUpdatedState(initialYawDegrees)
    val latestPitchAtLoad by rememberUpdatedState(initialPitchDegrees)

    /** OBJ fetch/parse only when URL or compare mode changes — not when base yaw/pitch change (Visual ↔ Posture). */
    LaunchedEffect(modelUrl, reloadKey, compareRotationLink) {
        compareRotationLink?.let { view.setCompareRotationLink(it) }
        loadState = MetricAvatarLoadState.Loading
        runCatching { withContext(Dispatchers.IO) { ObjParser.parse(modelUrl) } }
            .onSuccess { mesh ->
                if (compareRotationLink != null) {
                    view.applyLoadedMeshForCompare(mesh)
                } else {
                    view.applyLoadedMesh(mesh, latestYawAtLoad, latestPitchAtLoad)
                }
                latestAnchors?.invoke(mesh.measurementGuide)
                loadState = MetricAvatarLoadState.Ready
            }
            .onFailure { e ->
                Timber.w(e, "Metric avatar OBJ load failed url=%s", modelUrl)
                loadState = MetricAvatarLoadState.Error
            }
    }

    LaunchedEffect(initialYawDegrees, initialPitchDegrees, loadState, compareRotationLink) {
        if (loadState != MetricAvatarLoadState.Ready || compareRotationLink != null) return@LaunchedEffect
        view.setBaseOrientation(initialYawDegrees, initialPitchDegrees)
    }

    val containerModifier = if (useGradientBackground) {
        modifier.metricAvatarPreviewGradientBackground()
    } else {
        modifier
    }
    Box(modifier = containerModifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .alpha(if (loadState == MetricAvatarLoadState.Ready) 1f else 0f),
            factory = { view },
        )
        when (loadState) {
            MetricAvatarLoadState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
                        Text(
                            text = stringResource(R.string.scan_results_avatar_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            MetricAvatarLoadState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AppButton(
                        text = stringResource(R.string.action_retry),
                        onClick = { reloadKey++ })
                }
            }

            MetricAvatarLoadState.Ready -> { /* GL surface visible */
            }
        }
    }
}

private enum class MetricAvatarLoadState {
    Loading,
    Ready,
    Error,
}

private const val TOUCH_YAW_SENSITIVITY = 0.25f
private const val TOUCH_PITCH_SENSITIVITY = 0.2f

private const val EGL_CONTEXT_CLIENT_VERSION = 2
private const val EGL_COLOR_CHANNEL_BITS = 8
private const val EGL_DEPTH_BITS = 16
private const val EGL_STENCIL_BITS = 0

private class MetricAvatarSurfaceView : GLSurfaceView {
    private val avatarRenderer = MetricAvatarRenderer()
    private var onInteractionChanged: (Boolean) -> Unit = {}
    private var lastX = 0f
    private var lastY = 0f
    private var boundCompareLink: CompareRotationLink? = null
    private val compareRenderNotify: () -> Unit = { requestRender() }

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, onInteractionChanged: (Boolean) -> Unit) : super(context) {
        this.onInteractionChanged = onInteractionChanged
        initView()
    }

    private fun initView() {
        setEGLContextClientVersion(EGL_CONTEXT_CLIENT_VERSION)
        setEGLConfigChooser(
            EGL_COLOR_CHANNEL_BITS,
            EGL_COLOR_CHANNEL_BITS,
            EGL_COLOR_CHANNEL_BITS,
            EGL_COLOR_CHANNEL_BITS,
            EGL_DEPTH_BITS,
            EGL_STENCIL_BITS,
        )
        holder.setFormat(PixelFormat.TRANSLUCENT)
        /**
         * Keep default z-order (**not** [setZOrderOnTop]) so this view participates in normal
         * window compositing: scrolled content and widgets drawn after this view can appear on top,
         * and the surface tears down with the screen instead of lingering above during transitions.
         * Translucent EGL + [GLES20.glClearColor] alpha 0 preserves the gradient behind the mesh.
         */
        setRenderer(avatarRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun applyLoadedMesh(mesh: ObjMesh, yawDeg: Float, pitchDeg: Float) {
        queueEvent {
            avatarRenderer.setRotationLink(null)
            avatarRenderer.setBaseOrientation(yawDeg, pitchDeg)
            avatarRenderer.setMesh(mesh)
            requestRender()
        }
    }

    fun applyLoadedMeshForCompare(mesh: ObjMesh) {
        queueEvent {
            avatarRenderer.setMesh(mesh)
            requestRender()
        }
    }

    fun setCompareRotationLink(link: CompareRotationLink?) {
        boundCompareLink?.removeInvalidateCallback(compareRenderNotify)
        boundCompareLink = link
        avatarRenderer.setRotationLink(link)
        link?.addInvalidateCallback(compareRenderNotify)
    }

    fun setShowSkinAreas(show: Boolean) {
        queueEvent { avatarRenderer.setShowSkinAreas(show) }
        requestRender()
    }

    fun setVisualTransformListener(listener: ((Float, Float, Int, Int) -> Unit)?) {
        avatarRenderer.setVisualTransformListener(listener)
    }

    fun setLeaderSegments(segments: List<ModelLeaderSegment>?) {
        queueEvent {
            avatarRenderer.setLeaderSegments(segments)
            requestRender()
        }
    }

    fun setBaseOrientation(yawDeg: Float, pitchDeg: Float) {
        queueEvent {
            avatarRenderer.setBaseOrientation(yawDeg, pitchDeg)
            requestRender()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onInteractionChanged(true)
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                val link = boundCompareLink
                if (link != null) {
                    link.applyRotationDelta(
                        dx * TOUCH_YAW_SENSITIVITY,
                        dy * TOUCH_PITCH_SENSITIVITY,
                    )
                } else {
                    queueEvent {
                        avatarRenderer.rotateBy(
                            dyaw = dx * TOUCH_YAW_SENSITIVITY,
                            dpitch = dy * TOUCH_PITCH_SENSITIVITY,
                        )
                    }
                    requestRender()
                }
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_UP -> {
                performClick()
                onInteractionChanged(false)
            }

            MotionEvent.ACTION_CANCEL -> onInteractionChanged(false)
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

internal data class ObjMesh(
    val vertexBuffer: FloatBuffer,
    val vertexCount: Int,
    /** Anchors + mesh cross-sections from OBJ (see [buildMeasurementGuide]). */
    val measurementGuide: MetricAvatarMeasurementGuide,
    /** Solid neck cap (position-only buffer); fills the opening where the head was clipped. */
    val neckCapVertexBuffer: FloatBuffer? = null,
    val neckCapVertexCount: Int = 0,
)

private fun buildNeckCapMeshBuffer(
    packedNeck: FloatArray?,
    planeNormal: FloatArray,
): Pair<FloatBuffer?, Int> {
    if (packedNeck == null || packedNeck.size < 9) return null to 0
    val pts = ArrayList<FloatArray>(packedNeck.size / 3)
    var i = 0
    while (i + 2 < packedNeck.size) {
        pts.add(floatArrayOf(packedNeck[i], packedNeck[i + 1], packedNeck[i + 2]))
        i += 3
    }
    if (pts.size < 3) return null to 0
    val nx = planeNormal[0]
    val ny = planeNormal[1]
    val nz = planeNormal[2]
    val inset = 0.005f
    fun off(p: FloatArray) = floatArrayOf(
        p[0] - nx * inset,
        p[1] - ny * inset,
        p[2] - nz * inset,
    )
    var cx = 0f
    var cy = 0f
    var cz = 0f
    for (p in pts) {
        cx += p[0]
        cy += p[1]
        cz += p[2]
    }
    val inv = 1f / pts.size
    cx *= inv
    cy *= inv
    cz *= inv
    val c = off(floatArrayOf(cx, cy, cz))
    val p0 = off(pts[0])
    val p1 = off(pts[1])
    fun sub(a: FloatArray, b: FloatArray) = floatArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
    fun cross(a: FloatArray, b: FloatArray) = floatArrayOf(
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    )
    val cr = cross(sub(p0, c), sub(p1, c))
    val flip = cr[0] * nx + cr[1] * ny + cr[2] * nz < 0f
    val tri = ArrayList<Float>(pts.size * 9)
    for (k in pts.indices) {
        val k1 = (k + 1) % pts.size
        val va = off(pts[if (flip) k1 else k])
        val vb = off(pts[if (flip) k else k1])
        tri.add(c[0])
        tri.add(c[1])
        tri.add(c[2])
        tri.add(va[0])
        tri.add(va[1])
        tri.add(va[2])
        tri.add(vb[0])
        tri.add(vb[1])
        tri.add(vb[2])
    }
    val fa = tri.toFloatArray()
    val bb = ByteBuffer.allocateDirect(fa.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    bb.put(fa)
    bb.position(0)
    return bb to (fa.size / 3)
}

private object ObjParser {
    private const val NORMALIZED_TARGET_SIZE = 3.0f
    private const val FLOATS_PER_VERTEX = 9
    private const val BYTES_PER_FLOAT = 4
    private const val VERTEX_MERGE_RADIUS = 0.012f

    private const val OBJ_URL_CONNECT_TIMEOUT_MS = 10_000
    private const val OBJ_URL_READ_TIMEOUT_MS = 15_000

    /**
     * Body regions where vertices are not merged aggressively.
     * Head uses the neck clip plane from [MetricAvatarMeasurementGuide.neckClipPlane].
     */
    private const val PROTECTED_REGION_HANDS_ABS_X = 0.36f
    private const val PROTECTED_REGION_HANDS_MIN_Y = -1.05f
    private const val PROTECTED_REGION_HANDS_MAX_Y = 0.05f
    private const val PROTECTED_REGION_FEET_MAX_Y = -1.32f

    private data class Cluster(
        var x: Float,
        var y: Float,
        var z: Float,
        var count: Int,
        var cx: Int,
        var cy: Int,
        var cz: Int
    )

    private data class ClusterResult(val vertices: List<FloatArray>, val remap: IntArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ClusterResult
            return vertices == other.vertices && remap.contentEquals(other.remap)
        }

        override fun hashCode(): Int {
            var result = vertices.hashCode()
            result = 31 * result + remap.contentHashCode()
            return result
        }
    }

    private fun distSq(a: FloatArray, b: FloatArray): Float {
        val dx = a[0] - b[0]
        val dy = a[1] - b[1]
        val dz = a[2] - b[2]
        return dx * dx + dy * dy + dz * dz
    }

    fun parse(url: String): ObjMesh {
        val vertices = ArrayList<FloatArray>()
        val faces = ArrayList<IntArray>()
        val connection = URL(url).openConnection().apply {
            connectTimeout = OBJ_URL_CONNECT_TIMEOUT_MS
            readTimeout = OBJ_URL_READ_TIMEOUT_MS
        }

        BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
            reader.lineSequence().forEach { line ->
                when {
                    line.startsWith("v ") -> parseVertex(line, vertices)
                    line.startsWith("f ") -> parseFaceIndices(line, vertices.size)?.let {
                        faces.add(
                            it
                        )
                    }
                }
            }
        }

        if (faces.isEmpty()) throw IllegalArgumentException("OBJ contains no supported faces")

        val usedVertices = BooleanArray(vertices.size)
        faces.forEach { face -> face.forEach { index -> usedVertices[index] = true } }
        normalizeVertices(vertices, usedVertices)

        val measurementGuide = buildMeasurementGuide(vertices, usedVertices, faces)

        val clusterResult = clusterCloseVertices(vertices, usedVertices, measurementGuide.neckClipPlane)

        val triangles = ArrayList<Float>()
        val emittedTriangles = HashSet<String>()

        faces.forEach { face ->
            appendClusteredFace(
                face,
                clusterResult,
                emittedTriangles,
                triangles
            )
        }
        if (triangles.isEmpty()) throw IllegalArgumentException("OBJ contains no supported triangles")

        val buffer = ByteBuffer.allocateDirect(triangles.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(triangles.toFloatArray())
                position(0)
            }

        val (capBuf, capVerts) = buildNeckCapMeshBuffer(
            measurementGuide.crossSectionPolylines["neck"],
            floatArrayOf(
                measurementGuide.neckClipPlane[0],
                measurementGuide.neckClipPlane[1],
                measurementGuide.neckClipPlane[2],
            ),
        )

        return ObjMesh(
            vertexBuffer = buffer,
            vertexCount = triangles.size / FLOATS_PER_VERTEX,
            measurementGuide = measurementGuide,
            neckCapVertexBuffer = capBuf,
            neckCapVertexCount = capVerts,
        )
    }

    private fun parseVertex(line: String, vertices: MutableList<FloatArray>) {
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 4) return
        vertices.add(
            floatArrayOf(
                parts[1].toFloatOrNull() ?: 0f,
                parts[2].toFloatOrNull() ?: 0f,
                parts[3].toFloatOrNull() ?: 0f,
            )
        )
    }

    private fun parseFaceIndices(line: String, vertexCount: Int): IntArray? {
        val faceParts = line.trim().split(Regex("\\s+")).drop(1)
        if (faceParts.size < 3) return null

        val indices = ArrayList<Int>(faceParts.size)
        for (token in faceParts) {
            val rawIndex = token.substringBefore("/").toIntOrNull() ?: return null
            val index = if (rawIndex < 0) vertexCount + rawIndex else rawIndex - 1
            if (index !in 0 until vertexCount) return null
            indices.add(index)
        }

        return indices.toIntArray()
    }

    private fun appendClusteredFace(
        face: IntArray,
        clusterResult: ClusterResult,
        emittedTriangles: MutableSet<String>,
        triangles: MutableList<Float>,
    ) {
        val remap = clusterResult.remap
        val clusteredVertices = clusterResult.vertices

        for (i in 1 until face.size - 1) {
            val c0 = remap[face[0]]
            val c1 = remap[face[i]]
            val c2 = remap[face[i + 1]]

            if (c0 < 0 || c1 < 0 || c2 < 0) continue
            if (c0 == c1 || c1 == c2 || c2 == c0) continue

            val key = triangleKey(c0, c1, c2)
            if (!emittedTriangles.add(key)) continue

            val v0 = clusteredVertices[c0]
            val v1 = clusteredVertices[c1]
            val v2 = clusteredVertices[c2]

            var mx = 1f
            var my = 1f
            var mz = 1f
            val d01 = distSq(v0, v1)
            val d12 = distSq(v1, v2)
            val d20 = distSq(v2, v0)
            if (d12 >= d20 && d12 >= d01) mx = 0f else if (d20 >= d01) my = 0f else mz = 0f

            appendVertex(triangles, v0, 1f, 0f, 0f, mx, my, mz)
            appendVertex(triangles, v1, 0f, 1f, 0f, mx, my, mz)
            appendVertex(triangles, v2, 0f, 0f, 1f, mx, my, mz)
        }
    }

    private fun clusterCloseVertices(
        vertices: List<FloatArray>,
        usedVertices: BooleanArray,
        neckClipPlane: FloatArray,
    ): ClusterResult {
        val clusters = ArrayList<Cluster>()
        val remap = IntArray(vertices.size) { -1 }
        val grid = HashMap<String, MutableList<Int>>()
        val radiusSq = VERTEX_MERGE_RADIUS * VERTEX_MERGE_RADIUS

        vertices.forEachIndexed { vertexIndex, vertex ->
            if (!usedVertices[vertexIndex]) return@forEachIndexed

            if (isProtectedDetailVertex(vertex, neckClipPlane)) {
                val clusterIndex = clusters.size
                clusters.add(Cluster(vertex[0], vertex[1], vertex[2], 1, 0, 0, 0))
                remap[vertexIndex] = clusterIndex
                return@forEachIndexed
            }

            val cellX = cellOf(vertex[0])
            val cellY = cellOf(vertex[1])
            val cellZ = cellOf(vertex[2])
            var bestCluster = -1
            var bestDistSq = radiusSq

            for (x in cellX - 1..cellX + 1) {
                for (y in cellY - 1..cellY + 1) {
                    for (z in cellZ - 1..cellZ + 1) {
                        grid[gridKey(x, y, z)]?.forEach { clusterIndex ->
                            val cluster = clusters[clusterIndex]
                            val dx = vertex[0] - cluster.x
                            val dy = vertex[1] - cluster.y
                            val dz = vertex[2] - cluster.z
                            val d = dx * dx + dy * dy + dz * dz
                            if (d <= bestDistSq) {
                                bestDistSq = d
                                bestCluster = clusterIndex
                            }
                        }
                    }
                }
            }

            if (bestCluster >= 0) {
                addVertexToCluster(bestCluster, vertex, clusters, grid)
                remap[vertexIndex] = bestCluster
            } else {
                val clusterIndex = clusters.size
                clusters.add(Cluster(vertex[0], vertex[1], vertex[2], 1, cellX, cellY, cellZ))
                grid.getOrPut(gridKey(cellX, cellY, cellZ)) { ArrayList() }.add(clusterIndex)
                remap[vertexIndex] = clusterIndex
            }
        }

        val mergedVertices =
            clusters.map { cluster -> floatArrayOf(cluster.x, cluster.y, cluster.z) }
        return ClusterResult(vertices = mergedVertices, remap = remap)
    }

    private fun isProtectedDetailVertex(vertex: FloatArray, neckClipPlane: FloatArray): Boolean {
        val x = vertex[0]
        val y = vertex[1]
        val towardHead = vertex[0] * neckClipPlane[0] + vertex[1] * neckClipPlane[1] +
            vertex[2] * neckClipPlane[2] + neckClipPlane[3] > 1e-5f
        val hands = kotlin.math.abs(x) >= PROTECTED_REGION_HANDS_ABS_X &&
                y >= PROTECTED_REGION_HANDS_MIN_Y && y <= PROTECTED_REGION_HANDS_MAX_Y
        val feet = y <= PROTECTED_REGION_FEET_MAX_Y
        return towardHead || hands || feet
    }

    private fun addVertexToCluster(
        clusterIndex: Int,
        vertex: FloatArray,
        clusters: MutableList<Cluster>,
        grid: MutableMap<String, MutableList<Int>>,
    ) {
        val cluster = clusters[clusterIndex]
        val oldKey = gridKey(cluster.cx, cluster.cy, cluster.cz)
        val newCount = cluster.count + 1

        cluster.x = (cluster.x * cluster.count + vertex[0]) / newCount
        cluster.y = (cluster.y * cluster.count + vertex[1]) / newCount
        cluster.z = (cluster.z * cluster.count + vertex[2]) / newCount
        cluster.count = newCount

        val newCellX = cellOf(cluster.x)
        val newCellY = cellOf(cluster.y)
        val newCellZ = cellOf(cluster.z)
        val newKey = gridKey(newCellX, newCellY, newCellZ)

        if (newKey != oldKey) {
            grid[oldKey]?.remove(clusterIndex)
            cluster.cx = newCellX
            cluster.cy = newCellY
            cluster.cz = newCellZ
            grid.getOrPut(newKey) { ArrayList() }.add(clusterIndex)
        }
    }

    private fun appendVertex(
        into: MutableList<Float>,
        v: FloatArray,
        b1: Float,
        b2: Float,
        b3: Float,
        m1: Float,
        m2: Float,
        m3: Float,
    ) {
        into.add(v[0]); into.add(v[1]); into.add(v[2])
        into.add(b1); into.add(b2); into.add(b3)
        into.add(m1); into.add(m2); into.add(m3)
    }

    private fun normalizeVertices(vertices: MutableList<FloatArray>, usedVertices: BooleanArray) {
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        vertices.forEachIndexed { index, vertex ->
            if (!usedVertices[index]) return@forEachIndexed
            minX = minOf(minX, vertex[0]); maxX = maxOf(maxX, vertex[0])
            minY = minOf(minY, vertex[1]); maxY = maxOf(maxY, vertex[1])
            minZ = minOf(minZ, vertex[2]); maxZ = maxOf(maxZ, vertex[2])
        }

        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val cz = (minZ + maxZ) / 2f
        val scale = NORMALIZED_TARGET_SIZE / maxOf(maxX - minX, maxY - minY, maxZ - minZ, 1e-6f)

        vertices.forEach { vertex ->
            vertex[0] = (vertex[0] - cx) * scale
            vertex[1] = (vertex[1] - cy) * scale
            vertex[2] = (vertex[2] - cz) * scale
        }
    }

    private fun cellOf(value: Float): Int =
        kotlin.math.floor((value / VERTEX_MERGE_RADIUS).toDouble()).toInt()

    private fun gridKey(x: Int, y: Int, z: Int): String = "$x:$y:$z"

    private fun triangleKey(a: Int, b: Int, c: Int): String {
        val x = minOf(a, b, c)
        val z = maxOf(a, b, c)
        val y = a + b + c - x - z
        return "$x:$y:$z"
    }
}

private class MetricAvatarRenderer : GLSurfaceView.Renderer {
    private var mesh: ObjMesh? = null
    private var program = 0
    private var aPosition = 0
    private var aBary = 0
    private var aEdgeMask = 0
    private var uMvp = 0
    private var uSkinColor = 0
    private var uSuitColor = 0
    private var uShowSkin = 0
    private var uModelView = 0
    private var uMeshColor = 0
    private var uNeckClipPlane = 0
    private var showSkinAreas = false

    private var neckCapProgram = 0
    private var neckCapAPos = 0
    private var neckCapUMvp = 0
    private var neckCapUColor = 0

    private var leaderProgram = 0
    private var leaderAPos = 0
    private var leaderUMvp = 0
    private var leaderUColor = 0
    private var leaderLineBuffer: FloatBuffer? = null
    private var leaderLineVertCount = 0

    private var gradientProgram = 0
    private var gradientAClip = 0
    private var gradientLocResolution = 0
    private var gradientLocInner = 0
    private var gradientLocOuter = 0
    private var fullscreenClipBuffer: FloatBuffer? = null

    private var rotationLink: CompareRotationLink? = null

    private val projection = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    private val view = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    private val model = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    private val temp = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    private val mvp = FloatArray(MetricAvatarCamera.MATRIX_SIZE)

    private var yaw = 0f
    private var pitch = INITIAL_PITCH_DEG
    private var viewDistance = MetricAvatarCamera.INITIAL_VIEW_DISTANCE

    private var viewportWidth = 0
    private var viewportHeight = 0
    private var visualTransformListener: ((Float, Float, Int, Int) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastPostedYaw = Float.NaN
    private var lastPostedPitch = Float.NaN

    fun setVisualTransformListener(listener: ((Float, Float, Int, Int) -> Unit)?) {
        visualTransformListener = listener
        if (listener == null) {
            lastPostedYaw = Float.NaN
            lastPostedPitch = Float.NaN
        }
    }

    private fun scheduleVisualTransformNotify(force: Boolean) {
        val listener = visualTransformListener ?: return
        if (rotationLink != null) return
        if (viewportWidth <= 0 || viewportHeight <= 0) return
        val y = modelYaw()
        val p = modelPitch()
        if (!force &&
            kotlin.math.abs(y - lastPostedYaw) < 0.025f &&
            kotlin.math.abs(p - lastPostedPitch) < 0.025f
        ) {
            return
        }
        lastPostedYaw = y
        lastPostedPitch = p
        val w = viewportWidth
        val h = viewportHeight
        mainHandler.post { listener(y, p, w, h) }
    }

    fun setLeaderSegments(segments: List<ModelLeaderSegment>?) {
        rebuildLeaderBuffers(segments)
    }

    private fun rebuildLeaderBuffers(segments: List<ModelLeaderSegment>?) {
        if (segments.isNullOrEmpty()) {
            leaderLineBuffer = null
            leaderLineVertCount = 0
            return
        }
        val lineBb = ByteBuffer.allocateDirect(segments.size * 6 * BYTES_PER_FLOAT_GL)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (s in segments) {
            lineBb.put(s.ax)
            lineBb.put(s.ay)
            lineBb.put(s.az)
            lineBb.put(s.ex)
            lineBb.put(s.ey)
            lineBb.put(s.ez)
        }
        lineBb.position(0)
        leaderLineBuffer = lineBb
        leaderLineVertCount = segments.size * 2
    }

    private fun drawPreviewBackgroundGradient() {
        val prog = gradientProgram
        val buf = fullscreenClipBuffer
        if (prog == 0 || buf == null) {
            GLES20.glClearColor(
                PREVIEW_GRADIENT_INNER_R,
                PREVIEW_GRADIENT_INNER_G,
                PREVIEW_GRADIENT_INNER_B,
                1f,
            )
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            return
        }
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        GLES20.glUseProgram(prog)
        GLES20.glUniform2f(
            gradientLocResolution,
            viewportWidth.coerceAtLeast(1).toFloat(),
            viewportHeight.coerceAtLeast(1).toFloat(),
        )
        GLES20.glUniform3f(
            gradientLocInner,
            PREVIEW_GRADIENT_INNER_R,
            PREVIEW_GRADIENT_INNER_G,
            PREVIEW_GRADIENT_INNER_B,
        )
        GLES20.glUniform3f(
            gradientLocOuter,
            PREVIEW_GRADIENT_OUTER_R,
            PREVIEW_GRADIENT_OUTER_G,
            PREVIEW_GRADIENT_OUTER_B,
        )

        buf.position(0)
        GLES20.glVertexAttribPointer(gradientAClip, 2, GLES20.GL_FLOAT, false, 0, buf)
        GLES20.glEnableVertexAttribArray(gradientAClip)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        GLES20.glDisableVertexAttribArray(gradientAClip)

        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun drawNeckCap(m: ObjMesh) {
        val capBuf = m.neckCapVertexBuffer ?: return
        val capCount = m.neckCapVertexCount
        if (capCount <= 0 || neckCapProgram == 0 || neckCapAPos < 0) return

        GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL)
        GLES20.glPolygonOffset(-1.5f, -2f)

        GLES20.glUseProgram(neckCapProgram)
        GLES20.glUniformMatrix4fv(neckCapUMvp, 1, false, mvp, 0)
        GLES20.glUniform4f(neckCapUColor, SUIT_R * 0.94f, SUIT_G + 0.04f, SUIT_B + 0.12f, 1f)

        capBuf.position(0)
        GLES20.glVertexAttribPointer(neckCapAPos, 3, GLES20.GL_FLOAT, false, 0, capBuf)
        GLES20.glEnableVertexAttribArray(neckCapAPos)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, capCount)
        GLES20.glDisableVertexAttribArray(neckCapAPos)

        GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL)
    }

    private fun drawLeaderOverlay() {
        val prog = leaderProgram
        if (prog == 0) return
        val lines = leaderLineBuffer ?: return

        GLES20.glUseProgram(prog)
        GLES20.glUniformMatrix4fv(leaderUMvp, 1, false, mvp, 0)
        GLES20.glUniform4f(leaderUColor, 1f, 0.84f, 0.15f, 1f)

        GLES20.glDepthMask(false)

        lines.position(0)
        GLES20.glVertexAttribPointer(leaderAPos, 3, GLES20.GL_FLOAT, false, 0, lines)
        GLES20.glEnableVertexAttribArray(leaderAPos)
        GLES20.glLineWidth(2f)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, leaderLineVertCount)

        GLES20.glDisableVertexAttribArray(leaderAPos)
        GLES20.glDepthMask(true)
    }

    fun setRotationLink(link: CompareRotationLink?) {
        rotationLink = link
    }

    fun setMesh(value: ObjMesh) {
        mesh = value
    }

    fun setBaseOrientation(yawDeg: Float, pitchDeg: Float) {
        val link = rotationLink
        if (link != null) {
            link.replaceOrientation(yawDeg, pitchDeg)
        } else {
            yaw = yawDeg
            pitch = pitchDeg.coerceIn(MIN_PITCH_DEG, MAX_PITCH_DEG)
        }
    }

    fun setShowSkinAreas(show: Boolean) {
        showSkinAreas = show
    }

    fun rotateBy(dyaw: Float, dpitch: Float) {
        val link = rotationLink
        if (link != null) {
            link.applyRotationDelta(dyaw, dpitch)
        } else {
            yaw += dyaw
            pitch = (pitch + dpitch).coerceIn(MIN_PITCH_DEG, MAX_PITCH_DEG)
        }
    }

    private fun modelYaw(): Float {
        val link = rotationLink
        return if (link != null) synchronized(link) { link.yaw } else yaw
    }

    private fun modelPitch(): Float {
        val link = rotationLink
        return if (link != null) synchronized(link) { link.pitch } else pitch
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(
            PREVIEW_GRADIENT_INNER_R,
            PREVIEW_GRADIENT_INNER_G,
            PREVIEW_GRADIENT_INNER_B,
            1f,
        )
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        gradientProgram = createGradientProgram()
        if (gradientProgram != 0) {
            gradientAClip = GLES20.glGetAttribLocation(gradientProgram, "aClip")
            gradientLocResolution = GLES20.glGetUniformLocation(gradientProgram, "uResolution")
            gradientLocInner = GLES20.glGetUniformLocation(gradientProgram, "uInner")
            gradientLocOuter = GLES20.glGetUniformLocation(gradientProgram, "uOuter")
        }
        val clipBb = ByteBuffer.allocateDirect(6 * BYTES_PER_FLOAT_GL)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        clipBb.put(
            floatArrayOf(
                -1f, -1f,
                3f, -1f,
                -1f, 3f,
            ),
        )
        clipBb.position(0)
        fullscreenClipBuffer = clipBb

        program = createProgram()
        if (program == 0) return

        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aBary = GLES20.glGetAttribLocation(program, "aBary")
        aEdgeMask = GLES20.glGetAttribLocation(program, "aEdgeMask")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
        uSkinColor = GLES20.glGetUniformLocation(program, "uSkinColor")
        uSuitColor = GLES20.glGetUniformLocation(program, "uSuitColor")
        uShowSkin = GLES20.glGetUniformLocation(program, "uShowSkin")
        uModelView = GLES20.glGetUniformLocation(program, "uModelView")
        uMeshColor = GLES20.glGetUniformLocation(program, "uMeshColor")
        uNeckClipPlane = GLES20.glGetUniformLocation(program, "uNeckClipPlane")

        neckCapProgram = createNeckCapProgram()
        if (neckCapProgram != 0) {
            neckCapAPos = GLES20.glGetAttribLocation(neckCapProgram, "aPos")
            neckCapUMvp = GLES20.glGetUniformLocation(neckCapProgram, "uMvp")
            neckCapUColor = GLES20.glGetUniformLocation(neckCapProgram, "uColor")
        }

        leaderProgram = createLeaderProgram()
        if (leaderProgram != 0) {
            leaderAPos = GLES20.glGetAttribLocation(leaderProgram, "aPos")
            leaderUMvp = GLES20.glGetUniformLocation(leaderProgram, "uMvp")
            leaderUColor = GLES20.glGetUniformLocation(leaderProgram, "uColor")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportWidth = width
        viewportHeight = height
        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(
            projection,
            0,
            MetricAvatarCamera.FOV_DEG,
            aspect,
            MetricAvatarCamera.FRUSTUM_NEAR,
            MetricAvatarCamera.FRUSTUM_FAR,
        )
        viewDistance = computeMetricAvatarViewDistance(width, height)
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
        scheduleVisualTransformNotify(force = true)
    }

    override fun onDrawFrame(gl: GL10?) {
        drawPreviewBackgroundGradient()
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

        val m = mesh ?: return
        if (program == 0) return

        GLES20.glUseProgram(program)

        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, modelPitch(), 1f, 0f, 0f)
        Matrix.rotateM(model, 0, modelYaw(), 0f, 1f, 0f)
        Matrix.translateM(model, 0, 0f, MetricAvatarCamera.HEADLESS_BODY_FRAMING_OFFSET_Y, 0f)

        Matrix.multiplyMM(temp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0)

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModelView, 1, false, temp, 0)
        GLES20.glUniform4f(uSkinColor, SKIN_R, SKIN_G, SKIN_B, 1f)
        GLES20.glUniform4f(uSuitColor, SUIT_R, SUIT_G, SUIT_B, 1f)
        GLES20.glUniform4f(uMeshColor, MESH_R, MESH_G, MESH_B, 1f)
        GLES20.glUniform1f(uShowSkin, if (showSkinAreas) 1f else 0f)
        GLES20.glUniform4fv(uNeckClipPlane, 1, m.measurementGuide.neckClipPlane, 0)

        val buf = m.vertexBuffer
        buf.position(0)
        GLES20.glVertexAttribPointer(
            aPosition, FLOATS_PER_ATTRIB, GLES20.GL_FLOAT, false, VERTEX_STRIDE_BYTES, buf,
        )
        GLES20.glEnableVertexAttribArray(aPosition)

        if (aBary >= 0) {
            buf.position(BARY_FLOAT_OFFSET)
            GLES20.glVertexAttribPointer(
                aBary, FLOATS_PER_ATTRIB, GLES20.GL_FLOAT, false, VERTEX_STRIDE_BYTES, buf,
            )
            GLES20.glEnableVertexAttribArray(aBary)
        }

        if (aEdgeMask >= 0) {
            buf.position(EDGE_MASK_FLOAT_OFFSET)
            GLES20.glVertexAttribPointer(
                aEdgeMask, FLOATS_PER_ATTRIB, GLES20.GL_FLOAT, false, VERTEX_STRIDE_BYTES, buf,
            )
            GLES20.glEnableVertexAttribArray(aEdgeMask)
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, m.vertexCount)
        GLES20.glDisableVertexAttribArray(aPosition)
        if (aBary >= 0) GLES20.glDisableVertexAttribArray(aBary)
        if (aEdgeMask >= 0) GLES20.glDisableVertexAttribArray(aEdgeMask)

        drawNeckCap(m)

        drawLeaderOverlay()

        scheduleVisualTransformNotify(force = false)
    }

    private fun createProgram(): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        if (vs == 0 || fs == 0) return 0

        val programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vs)
        GLES20.glAttachShader(programId, fs)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Timber.e("GL Link Error: %s", GLES20.glGetProgramInfoLog(programId))
            return 0
        }
        return programId
    }

    private fun createGradientProgram(): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, GRADIENT_VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, GRADIENT_FRAGMENT_SHADER)
        if (vs == 0 || fs == 0) return 0

        val programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vs)
        GLES20.glAttachShader(programId, fs)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Timber.e("GL Gradient Link Error: %s", GLES20.glGetProgramInfoLog(programId))
            return 0
        }
        return programId
    }

    private fun createLeaderProgram(): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, LEADER_VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, LEADER_FRAGMENT_SHADER)
        if (vs == 0 || fs == 0) return 0

        val programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vs)
        GLES20.glAttachShader(programId, fs)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Timber.e("GL Leader Link Error: %s", GLES20.glGetProgramInfoLog(programId))
            return 0
        }
        return programId
    }

    private fun createNeckCapProgram(): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, NECK_CAP_VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, NECK_CAP_FRAGMENT_SHADER)
        if (vs == 0 || fs == 0) return 0

        val programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vs)
        GLES20.glAttachShader(programId, fs)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Timber.e("GL Neck Cap Link Error: %s", GLES20.glGetProgramInfoLog(programId))
            return 0
        }
        return programId
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Timber.e("Shader Compile Error: %s", GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private companion object {
        private const val SKIN_R = 0.84f
        private const val SKIN_G = 0.68f
        private const val SKIN_B = 0.58f
        private const val SUIT_R = 0f
        private const val SUIT_G = 0.02f
        private const val SUIT_B = 0.06f
        private const val MESH_R = 0f
        private const val MESH_G = 0.188f
        private const val MESH_B = 1f

        /** XYZ components per vertex attribute in the packed interleaved buffer. */
        private const val FLOATS_PER_ATTRIB = 3

        /** Stride in bytes: position(3) + bary(3) + edge mask(3) floats × 4 bytes. */
        private const val FLOATS_PER_INTERLEAVED_VERTEX = 9
        private const val BYTES_PER_FLOAT_GL = 4
        private const val VERTEX_STRIDE_BYTES = FLOATS_PER_INTERLEAVED_VERTEX * BYTES_PER_FLOAT_GL

        private const val BARY_FLOAT_OFFSET = FLOATS_PER_ATTRIB
        private const val EDGE_MASK_FLOAT_OFFSET = FLOATS_PER_ATTRIB * 2

        /** Radial gradient matching [MetricAvatarPreviewBackgroundBrush] (center → edge). */
        private const val GRADIENT_VERTEX_SHADER = """
            attribute vec2 aClip;
            void main() {
                gl_Position = vec4(aClip, 0.0, 1.0);
            }
        """

        private const val GRADIENT_FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec2 uResolution;
            uniform vec3 uInner;
            uniform vec3 uOuter;
            void main() {
                vec2 uv = vec2(
                    gl_FragCoord.x / uResolution.x,
                    1.0 - gl_FragCoord.y / uResolution.y
                );
                vec2 c = vec2(0.5, 0.5);
                float d = distance(uv, c) * 1.41421356;
                float t = clamp(d, 0.0, 1.0);
                vec3 col = mix(uInner, uOuter, t);
                gl_FragColor = vec4(col, 1.0);
            }
        """

        private const val LEADER_VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """

        private const val LEADER_FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """

        private const val NECK_CAP_VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """

        private const val NECK_CAP_FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """

        const val VERTEX_SHADER = """
            attribute vec3 aPosition; attribute vec3 aBary; attribute vec3 aEdgeMask;
            uniform mat4 uMvp; varying float vModelY; varying vec3 vModelPos; 
            varying vec3 vBary; varying vec3 vEdgeMask;
            void main() {
                vModelY = aPosition.y; vModelPos = aPosition; vBary = aBary; vEdgeMask = aEdgeMask;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_standard_derivatives : enable
            precision mediump float;
            varying float vModelY; varying vec3 vModelPos; varying vec3 vBary; varying vec3 vEdgeMask;
            uniform vec4 uSkinColor; uniform vec4 uSuitColor; uniform vec4 uMeshColor; 
            uniform float uShowSkin; uniform mat4 uModelView;
            uniform vec4 uNeckClipPlane;
            
            void main() {
                if (dot(vModelPos, uNeckClipPlane.xyz) + uNeckClipPlane.w > 0.001) discard;
                float hands = step(0.36, abs(vModelPos.x)) * step(-1.05, vModelPos.y) * step(vModelPos.y, 0.05);
                float feet = step(vModelPos.y, -1.32);
                float skinMask = min(1.0, hands + feet) * uShowSkin;

                vec3 fdx = dFdx(vModelPos); vec3 fdy = dFdy(vModelPos);
                vec3 nView = normalize((uModelView * vec4(normalize(cross(fdx, fdy)), 0.0)).xyz);
                
                vec3 lightDir = normalize(vec3(0.0, 0.6, 0.8));
                float brightness = smoothstep(-0.15, 0.55, dot(nView, lightDir));
                float falloff = pow(brightness, 1.8); 

                vec3 body = mix(vec3(0.0, 0.004, 0.015), uSuitColor.rgb + vec3(0.0, 0.06, 0.18), falloff);

                vec3 baryWidth = fwidth(vBary);
                vec3 baryAA = smoothstep(vec3(0.0), baryWidth * 1.1, vBary);
                vec3 edgeVec = mix(vec3(1.0), baryAA, vEdgeMask);
                float edge = 1.0 - min(min(edgeVec.x, edgeVec.y), edgeVec.z);

                vec3 halfDir = normalize(lightDir + vec3(0.0, 0.0, 1.0));
                float spec = pow(max(dot(nView, halfDir), 0.0), 64.0);
                vec3 wireCol = uMeshColor.rgb + (vec3(0.7, 0.9, 1.0) * spec * 1.8);
                
                vec3 finalBodyCol = mix(body, wireCol, edge * (0.05 + falloff * 0.85));

                float maxBary = max(max(vBary.x, vBary.y), vBary.z);
                float pointMask = smoothstep(0.88, 0.95, maxBary);
                vec3 pointColor = uMeshColor.rgb + vec3(0.4, 0.6, 1.0);
                finalBodyCol = mix(finalBodyCol, pointColor, pointMask * (0.2 + falloff * 0.3));

                float rim = pow(1.0 - max(nView.z, 0.0), 8.0);
                vec3 res = finalBodyCol + uMeshColor.rgb * rim * 0.5;
                gl_FragColor = vec4(mix(res, uSkinColor.rgb, skinMask), 1.0);
            }
        """
    }
}