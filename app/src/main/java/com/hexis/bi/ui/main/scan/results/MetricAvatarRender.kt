package com.hexis.bi.ui.main.scan.results

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Renders the metric avatar (body OBJ model) into a Compose surface with drag-to-rotate gestures.
 * Falls back to a Retry button when the OBJ download or parse fails.
 */
@Composable
internal fun MetricAvatarPreview(
    modelUrl: String,
    onInteractionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showSkinAreas: Boolean = false,
) {
    val context = LocalContext.current
    // Capture the latest callback so we don't recreate the GLSurfaceView on each recomposition.
    val latestOnInteraction by rememberUpdatedState(onInteractionChanged)
    val view = remember(context) {
        MetricAvatarSurfaceView(context) { latestOnInteraction(it) }
    }
    var hasError by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(view, showSkinAreas) {
        view.setShowSkinAreas(showSkinAreas)
    }

    LaunchedEffect(modelUrl, reloadKey) {
        runCatching { withContext(Dispatchers.IO) { ObjParser.parse(modelUrl) } }
            .onSuccess { mesh ->
                view.queueEvent { view.setMesh(mesh) }
                hasError = false
            }
            .onFailure { e ->
                Timber.w(e, "Metric avatar OBJ load failed url=%s", modelUrl)
                hasError = true
            }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { view },
        )
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AppButton(
                    text = stringResource(R.string.action_retry),
                    onClick = {
                        hasError = false
                        reloadKey++
                    },
                )
            }
        }
    }
}

private const val TOUCH_YAW_SENSITIVITY = 0.25f
private const val TOUCH_PITCH_SENSITIVITY = 0.2f

private class MetricAvatarSurfaceView(
    context: Context,
    private val onInteractionChanged: (Boolean) -> Unit,
) : GLSurfaceView(context) {
    private val avatarRenderer = MetricAvatarRenderer()
    private var lastX = 0f
    private var lastY = 0f

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        setRenderer(avatarRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setMesh(mesh: ObjMesh) {
        avatarRenderer.setMesh(mesh)
    }

    fun setShowSkinAreas(show: Boolean) {
        queueEvent { avatarRenderer.setShowSkinAreas(show) }
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
                queueEvent {
                    avatarRenderer.rotateBy(
                        deltaYaw = dx * TOUCH_YAW_SENSITIVITY,
                        deltaPitch = dy * TOUCH_PITCH_SENSITIVITY,
                    )
                }
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onInteractionChanged(false)
            }
        }
        return true
    }
}

internal data class ObjMesh(val vertexBuffer: FloatBuffer, val vertexCount: Int)

private object ObjParser {
    private const val NORMALIZED_TARGET_SIZE = 3.0f
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000
    // Each vertex emits position (x,y,z) + barycentric coord (bx,by,bz). Bary lets the fragment
    // shader draw a true triangle-edge wireframe with screen-space (depth-aware) line width.
    private const val FLOATS_PER_VERTEX = 6
    private const val BYTES_PER_FLOAT = 4

    fun parse(url: String): ObjMesh {
        val vertices = ArrayList<FloatArray>()
        val triangles = ArrayList<Float>()
        val connection = URL(url).openConnection().apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
        BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
            reader.lineSequence().forEach { line ->
                when {
                    line.startsWith("v ") -> parseVertex(line, vertices)
                    line.startsWith("f ") -> parseFace(line, vertices, triangles)
                }
            }
        }
        if (triangles.isEmpty()) error("OBJ has no triangles")
        normalize(triangles)

