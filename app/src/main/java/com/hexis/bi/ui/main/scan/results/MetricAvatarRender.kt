package com.hexis.bi.ui.main.scan.results

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
) {
    val context = LocalContext.current
    val latestOnInteraction by rememberUpdatedState(onInteractionChanged)
    val view = remember(context) { MetricAvatarSurfaceView(context) { latestOnInteraction(it) } }
    var loadState by remember { mutableStateOf(MetricAvatarLoadState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(view, showSkinAreas) { view.setShowSkinAreas(showSkinAreas) }

    DisposableEffect(view, compareRotationLink) {
        view.setCompareRotationLink(compareRotationLink)
        onDispose { view.setCompareRotationLink(null) }
    }

    LaunchedEffect(
        modelUrl,
        reloadKey,
        initialYawDegrees,
        initialPitchDegrees,
        compareRotationLink
    ) {
        compareRotationLink?.let { view.setCompareRotationLink(it) }
        loadState = MetricAvatarLoadState.Loading
        runCatching { withContext(Dispatchers.IO) { ObjParser.parse(modelUrl) } }
            .onSuccess { mesh ->
                if (compareRotationLink != null) {
                    view.applyLoadedMeshForCompare(mesh)
                } else {
                    view.applyLoadedMesh(mesh, initialYawDegrees, initialPitchDegrees)
                }
                loadState = MetricAvatarLoadState.Ready
            }
            .onFailure { e ->
                Timber.w(e, "Metric avatar OBJ load failed url=%s", modelUrl)
                loadState = MetricAvatarLoadState.Error
            }
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
        setZOrderOnTop(true)
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

internal data class ObjMesh(val vertexBuffer: FloatBuffer, val vertexCount: Int)

private object ObjParser {
    private const val NORMALIZED_TARGET_SIZE = 3.0f
    private const val FLOATS_PER_VERTEX = 9
    private const val BYTES_PER_FLOAT = 4
    private const val VERTEX_MERGE_RADIUS = 0.012f

    private const val OBJ_URL_CONNECT_TIMEOUT_MS = 10_000
    private const val OBJ_URL_READ_TIMEOUT_MS = 15_000

    /**
     * Body regions where vertices are not merged aggressively.
     * Values mirror the skin-mask thresholds in [MetricAvatarRenderer]'s fragment shader.
     */
    private const val PROTECTED_REGION_HEAD_MIN_Y = 1.08f
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

        val clusterResult = clusterCloseVertices(vertices, usedVertices)
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

        return ObjMesh(vertexBuffer = buffer, vertexCount = triangles.size / FLOATS_PER_VERTEX)
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
        usedVertices: BooleanArray
    ): ClusterResult {
        val clusters = ArrayList<Cluster>()
        val remap = IntArray(vertices.size) { -1 }
        val grid = HashMap<String, MutableList<Int>>()
        val radiusSq = VERTEX_MERGE_RADIUS * VERTEX_MERGE_RADIUS

        vertices.forEachIndexed { vertexIndex, vertex ->
            if (!usedVertices[vertexIndex]) return@forEachIndexed

            if (isProtectedDetailVertex(vertex)) {
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

    private fun isProtectedDetailVertex(vertex: FloatArray): Boolean {
        val x = vertex[0]
        val y = vertex[1]
        val head = y >= PROTECTED_REGION_HEAD_MIN_Y
        val hands = kotlin.math.abs(x) >= PROTECTED_REGION_HANDS_ABS_X &&
            y >= PROTECTED_REGION_HANDS_MIN_Y && y <= PROTECTED_REGION_HANDS_MAX_Y
        val feet = y <= PROTECTED_REGION_FEET_MAX_Y
        return head || hands || feet
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
    private var showSkinAreas = false

    private var rotationLink: CompareRotationLink? = null

    private val projection = FloatArray(MATRIX_SIZE)
    private val view = FloatArray(MATRIX_SIZE)
    private val model = FloatArray(MATRIX_SIZE)
    private val temp = FloatArray(MATRIX_SIZE)
    private val mvp = FloatArray(MATRIX_SIZE)

    private var yaw = 0f
    private var pitch = INITIAL_PITCH_DEG
    private var viewDistance = INITIAL_VIEW_DISTANCE

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
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
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
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projection, 0, FOV_DEG, aspect, FRUSTUM_NEAR, FRUSTUM_FAR)

        val tanHalfFov = kotlin.math.tan(Math.toRadians(FOV_DEG / 2.0)).toFloat()
            .coerceAtLeast(MIN_TAN_HALF_FOV)
        val distForHeight = 1.0f / tanHalfFov
        val distForWidth = 1.0f / (tanHalfFov * aspect.coerceAtLeast(MIN_ASPECT_FOR_FRAMING))
        viewDistance = maxOf(distForHeight, distForWidth) * VIEW_DISTANCE_SAFETY_MARGIN

        Matrix.setLookAtM(view, 0, 0f, EYE_HEIGHT, viewDistance, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val m = mesh ?: return
        if (program == 0) return

        GLES20.glUseProgram(program)

        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, modelPitch(), 1f, 0f, 0f)
        Matrix.rotateM(model, 0, modelYaw(), 0f, 1f, 0f)

        Matrix.multiplyMM(temp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0)

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModelView, 1, false, temp, 0)
        GLES20.glUniform4f(uSkinColor, SKIN_R, SKIN_G, SKIN_B, 1f)
        GLES20.glUniform4f(uSuitColor, SUIT_R, SUIT_G, SUIT_B, 1f)
        GLES20.glUniform4f(uMeshColor, MESH_R, MESH_G, MESH_B, 1f)
        GLES20.glUniform1f(uShowSkin, if (showSkinAreas) 1f else 0f)

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
        private const val MATRIX_SIZE = 16

        /** Default camera distance before [onSurfaceChanged] recomputes framing. */
        private const val INITIAL_VIEW_DISTANCE = 3.2f

        private const val FOV_DEG = 42f
        private const val FRUSTUM_NEAR = 0.1f
        private const val FRUSTUM_FAR = 100f
        private const val MIN_TAN_HALF_FOV = 0.001f
        private const val MIN_ASPECT_FOR_FRAMING = 0.6f
        private const val VIEW_DISTANCE_SAFETY_MARGIN = 1.25f
        private const val EYE_HEIGHT = 0.85f

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

        const val VERTEX_SHADER = """
            attribute vec3 aPosition; attribute vec3 aBary; attribute vec3 aEdgeMask;
            uniform mat4 uMvp; varying float vModelY; varying vec3 vModelPos; 
            varying vec3 vBary; varying vec3 vEdgeMask;
            void main() {
                vModelY = aPosition.y; vModelPos = aPosition; vBary = aBary; vEdgeMask = aEdgeMask;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        const val FRAGMENT_SHADER = """
            #extension GL_OES_standard_derivatives : enable
            precision mediump float;
            varying float vModelY; varying vec3 vModelPos; varying vec3 vBary; varying vec3 vEdgeMask;
            uniform vec4 uSkinColor; uniform vec4 uSuitColor; uniform vec4 uMeshColor; 
            uniform float uShowSkin; uniform mat4 uModelView;
            
            void main() {
                float head = step(1.08, vModelY);
                float hands = step(0.36, abs(vModelPos.x)) * step(-1.05, vModelY) * step(vModelY, 0.05);
                float feet = step(vModelY, -1.32);
                float skinMask = min(1.0, head + hands + feet) * uShowSkin;

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