        val buffer = ByteBuffer
            .allocateDirect(triangles.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
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
            ),
        )
    }

    private fun parseFace(
        line: String,
        vertices: List<FloatArray>,
        triangles: MutableList<Float>,
    ) {
        val faceParts = line.trim().split(Regex("\\s+")).drop(1)
        if (faceParts.size < 3) return

        // Resolve indices first; reject the whole face if any token is malformed or out of range.
        // Bailing per-token would produce orphan triangles with mismatched vertex counts.
        val indices = ArrayList<Int>(faceParts.size)
        for (token in faceParts) {
            val raw = token.substringBefore("/").toIntOrNull() ?: return
            // OBJ supports negative indices, counted backwards from the current vertex list.
            val resolved = if (raw < 0) vertices.size + raw else raw - 1
            if (resolved !in vertices.indices) return
            indices.add(resolved)
        }

        // Triangulate as a fan from the first vertex, assigning each of the three vertices
        // a unit-basis barycentric so fwidth() in the fragment shader can detect edges.
        for (i in 1 until indices.size - 1) {
            appendVertex(triangles, vertices[indices[0]], 1f, 0f, 0f)
            appendVertex(triangles, vertices[indices[i]], 0f, 1f, 0f)
            appendVertex(triangles, vertices[indices[i + 1]], 0f, 0f, 1f)
        }
    }

    private fun appendVertex(
        into: MutableList<Float>,
        vertex: FloatArray,
        bx: Float,
        by: Float,
        bz: Float,
    ) {
        into.add(vertex[0])
        into.add(vertex[1])
        into.add(vertex[2])
        into.add(bx)
        into.add(by)
        into.add(bz)
    }

    private fun normalize(triangles: MutableList<Float>) {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (i in triangles.indices step FLOATS_PER_VERTEX) {
            val x = triangles[i]
            val y = triangles[i + 1]
            val z = triangles[i + 2]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        val largestExtent = maxOf(maxX - minX, maxY - minY, maxZ - minZ, 1e-6f)
        val scale = NORMALIZED_TARGET_SIZE / largestExtent
        for (i in triangles.indices step FLOATS_PER_VERTEX) {
            triangles[i] = (triangles[i] - cx) * scale
            triangles[i + 1] = (triangles[i + 1] - cy) * scale
            triangles[i + 2] = (triangles[i + 2] - cz) * scale
        }
    }
}

private class MetricAvatarRenderer : GLSurfaceView.Renderer {
    private var mesh: ObjMesh? = null
    private var program = 0
    private var aPosition = 0
    private var aBary = 0
    private var uMvp = 0
    private var uSkinColor = 0
    private var uSuitColor = 0
    private var uSuitLevel = 0
    private var uShowSkin = 0
    private var uModelView = 0
    private var uMeshColor = 0
    private var showSkinAreas = false
    private val projection = FloatArray(MATRIX_SIZE)
    private val view = FloatArray(MATRIX_SIZE)
    private val model = FloatArray(MATRIX_SIZE)
    private val temp = FloatArray(MATRIX_SIZE)
    private val mvp = FloatArray(MATRIX_SIZE)
    private var yaw = 0f
    private var pitch = INITIAL_PITCH_DEG
    private var viewDistance = INITIAL_VIEW_DISTANCE

    fun setMesh(value: ObjMesh) {
        mesh = value
    }

    fun setShowSkinAreas(show: Boolean) {
        showSkinAreas = show
    }

    fun rotateBy(deltaYaw: Float, deltaPitch: Float) {
        yaw += deltaYaw
        pitch = (pitch + deltaPitch).coerceIn(MIN_PITCH_DEG, MAX_PITCH_DEG)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) return
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aBary = GLES20.glGetAttribLocation(program, "aBary")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
        uSkinColor = GLES20.glGetUniformLocation(program, "uSkinColor")
        uSuitColor = GLES20.glGetUniformLocation(program, "uSuitColor")
        uSuitLevel = GLES20.glGetUniformLocation(program, "uSuitLevel")
        uShowSkin = GLES20.glGetUniformLocation(program, "uShowSkin")
        uModelView = GLES20.glGetUniformLocation(program, "uModelView")
        uMeshColor = GLES20.glGetUniformLocation(program, "uMeshColor")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projection, 0, FOV_DEG, aspect, FRUSTUM_NEAR, FRUSTUM_FAR)
        val tanHalfFov = kotlin.math.tan(Math.toRadians(FOV_DEG / 2.0)).toFloat()
            .coerceAtLeast(0.001f)
        // Frame the model independently of parser scale so future scale changes
        // actually move it on screen.
        val halfHeight = 1.0f
        val halfWidth = 1.0f
        val distForHeight = halfHeight / tanHalfFov
        val distForWidth = halfWidth / (tanHalfFov * aspect.coerceAtLeast(MIN_ASPECT_FOR_FRAMING))
        viewDistance = maxOf(distForHeight, distForWidth) * VIEW_DISTANCE_SAFETY_MARGIN
        Matrix.setLookAtM(
            view, 0,
            0f, EYE_HEIGHT, viewDistance,
            0f, 0f, 0f,
            0f, 1f, 0f,
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val currentMesh = mesh ?: return
        if (program == 0) return
        GLES20.glUseProgram(program)

        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, pitch, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, yaw, 0f, 1f, 0f)

        Matrix.multiplyMM(temp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0)

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        // temp = view * model; fragment uses it to push model-space normals into view space for rim lighting.
        GLES20.glUniformMatrix4fv(uModelView, 1, false, temp, 0)
        GLES20.glUniform4f(uSkinColor, SKIN_R, SKIN_G, SKIN_B, 1f)
        GLES20.glUniform4f(uSuitColor, SUIT_R, SUIT_G, SUIT_B, 1f)
        GLES20.glUniform4f(uMeshColor, MESH_R, MESH_G, MESH_B, 1f)
        GLES20.glUniform1f(uSuitLevel, SUIT_LEVEL)
        GLES20.glUniform1f(uShowSkin, if (showSkinAreas) 1f else 0f)

        val buf = currentMesh.vertexBuffer
        buf.position(0)
        GLES20.glVertexAttribPointer(
            aPosition, 3, GLES20.GL_FLOAT, false, FLOAT_STRIDE, buf,
        )
        GLES20.glEnableVertexAttribArray(aPosition)
        if (aBary >= 0) {
            buf.position(POSITION_FLOAT_COUNT)
            GLES20.glVertexAttribPointer(
                aBary, 3, GLES20.GL_FLOAT, false, FLOAT_STRIDE, buf,
            )
            GLES20.glEnableVertexAttribArray(aBary)
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, currentMesh.vertexCount)
        GLES20.glDisableVertexAttribArray(aPosition)
        if (aBary >= 0) GLES20.glDisableVertexAttribArray(aBary)
    }

    private fun createProgram(vsSource: String, fsSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vsSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSource)
        if (vertexShader == 0 || fragmentShader == 0) return 0
        val programId = GLES20.glCreateProgram()
        if (programId == 0) return 0
        GLES20.glAttachShader(programId, vertexShader)
        GLES20.glAttachShader(programId, fragmentShader)
        GLES20.glLinkProgram(programId)
        val status = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == GLES20.GL_FALSE) {
            Timber.e("GL program link failed: %s", GLES20.glGetProgramInfoLog(programId))
            GLES20.glDeleteProgram(programId)
            return 0
        }
        return programId
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == GLES20.GL_FALSE) {
            Timber.e("GL shader compile failed: %s", GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private companion object {
        private const val MATRIX_SIZE = 16
        // Per-vertex layout: 3 floats position + 3 floats barycentric = 6 floats / 24 bytes.
        private const val POSITION_FLOAT_COUNT = 3
        private const val FLOAT_STRIDE = 24

        private const val FOV_DEG = 42f
        private const val FRUSTUM_NEAR = 0.1f
        private const val FRUSTUM_FAR = 100f
        private const val EYE_HEIGHT = 0.85f
        private const val MIN_ASPECT_FOR_FRAMING = 0.6f
        private const val VIEW_DISTANCE_SAFETY_MARGIN = 1.25f
        private const val INITIAL_VIEW_DISTANCE = 3.2f
        private const val INITIAL_PITCH_DEG = -12f
        private const val MIN_PITCH_DEG = -55f
        private const val MAX_PITCH_DEG = 35f

        // Approximate body styling: skin tone for exposed areas, dark suit elsewhere.
        private const val SKIN_R = 0.84f
        private const val SKIN_G = 0.68f
        private const val SKIN_B = 0.58f
        // Suit fill stays black; the mesh wireframe + rim glow use theme Blue300 (#0030FF).
        private const val SUIT_R = 0.00f
        private const val SUIT_G = 0.00f
        private const val SUIT_B = 0.00f
        private const val SUIT_LEVEL = 0.52f
        private const val MESH_R = 0.00f
        private const val MESH_G = 0.188f
        private const val MESH_B = 1.00f

        private const val VERTEX_SHADER = """
            attribute vec3 aPosition;
            attribute vec3 aBary;
            uniform mat4 uMvp;
            varying float vModelY;
            varying vec3 vModelPos;
            varying vec3 vBary;
            void main() {
                vModelY = aPosition.y;
                vModelPos = aPosition;
                vBary = aBary;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_standard_derivatives : enable
            precision mediump float;
            varying float vModelY;
            varying vec3 vModelPos;
            varying vec3 vBary;
            uniform vec4 uSkinColor;
            uniform vec4 uSuitColor;
            uniform vec4 uMeshColor;
            uniform float uSuitLevel;
            uniform float uShowSkin;
            uniform mat4 uModelView;
            void main() {
                // Skin masks (head / palms / feet) — only contribute when host enables them.
                float headMask = step(1.08, vModelY);
                float handMask =
                    step(0.36, abs(vModelPos.x)) *
                    step(-1.05, vModelY) *
                    step(vModelY, 0.05);
                float feetMask = step(vModelY, -1.32);
                float skinMask = min(1.0, headMask + handMask + feetMask) * uShowSkin;

                // View-space face normal (flat per-triangle) used for shading + rim.
                // Derivatives give a model-space normal; uModelView pushes it to view space
                // so the camera sits at +Z.
                vec3 fdx = dFdx(vModelPos);
                vec3 fdy = dFdy(vModelPos);
                vec3 nModel = normalize(cross(fdx, fdy));
                vec3 nView = normalize((uModelView * vec4(nModel, 0.0)).xyz);

                // Lambert shading: key light from upper-front-right, soft fill from below-left.
                vec3 keyDir = normalize(vec3(0.45, 0.55, 0.85));
                vec3 fillDir = normalize(vec3(-0.50, -0.30, 0.60));
                float key = max(dot(nView, keyDir), 0.0);
                float fill = max(dot(nView, fillDir), 0.0) * 0.35;
                float ambient = 0.25;
                float shade = clamp(ambient + key * 0.85 + fill, 0.0, 1.0);

                // True triangle-edge wireframe: barycentric coords reach 0 at each edge of the
                // source triangle. fwidth() gives the screen-space derivative magnitude, so the
                // line stays a constant pixel width regardless of distance — depth-aware lines
                // that thin out toward the silhouette / far-away parts.
                vec3 baryWidth = fwidth(vBary);
                vec3 baryAA = smoothstep(vec3(0.0), baryWidth * 1.6, vBary);
                float edge = 1.0 - min(min(baryAA.x, baryAA.y), baryAA.z);
                // Slight thickness lift so the wireframe reads even at oblique angles.
                edge = clamp(edge * 1.15, 0.0, 1.0);
                vec3 shadedMesh = uMeshColor.rgb * shade;
                vec3 suitMesh = mix(uSuitColor.rgb, shadedMesh, edge);

                // Rim glow: silhouette edges (surface facing perpendicular to camera) light up.
                float silhouette = 1.0 - abs(nView.z);
                // Two-lobe glow: a wide soft halo plus a tighter bright edge.
                float rimSoft = pow(silhouette, 1.4);
                float rimSharp = pow(silhouette, 4.0);
                vec3 rimTint = uMeshColor.rgb + vec3(0.05, 0.15, 0.25);
                vec3 lit = suitMesh + rimTint * (rimSoft * 1.35 + rimSharp * 1.10);

                vec3 finalRgb = mix(lit, uSkinColor.rgb, skinMask);
                gl_FragColor = vec4(finalRgb, 1.0);
            }
        """
    }
}
