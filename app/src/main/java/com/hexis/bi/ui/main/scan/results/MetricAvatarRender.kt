package com.hexis.bi.ui.main.scan.results

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.LruCache
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.main.scan.results.MetricAvatarFrameSolver.buildFrames
import com.hexis.bi.ui.main.scan.results.MetricAvatarFrameSolver.manualBounds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * Radial preview backdrop behind the GL mesh — uses [@color/metric_avatar_preview_gradient_inner] /
 * [@color/metric_avatar_preview_gradient_outer] (same as [MetricAvatarRenderer] GL uniforms).
 */
@Composable
internal fun Modifier.metricAvatarPreviewGradientBackground(): Modifier {
    val inner = colorResource(R.color.metric_avatar_preview_gradient_inner)
    val outer = colorResource(R.color.metric_avatar_preview_gradient_outer)
    val brush = remember(inner, outer) {
        Brush.radialGradient(
            colors = listOf(inner, outer),
            center = Offset.Unspecified,
            radius = Float.POSITIVE_INFINITY,
        )
    }
    return this.background(brush)
}

/** Default starting orientation for the rotatable mesh, in degrees. */
private const val INITIAL_PITCH_DEG = -12f
private const val MIN_PITCH_DEG = -55f
private const val MAX_PITCH_DEG = 35f

internal class CompareRotationLink {
    @Volatile
    var yaw: Float = 0f

    @Volatile
    var pitch: Float = INITIAL_PITCH_DEG

    @Volatile
    var zoom: Float = MetricAvatarCamera.MIN_USER_ZOOM

    @Volatile
    var panX: Float = 0f

    @Volatile
    var panY: Float = 0f

    // Animated reset of user yaw-spin / zoom / pan back to defaults when the framed part changes.
    private var resetAnimating = false
    private var resetStartMs = 0L
    private var fromYaw = 0f
    private var fromZoom = MetricAvatarCamera.MIN_USER_ZOOM
    private var fromPanX = 0f
    private var fromPanY = 0f

    private val invalidateCallbacks = CopyOnWriteArrayList<() -> Unit>()

    fun addInvalidateCallback(callback: () -> Unit) {
        invalidateCallbacks.add(callback)
    }

    fun removeInvalidateCallback(callback: () -> Unit) {
        invalidateCallbacks.remove(callback)
    }

    fun applyZoomFactor(factor: Float) {
        synchronized(this) {
            resetAnimating = false
            zoom = (zoom * factor).coerceIn(
                MetricAvatarCamera.MIN_USER_ZOOM,
                MetricAvatarCamera.MAX_USER_ZOOM,
            )
        }
        invalidateCallbacks.forEach { it() }
    }

    fun applyPanDelta(dxWorld: Float, dyWorld: Float, maxX: Float, maxY: Float) {
        synchronized(this) {
            resetAnimating = false
            panX = (panX + dxWorld).coerceIn(-maxX, maxX)
            panY = (panY + dyWorld).coerceIn(-maxY, maxY)
        }
        invalidateCallbacks.forEach { it() }
    }

    fun clampPan(maxX: Float, maxY: Float) {
        synchronized(this) {
            panX = panX.coerceIn(-maxX, maxX)
            panY = panY.coerceIn(-maxY, maxY)
        }
        invalidateCallbacks.forEach { it() }
    }

    /**
     * Animate the user's yaw-spin / zoom / pan back to defaults (used when the framed part changes).
     * Safe to call from both linked models for the same transition — only the first snapshots the
     * starting values; the [displayYaw]/[displayZoom]/[displayPan] getters interpolate to default.
     */
    fun beginResetAnimation() {
        synchronized(this) {
            val now = SystemClock.uptimeMillis()
            if (!resetAnimating || now - resetStartMs >= FRAME_ANIMATION_DURATION_MS) {
                fromYaw = yaw
                fromZoom = zoom
                fromPanX = panX
                fromPanY = panY
                resetStartMs = now
                resetAnimating = true
            }
            yaw = 0f
            pitch = INITIAL_PITCH_DEG
            zoom = MetricAvatarCamera.MIN_USER_ZOOM
            panX = 0f
            panY = 0f
        }
        invalidateCallbacks.forEach { it() }
    }

    fun resetAdjustmentsImmediate() {
        synchronized(this) {
            resetAnimating = false
            yaw = 0f
            pitch = INITIAL_PITCH_DEG
            zoom = MetricAvatarCamera.MIN_USER_ZOOM
            panX = 0f
            panY = 0f
        }
        invalidateCallbacks.forEach { it() }
    }

    /** Eased reset progress in [0,1]; clears the animation flag once complete. */
    private fun resetProgress(): Float {
        if (!resetAnimating) return 1f
        val linear = (SystemClock.uptimeMillis() - resetStartMs).toFloat() / FRAME_ANIMATION_DURATION_MS
        if (linear >= 1f) {
            resetAnimating = false
            return 1f
        }
        return easeInOutSine(linear)
    }

    fun displayYaw(): Float = synchronized(this) {
        if (!resetAnimating) yaw else lerpAngleDegrees(fromYaw, yaw, resetProgress())
    }

    fun displayZoom(): Float = synchronized(this) {
        if (!resetAnimating) zoom else lerp(fromZoom, zoom, resetProgress())
    }

    fun displayPanX(): Float = synchronized(this) {
        if (!resetAnimating) panX else lerp(fromPanX, panX, resetProgress())
    }

    fun displayPanY(): Float = synchronized(this) {
        if (!resetAnimating) panY else lerp(fromPanY, panY, resetProgress())
    }

    fun applyRotationDelta(dyaw: Float, dpitch: Float) {
        synchronized(this) {
            resetAnimating = false
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
    modifier: Modifier = Modifier,
    modelUrl: String,
    useModelVertexColors: Boolean = false,
    onInteractionChanged: (Boolean) -> Unit,
    showSkinAreas: Boolean = false,
    drawBackground: Boolean = true,
    touchRotationEnabled: Boolean = true,
    zoomPanEnabled: Boolean = false,
    baseDistanceScale: Float = 1f,
    meshGlow: Float = 0f,
    /** When false, the parent already drew [metricAvatarPreviewGradientBackground]. */
    useGradientBackground: Boolean = true,
    initialYawDegrees: Float = 0f,
    initialPitchDegrees: Float = INITIAL_PITCH_DEG,
    compareRotationLink: CompareRotationLink? = null,
    /** Invoked on the main thread when yaw/pitch or viewport size change (Visual overlay). Ignored when [compareRotationLink] is non-null. */
    onVisualTransformChanged: ((yawDeg: Float, pitchDeg: Float, viewWidthPx: Int, viewHeightPx: Int) -> Unit)? = null,
    /** 3D leader segments (depth-tested in GL). Null or empty clears. */
    leaderSegments: List<ModelLeaderSegment>? = null,
    /** Invoked on the main thread after OBJ parse (anchors + slice polylines for circumference labels). */
    onMeasurementGuideLoaded: ((MetricAvatarMeasurementGuide) -> Unit)? = null,
    /** Invoked on the main thread when the mesh is loaded and the GL surface is visible (opaque). */
    onAvatarReady: (() -> Unit)? = null,
    /** Visual tab: model-space auto-framing region. Null keeps the default camera/orientation behavior. */
    framingRegion: BodyMeasurementRegion? = null,
    /** When true, framing keeps the figure horizontally centered (Compare) instead of right-shifted (Visual). */
    centerFraming: Boolean = false,
    @StringRes loadingMessageRes: Int = R.string.scan_results_avatar_loading,
) {
    val context = LocalContext.current
    val latestOnInteraction by rememberUpdatedState(onInteractionChanged)
    val latestTransform by rememberUpdatedState(onVisualTransformChanged)
    val latestAnchors by rememberUpdatedState(onMeasurementGuideLoaded)
    val latestOnAvatarReady by rememberUpdatedState(onAvatarReady)
    val renderHost = remember(context) {
        MetricAvatarTextureView(context) { latestOnInteraction(it) }
    }
    var loadState by remember { mutableStateOf(MetricAvatarLoadState.Loading) }
    // Wait for a rendered frame to avoid a white flash on model switches.
    var firstFrameReady by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val latestRenderFailure by rememberUpdatedState {
        loadState = MetricAvatarLoadState.Error
    }

    LaunchedEffect(renderHost, showSkinAreas) { renderHost.setShowSkinAreas(showSkinAreas) }
    LaunchedEffect(renderHost, drawBackground) { renderHost.setDrawBackground(drawBackground) }
    LaunchedEffect(renderHost, touchRotationEnabled) {
        renderHost.setTouchRotationEnabled(
            touchRotationEnabled
        )
    }
    LaunchedEffect(renderHost, zoomPanEnabled) { renderHost.setZoomPanEnabled(zoomPanEnabled) }
    LaunchedEffect(renderHost, baseDistanceScale) {
        renderHost.setBaseDistanceScale(
            baseDistanceScale
        )
    }
    LaunchedEffect(renderHost, meshGlow) { renderHost.setMeshGlow(meshGlow) }
    LaunchedEffect(renderHost, framingRegion) { renderHost.setFramingRegion(framingRegion) }
    LaunchedEffect(renderHost, centerFraming) { renderHost.setCenterFraming(centerFraming) }

    DisposableEffect(renderHost) {
        renderHost.setRenderFailureListener { latestRenderFailure() }
        renderHost.setOnFirstFrameRendered { firstFrameReady = true }
        renderHost.onResumeHost()
        onDispose {
            renderHost.setRenderFailureListener(null)
            renderHost.setOnFirstFrameRendered(null)
            renderHost.onPauseHost()
        }
    }

    LaunchedEffect(renderHost, leaderSegments, loadState, compareRotationLink) {
        when {
            loadState != MetricAvatarLoadState.Ready -> renderHost.setLeaderSegments(null)
            compareRotationLink != null -> renderHost.setLeaderSegments(null)
            else -> renderHost.setLeaderSegments(leaderSegments)
        }
    }

    DisposableEffect(renderHost, latestTransform) {
        renderHost.setVisualTransformListener(latestTransform)
        onDispose { renderHost.setVisualTransformListener(null) }
    }

    DisposableEffect(renderHost, compareRotationLink) {
        renderHost.setCompareRotationLink(compareRotationLink)
        onDispose { renderHost.setCompareRotationLink(null) }
    }

    LaunchedEffect(loadState, modelUrl, useModelVertexColors, reloadKey) {
        if (loadState == MetricAvatarLoadState.Ready) {
            latestOnAvatarReady?.invoke()
        }
    }

    val latestYawAtLoad by rememberUpdatedState(initialYawDegrees)
    val latestPitchAtLoad by rememberUpdatedState(initialPitchDegrees)

    LaunchedEffect(modelUrl, useModelVertexColors, reloadKey, compareRotationLink) {
        compareRotationLink?.let { renderHost.setCompareRotationLink(it) }
        loadState = MetricAvatarLoadState.Loading
        firstFrameReady = false
        try {
            val cacheDir = context.cacheDir
            val mesh = withContext(Dispatchers.IO) {
                ObjParser.parse(modelUrl, cacheDir, includeVertexColors = useModelVertexColors)
            }
            if (compareRotationLink != null) {
                renderHost.applyLoadedMeshForCompare(mesh)
            } else {
                renderHost.applyLoadedMesh(mesh, latestYawAtLoad, latestPitchAtLoad)
            }
            latestAnchors?.invoke(mesh.measurementGuide)
            loadState = MetricAvatarLoadState.Ready
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(
                e,
                "Metric avatar OBJ load failed model=%s useColors=%s",
                modelUrl,
                useModelVertexColors
            )
            loadState = MetricAvatarLoadState.Error
        }
    }

    LaunchedEffect(
        initialYawDegrees,
        initialPitchDegrees,
        loadState,
        compareRotationLink,
        framingRegion
    ) {
        if (loadState != MetricAvatarLoadState.Ready || compareRotationLink != null) return@LaunchedEffect
        if (framingRegion != null) return@LaunchedEffect
        renderHost.setBaseOrientation(initialYawDegrees, initialPitchDegrees)
    }

    val containerModifier = if (useGradientBackground && drawBackground) {
        modifier.metricAvatarPreviewGradientBackground()
    } else {
        modifier
    }
    Box(modifier = containerModifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .graphicsLayer {
                    alpha = if (firstFrameReady) 1f else 0f
                    // Keep GL layer composited so the surface still lays out while loading (avoids 0×0 / stuck first frame on some devices).
                    compositingStrategy = CompositingStrategy.Auto
                },
            factory = { renderHost.view },
        )
        when {
            loadState == MetricAvatarLoadState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarRetryButton(onClick = { reloadKey++ })
                }
            }

            !firstFrameReady -> {
                MetricAvatarLoading(
                    messageRes = loadingMessageRes,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
internal fun MetricAvatarLoading(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            MetricAvatarStatusText(messageRes = messageRes)
        }
    }
}

@Composable
internal fun MetricAvatarStatusText(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.widthIn(
            max = dimensionResource(R.dimen.metric_avatar_text_max_width),
        ),
    )
}

@Composable
private fun AvatarRetryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = dimensionResource(R.dimen.border_line),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacer_l),
            vertical = dimensionResource(R.dimen.spacer_xxs),
        ),
        modifier = modifier
            .height(dimensionResource(R.dimen.metric_avatar_retry_button_height)),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.metric_avatar_retry_icon_size)),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
        Text(
            text = stringResource(R.string.action_retry),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private enum class MetricAvatarLoadState {
    Loading,
    Ready,
    Error,
}

private const val TOUCH_YAW_SENSITIVITY = 0.25f
private const val TOUCH_PITCH_SENSITIVITY = 0.2f
private const val TWO_FINGER_ZOOM_SLOP_MULTIPLIER = 1.25f
private const val TWO_FINGER_ZOOM_DOMINANCE_RATIO = 1.25f
private const val TWO_FINGER_ZOOM_SENSITIVITY = 0.85f
private const val FRAME_ANIMATION_DURATION_MS = 360L
private const val FRAME_ANIMATION_FRAME_DELAY_MS = 16L
private const val RENDER_THREAD_JOIN_TIMEOUT_MS = 250L

private const val EGL_CONTEXT_CLIENT_VERSION = 2
private const val EGL_COLOR_CHANNEL_BITS = 8
private const val EGL_RGB565_RED_BITS = 5
private const val EGL_RGB565_GREEN_BITS = 6
private const val EGL_RGB565_BLUE_BITS = 5
private const val EGL_NO_ALPHA_BITS = 0
private const val EGL_DEPTH_BITS = 16
private const val EGL_NO_DEPTH_BITS = 0
private const val EGL_STENCIL_BITS = 0
private const val EGL_SAMPLE_BUFFERS = 0x3032
private const val EGL_SAMPLES = 0x3031
private const val EGL_MSAA_SAMPLES = 4
private const val EGL_NO_MSAA_SAMPLES = 0

private interface MetricAvatarRenderHost {
    val view: View
    fun onResumeHost()
    fun onPauseHost()
    fun applyLoadedMesh(mesh: ObjMesh, yawDeg: Float, pitchDeg: Float)
    fun applyLoadedMeshForCompare(mesh: ObjMesh)
    fun setCompareRotationLink(link: CompareRotationLink?)
    fun setShowSkinAreas(show: Boolean)
    fun setMeshGlow(glow: Float)
    fun setDrawBackground(draw: Boolean)
    fun setTouchRotationEnabled(enabled: Boolean)
    fun setZoomPanEnabled(enabled: Boolean)
    fun setBaseDistanceScale(scale: Float)
    fun setVisualTransformListener(listener: ((Float, Float, Int, Int) -> Unit)?)
    fun setLeaderSegments(segments: List<ModelLeaderSegment>?)
    fun setBaseOrientation(yawDeg: Float, pitchDeg: Float)
    fun setFramingRegion(region: BodyMeasurementRegion?)
    fun setCenterFraming(enabled: Boolean)
    fun setRenderFailureListener(listener: (() -> Unit)?)
    fun setOnFirstFrameRendered(listener: (() -> Unit)?)
}

private enum class TwoFingerGesture { UNDECIDED, ZOOM, PAN }

private class MetricAvatarTextureView(
    context: Context,
    private var onInteractionChanged: (Boolean) -> Unit,
) : TextureView(context), TextureView.SurfaceTextureListener, MetricAvatarRenderHost {
    private val avatarRenderer = MetricAvatarRenderer()
    private var touchRotationEnabled = true
    private var zoomPanEnabled = false
    private var lastX = 0f
    private var lastY = 0f
    private var lastCentroidX = 0f
    private var lastCentroidY = 0f
    private var twoFingerGesture: TwoFingerGesture? = null
    private var gestureStartSpan = 0f
    private var gestureLastSpan = 0f
    private var gestureStartCx = 0f
    private var gestureStartCy = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null
    private var boundCompareLink: CompareRotationLink? = null
    private var renderFailureListener: (() -> Unit)? = null
    private var onFirstFrameRendered: (() -> Unit)? = null
    private var pendingFirstFrameNotify = false
    private var animationFrameScheduled = false
    private val pendingEvents = mutableListOf<() -> Unit>()
    private val compareRenderNotify: () -> Unit = { requestRenderOnThread() }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val eglConfigProfiles = listOf(
        EglConfigProfile(
            name = "rgba8888-depth16-msaa4",
            redBits = EGL_COLOR_CHANNEL_BITS,
            greenBits = EGL_COLOR_CHANNEL_BITS,
            blueBits = EGL_COLOR_CHANNEL_BITS,
            alphaBits = EGL_COLOR_CHANNEL_BITS,
            depthBits = EGL_DEPTH_BITS,
            samples = EGL_MSAA_SAMPLES,
        ),
        EglConfigProfile(
            name = "rgba8888-depth16",
            redBits = EGL_COLOR_CHANNEL_BITS,
            greenBits = EGL_COLOR_CHANNEL_BITS,
            blueBits = EGL_COLOR_CHANNEL_BITS,
            alphaBits = EGL_COLOR_CHANNEL_BITS,
            depthBits = EGL_DEPTH_BITS,
            samples = EGL_NO_MSAA_SAMPLES,
        ),
        EglConfigProfile(
            name = "rgb888-depth16",
            redBits = EGL_COLOR_CHANNEL_BITS,
            greenBits = EGL_COLOR_CHANNEL_BITS,
            blueBits = EGL_COLOR_CHANNEL_BITS,
            alphaBits = EGL_NO_ALPHA_BITS,
            depthBits = EGL_DEPTH_BITS,
            samples = EGL_NO_MSAA_SAMPLES,
        ),
        EglConfigProfile(
            name = "rgb565-depth16",
            redBits = EGL_RGB565_RED_BITS,
            greenBits = EGL_RGB565_GREEN_BITS,
            blueBits = EGL_RGB565_BLUE_BITS,
            alphaBits = EGL_NO_ALPHA_BITS,
            depthBits = EGL_DEPTH_BITS,
            samples = EGL_NO_MSAA_SAMPLES,
        ),
        EglConfigProfile(
            name = "rgb565-no-depth",
            redBits = EGL_RGB565_RED_BITS,
            greenBits = EGL_RGB565_GREEN_BITS,
            blueBits = EGL_RGB565_BLUE_BITS,
            alphaBits = EGL_NO_ALPHA_BITS,
            depthBits = EGL_NO_DEPTH_BITS,
            samples = EGL_NO_MSAA_SAMPLES,
        ),
    )

    override val view: View get() = this

    init {
        isOpaque = false
        surfaceTextureListener = this
        avatarRenderer.setPreviewGradientFromResources(context)
    }

    override fun onResumeHost() = Unit

    override fun onPauseHost() {
        stopRenderThread()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        startRenderThread(surface, width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        queueRenderEvent {
            avatarRenderer.onSurfaceChanged(width, height)
            drawFrame()
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRenderThread()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        if (pendingFirstFrameNotify) {
            pendingFirstFrameNotify = false
            onFirstFrameRendered?.invoke()
        }
    }

    private fun startRenderThread(surface: SurfaceTexture, width: Int, height: Int) {
        if (renderThread != null) return
        val thread = HandlerThread("MetricAvatarTextureRenderer").also { it.start() }
        val handler = Handler(thread.looper)
        renderThread = thread
        renderHandler = handler
        handler.post {
            if (!initEgl(surface)) {
                handleRenderThreadInitFailure(thread)
                return@post
            }
            avatarRenderer.onSurfaceCreated()
            avatarRenderer.onSurfaceChanged(width, height)
            synchronized(pendingEvents) {
                pendingEvents.forEach { it() }
                pendingEvents.clear()
            }
            drawFrame()
        }
    }

    private fun stopRenderThread() {
        val handler = renderHandler ?: return
        val thread = renderThread
        renderHandler = null
        renderThread = null
        handler.post {
            releaseEgl()
            thread?.quitSafely()
        }
        // Avoid a new render thread racing EGL teardown.
        thread?.join(RENDER_THREAD_JOIN_TIMEOUT_MS)
    }

    private fun handleRenderThreadInitFailure(thread: HandlerThread) {
        releaseEgl()
        renderHandler = null
        renderThread = null
        thread.quitSafely()
        notifyRenderFailure()
    }

    private fun queueRenderEvent(block: () -> Unit) {
        val handler = renderHandler
        if (handler == null) {
            synchronized(pendingEvents) { pendingEvents += block }
        } else {
            handler.post(block)
        }
    }

    private fun requestRenderOnThread() {
        queueRenderEvent { drawFrame() }
    }

    private fun drawFrame() {
        animationFrameScheduled = false
        val display = eglDisplay ?: return
        val surface = eglSurface ?: return
        avatarRenderer.onDrawFrame()
        if (!EGL14.eglSwapBuffers(display, surface)) {
            Timber.w("Metric avatar EGL swap failed: error=%s", eglErrorString())
            notifyRenderFailure()
            return
        }
        scheduleAnimationFrameIfNeeded()
    }

    private fun scheduleAnimationFrameIfNeeded() {
        val handler = renderHandler ?: return
        if (!avatarRenderer.isFrameAnimationRunning()) return
        if (animationFrameScheduled) return
        animationFrameScheduled = true
        handler.postDelayed({ drawFrame() }, FRAME_ANIMATION_FRAME_DELAY_MS)
    }

    private fun initEgl(surfaceTexture: SurfaceTexture): Boolean {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return false
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) return false

        eglConfigProfiles.forEach { profile ->
            if (initEglWithProfile(display, surfaceTexture, profile)) {
                Timber.d("Metric avatar EGL initialized with config=%s", profile.name)
                return true
            }
            Timber.w(
                "Metric avatar EGL init failed with config=%s, retrying fallback: error=%s",
                profile.name,
                eglErrorString(),
            )
        }

        Timber.w("Metric avatar EGL init failed for every fallback config")
        EGL14.eglTerminate(display)
        return false
    }

    private fun initEglWithProfile(
        display: EGLDisplay,
        surfaceTexture: SurfaceTexture,
        profile: EglConfigProfile,
    ): Boolean {
        val config = chooseEglConfig(display, profile) ?: return false
        val contextAttribs =
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, EGL_CONTEXT_CLIENT_VERSION, EGL14.EGL_NONE)
        val context =
            EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (context == EGL14.EGL_NO_CONTEXT) {
            return false
        }
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        val surface =
            EGL14.eglCreateWindowSurface(display, config, surfaceTexture, surfaceAttribs, 0)
        if (surface == EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroyContext(display, context)
            return false
        }
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, context)
            return false
        }
        eglDisplay = display
        eglContext = context
        eglSurface = surface
        return true
    }

    private fun chooseEglConfig(display: EGLDisplay, profile: EglConfigProfile): EGLConfig? {
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        val configAttribs = buildEglConfigAttribs(profile)
        if (!EGL14.eglChooseConfig(
                display,
                configAttribs,
                0,
                configs,
                0,
                configs.size,
                numConfigs,
                0
            )
        ) {
            return null
        }
        return configs.firstOrNull()
    }

    private fun buildEglConfigAttribs(profile: EglConfigProfile): IntArray {
        val base = mutableListOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, profile.redBits,
            EGL14.EGL_GREEN_SIZE, profile.greenBits,
            EGL14.EGL_BLUE_SIZE, profile.blueBits,
            EGL14.EGL_ALPHA_SIZE, profile.alphaBits,
            EGL14.EGL_DEPTH_SIZE, profile.depthBits,
            EGL14.EGL_STENCIL_SIZE, EGL_STENCIL_BITS,
        )
        if (profile.samples > EGL_NO_MSAA_SAMPLES) {
            base += EGL_SAMPLE_BUFFERS
            base += 1
            base += EGL_SAMPLES
            base += profile.samples
        }
        base += EGL14.EGL_NONE
        return base.toIntArray()
    }

    private fun eglErrorString(): String {
        val error = EGL14.eglGetError()
        return "0x${error.toString(16)}"
    }

    private fun releaseEgl() {
        val display = eglDisplay ?: return
        val surface = eglSurface
        val context = eglContext
        EGL14.eglMakeCurrent(
            display,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        if (surface != null && surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(
            display,
            surface
        )
        if (context != null && context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(
            display,
            context
        )
        EGL14.eglTerminate(display)
        eglDisplay = null
        eglContext = null
        eglSurface = null
    }

    override fun applyLoadedMesh(mesh: ObjMesh, yawDeg: Float, pitchDeg: Float) {
        pendingFirstFrameNotify = true
        queueRenderEvent {
            avatarRenderer.setRotationLink(null)
            avatarRenderer.setBaseOrientation(yawDeg, pitchDeg)
            avatarRenderer.setMesh(mesh)
            drawFrame()
        }
    }

    override fun applyLoadedMeshForCompare(mesh: ObjMesh) {
        pendingFirstFrameNotify = true
        queueRenderEvent {
            avatarRenderer.setMesh(mesh)
            drawFrame()
        }
    }

    override fun setCompareRotationLink(link: CompareRotationLink?) {
        boundCompareLink?.removeInvalidateCallback(compareRenderNotify)
        boundCompareLink = link
        queueRenderEvent { avatarRenderer.setRotationLink(link) }
        link?.addInvalidateCallback(compareRenderNotify)
    }

    override fun setMeshGlow(glow: Float) {
        queueRenderEvent {
            avatarRenderer.setMeshGlow(glow)
            drawFrame()
        }
    }

    override fun setShowSkinAreas(show: Boolean) {
        queueRenderEvent {
            avatarRenderer.setShowSkinAreas(show)
            drawFrame()
        }
    }

    override fun setDrawBackground(draw: Boolean) {
        queueRenderEvent {
            avatarRenderer.setDrawBackground(draw)
            drawFrame()
        }
    }

    override fun setTouchRotationEnabled(enabled: Boolean) {
        touchRotationEnabled = enabled
    }

    override fun setZoomPanEnabled(enabled: Boolean) {
        zoomPanEnabled = enabled
    }

    override fun setBaseDistanceScale(scale: Float) {
        queueRenderEvent {
            avatarRenderer.setBaseDistanceScale(scale)
            drawFrame()
        }
    }

    override fun setVisualTransformListener(listener: ((Float, Float, Int, Int) -> Unit)?) {
        queueRenderEvent {
            avatarRenderer.setVisualTransformListener(listener)
            drawFrame()
        }
    }

    override fun setLeaderSegments(segments: List<ModelLeaderSegment>?) {
        queueRenderEvent {
            avatarRenderer.setLeaderSegments(segments)
            drawFrame()
        }
    }

    override fun setBaseOrientation(yawDeg: Float, pitchDeg: Float) {
        queueRenderEvent {
            avatarRenderer.setBaseOrientation(yawDeg, pitchDeg)
            drawFrame()
        }
    }

    override fun setFramingRegion(region: BodyMeasurementRegion?) {
        queueRenderEvent {
            avatarRenderer.setFramingRegion(region)
            drawFrame()
        }
    }

    override fun setCenterFraming(enabled: Boolean) {
        queueRenderEvent {
            avatarRenderer.setCenterFraming(enabled)
            drawFrame()
        }
    }

    override fun setRenderFailureListener(listener: (() -> Unit)?) {
        renderFailureListener = listener
    }

    override fun setOnFirstFrameRendered(listener: (() -> Unit)?) {
        onFirstFrameRendered = listener
    }

    private fun notifyRenderFailure() {
        val listener = renderFailureListener ?: return
        mainHandler.post { listener() }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!touchRotationEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Avoid parent scroll intercept during model dragging.
                parent?.requestDisallowInterceptTouchEvent(true)
                onInteractionChanged(true)
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_POINTER_DOWN ->
                if (zoomPanEnabled && event.pointerCount >= 2) beginTwoFinger(event)

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount - 1 < 2) {
                    twoFingerGesture = null
                    val survivor = if (event.actionIndex == 0) 1 else 0
                    lastX = event.getX(survivor)
                    lastY = event.getY(survivor)
                } else if (zoomPanEnabled) {
                    beginTwoFinger(event)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (zoomPanEnabled && twoFingerGesture != null && event.pointerCount >= 2) {
                    handleTwoFingerMove(event)
                } else if (twoFingerGesture == null) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    val link = boundCompareLink
                    if (link != null) {
                        link.applyRotationDelta(
                            dx * TOUCH_YAW_SENSITIVITY,
                            dy * TOUCH_PITCH_SENSITIVITY
                        )
                    } else {
                        queueRenderEvent {
                            avatarRenderer.rotateBy(
                                dyaw = dx * TOUCH_YAW_SENSITIVITY,
                                dpitch = dy * TOUCH_PITCH_SENSITIVITY,
                            )
                            drawFrame()
                        }
                    }
                    lastX = event.x
                    lastY = event.y
                }
            }

            MotionEvent.ACTION_UP -> {
                performClick()
                onInteractionChanged(false)
                twoFingerGesture = null
            }

            MotionEvent.ACTION_CANCEL -> {
                onInteractionChanged(false)
                twoFingerGesture = null
            }
        }
        return true
    }

    private fun beginTwoFinger(event: MotionEvent) {
        twoFingerGesture = TwoFingerGesture.UNDECIDED
        gestureStartSpan = spanOf(event)
        gestureLastSpan = gestureStartSpan
        gestureStartCx = averagePointer(event, excludeIndex = -1, axisX = true)
        gestureStartCy = averagePointer(event, excludeIndex = -1, axisX = false)
        lastCentroidX = gestureStartCx
        lastCentroidY = gestureStartCy
    }

    private fun handleTwoFingerMove(event: MotionEvent) {
        val span = spanOf(event)
        val cx = averagePointer(event, excludeIndex = -1, axisX = true)
        val cy = averagePointer(event, excludeIndex = -1, axisX = false)
        if (twoFingerGesture == TwoFingerGesture.UNDECIDED) {
            val spanDelta = abs(span - gestureStartSpan)
            val panDelta = hypot(cx - gestureStartCx, cy - gestureStartCy)
            val zoomIsClear =
                spanDelta >= touchSlop * TWO_FINGER_ZOOM_SLOP_MULTIPLIER &&
                        spanDelta > panDelta * TWO_FINGER_ZOOM_DOMINANCE_RATIO
            when {
                zoomIsClear -> twoFingerGesture = TwoFingerGesture.ZOOM
                panDelta >= touchSlop -> twoFingerGesture = TwoFingerGesture.PAN
            }
        }
        when (twoFingerGesture) {
            TwoFingerGesture.ZOOM -> if (gestureLastSpan > 0f) applyZoomFactor(span / gestureLastSpan)
            TwoFingerGesture.PAN -> applyPan(cx - lastCentroidX, cy - lastCentroidY)
            else -> Unit
        }
        gestureLastSpan = span
        lastCentroidX = cx
        lastCentroidY = cy
    }

    private fun spanOf(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        return hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0))
    }

    private fun averagePointer(event: MotionEvent, excludeIndex: Int, axisX: Boolean): Float {
        var sum = 0f
        var count = 0
        for (i in 0 until event.pointerCount) {
            if (i == excludeIndex) continue
            sum += if (axisX) event.getX(i) else event.getY(i)
            count++
        }
        return if (count == 0) {
            if (axisX) event.getX(0) else event.getY(0)
        } else {
            sum / count
        }
    }

    private fun applyZoomFactor(factor: Float) {
        queueRenderEvent {
            avatarRenderer.zoomBy(factor.pow(TWO_FINGER_ZOOM_SENSITIVITY))
            if (boundCompareLink == null) drawFrame()
        }
    }

    private fun applyPan(dxPixels: Float, dyPixels: Float) {
        if (dxPixels == 0f && dyPixels == 0f) return
        queueRenderEvent {
            avatarRenderer.panBy(dxPixels, dyPixels)
            if (boundCompareLink == null) drawFrame()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private data class EglConfigProfile(
        val name: String,
        val redBits: Int,
        val greenBits: Int,
        val blueBits: Int,
        val alphaBits: Int,
        val depthBits: Int,
        val samples: Int,
    )
}

internal data class ObjMesh(
    val vertexBuffer: FloatBuffer,
    val vertexCount: Int,
    val wireVertexBuffer: FloatBuffer,
    val wireVertexCount: Int,
    val bounds: ModelBounds,
    /** Anchors + mesh cross-sections from OBJ (see [buildMeasurementGuide]). */
    val measurementGuide: MetricAvatarMeasurementGuide,
    val colorBuffer: FloatBuffer? = null,
)

private data class AvatarFrame(
    val yawDeg: Float,
    val pitchDeg: Float,
    val distanceScale: Float,
    val translateX: Float,
    val translateY: Float,
    /** Camera eye height vs the part center: >0 looks down from above, 0 is level, <0 from below. */
    val eyeHeight: Float,
)

private fun AvatarFrame.lerpTo(target: AvatarFrame, fraction: Float): AvatarFrame =
    AvatarFrame(
        yawDeg = lerpAngleDegrees(yawDeg, target.yawDeg, fraction),
        pitchDeg = lerp(pitchDeg, target.pitchDeg, fraction),
        distanceScale = lerp(distanceScale, target.distanceScale, fraction),
        translateX = lerp(translateX, target.translateX, fraction),
        translateY = lerp(translateY, target.translateY, fraction),
        eyeHeight = lerp(eyeHeight, target.eyeHeight, fraction),
    )

private fun lerpAngleDegrees(start: Float, stop: Float, fraction: Float): Float {
    var delta = (stop - start) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return start + delta * fraction
}

private fun easeInOutSine(fraction: Float): Float =
    ((1f - cos(fraction.coerceIn(0f, 1f) * Math.PI.toFloat())) / 2f)

internal data class ModelBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float,
) {
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
    val centerZ: Float get() = (minZ + maxZ) / 2f
    val spanX: Float get() = maxX - minX
    val spanY: Float get() = maxY - minY
    val spanZ: Float get() = maxZ - minZ

    fun padded(x: Float, y: Float, z: Float): ModelBounds = copy(
        minX = minX - x,
        maxX = maxX + x,
        minY = minY - y,
        maxY = maxY + y,
        minZ = minZ - z,
        maxZ = maxZ + z,
    )

    /** Extends the bounds downward (−Y) so content below the region — e.g. feet — is framed in. */
    fun extendedDown(by: Float): ModelBounds = copy(minY = minY - by)

    fun including(other: ModelBounds): ModelBounds = copy(
        minX = minOf(minX, other.minX),
        maxX = maxOf(maxX, other.maxX),
        minY = minOf(minY, other.minY),
        maxY = maxOf(maxY, other.maxY),
        minZ = minOf(minZ, other.minZ),
        maxZ = maxOf(maxZ, other.maxZ),
    )
}

private object MetricAvatarFrameSolver {
    private data class RegionCameraPosition(
        val yaw: Float,
        val targetSpan: Float,
        val eyeHeight: Float = MetricAvatarCamera.EYE_HEIGHT,
        val extraPanX: Float = 0f,
        val extraPanY: Float = 0f,
    )

    private data class RegionBoundsPadding(
        val x: Float,
        val y: Float,
        val z: Float,
    )

    /**
     * Per-region camera tuning happens in [buildFrames]. The knobs per `frame(...)` call:
     *  - `yaw`        — how far the model is turned (degrees).
     *  - `targetSpan` — zoom: `distanceScale = visibleSpan / targetSpan`, so a *larger*
     *                   targetSpan ⇒ closer camera ⇒ the part fills more of the frame.
     *  - `extraPanY`  — nudges the part up (+) / down (−) on screen.
     *  - `extraPanX`  — nudges the part right (+) / left (−) on screen.
     */
    private const val MIN_DISTANCE_SCALE = 0.28f
    private const val MAX_DISTANCE_SCALE = 1.12f
    private const val FULL_BODY_DISTANCE_SCALE = 1.56f

    /** Horizontal offset (at `distanceScale` 1) that keeps the part clear of the part list. */
    private const val RIGHT_WINDOW_CENTER_X = 0.46f
    private const val FULL_BODY_RIGHT_EDGE_X = 1.42f
    private const val FRAME_CENTER_Y = -0.02f
    private const val DEGREES_TO_RADIANS = (Math.PI / 180.0).toFloat()

    private val cameraPositions = mapOf(
        BodyMeasurementRegion.Neck to RegionCameraPosition(
            yaw = 42f,
            targetSpan = 2.0f,
            eyeHeight = 0.05f,
        ),
        BodyMeasurementRegion.Shoulders to RegionCameraPosition(
            yaw = 78f,
            targetSpan = 2.9f,
        ),
        BodyMeasurementRegion.Chest to RegionCameraPosition(
            yaw = 38f,
            targetSpan = 2.55f,
        ),
        BodyMeasurementRegion.Forearm to RegionCameraPosition(
            yaw = 70f,
            targetSpan = 2.7f,
            extraPanX = 0.18f,
            extraPanY = -0.18f,
        ),
        BodyMeasurementRegion.Bicep to RegionCameraPosition(
            yaw = 70f,
            targetSpan = 2.7f,
        ),
        BodyMeasurementRegion.UpperWaist to RegionCameraPosition(
            yaw = 18f,
            targetSpan = 2.5f,
        ),
        BodyMeasurementRegion.Waist to RegionCameraPosition(
            yaw = 18f,
            targetSpan = 2.5f,
        ),
        BodyMeasurementRegion.LowerWaist to RegionCameraPosition(
            yaw = 18f,
            targetSpan = 2.5f,
            extraPanY = -0.08f,
        ),
        // Glutes are on the back, so this view turns the model to a 3/4 rear angle.
        BodyMeasurementRegion.HipsGlutes to RegionCameraPosition(
            yaw = -165f,
            targetSpan = 1.65f,
        ),
        BodyMeasurementRegion.Thigh to RegionCameraPosition(
            yaw = 8f,
            targetSpan = 1.85f,
            eyeHeight = 0.15f,
        ),
        BodyMeasurementRegion.Calf to RegionCameraPosition(
            yaw = 8f,
            targetSpan = 1.85f,
            eyeHeight = 0.0f,
        ),
        BodyMeasurementRegion.Ankle to RegionCameraPosition(
            yaw = 60f,
            targetSpan = 1.35f,
            eyeHeight = 0.0f,
            extraPanY = -0.22f,
        ),
    )

    private val boundsPadding = mapOf(
        BodyMeasurementRegion.Neck to RegionBoundsPadding(0.20f, 0.20f, 0.18f),
        BodyMeasurementRegion.Shoulders to RegionBoundsPadding(0.22f, 0.20f, 0.18f),
        BodyMeasurementRegion.Chest to RegionBoundsPadding(0.20f, 0.22f, 0.18f),
        BodyMeasurementRegion.Forearm to RegionBoundsPadding(0.20f, 0.28f, 0.18f),
        BodyMeasurementRegion.Bicep to RegionBoundsPadding(0.22f, 0.25f, 0.18f),
        BodyMeasurementRegion.UpperWaist to RegionBoundsPadding(0.18f, 0.22f, 0.18f),
        BodyMeasurementRegion.Waist to RegionBoundsPadding(0.18f, 0.22f, 0.18f),
        BodyMeasurementRegion.LowerWaist to RegionBoundsPadding(0.18f, 0.22f, 0.18f),
        BodyMeasurementRegion.Thigh to RegionBoundsPadding(0.22f, 0.28f, 0.18f),
        BodyMeasurementRegion.Calf to RegionBoundsPadding(0.20f, 0.30f, 0.18f),
    )

    fun buildFrames(
        guide: MetricAvatarMeasurementGuide,
        fullBodyBounds: ModelBounds,
        centered: Boolean = false,
    ): Map<BodyMeasurementRegion, AvatarFrame> {
        // Visual frames push the figure to the right to clear the left selector ruler;
        // centered framing (Compare) keeps the figure horizontally centered instead.
        val windowCenterX = if (centered) 0f else RIGHT_WINDOW_CENTER_X
        fun frame(
            region: BodyMeasurementRegion,
            yaw: Float,
            bounds: ModelBounds,
            targetSpan: Float,
            extraPanX: Float = 0f,
            extraPanY: Float = 0f,
            eyeHeight: Float = MetricAvatarCamera.EYE_HEIGHT,
        ): Pair<BodyMeasurementRegion, AvatarFrame> {
            // The camera sees the part turned by `yaw`, so its on-screen width is the X-Z
            // footprint projected through that turn; height (Y) is unaffected by yaw.
            val yawRad = yaw * DEGREES_TO_RADIANS
            val visibleWidth =
                bounds.spanX * abs(cos(yawRad)) + bounds.spanZ * abs(sin(yawRad))
            val visibleSpan = maxOf(visibleWidth, bounds.spanY, 0.001f)
            val distanceScale =
                (visibleSpan / targetSpan).coerceIn(MIN_DISTANCE_SCALE, MAX_DISTANCE_SCALE)

            // Put the part's center on the camera axis (accounting for the same yaw+pitch
            // the mesh is drawn with), then offset within the frame. The offset scales with
            // distanceScale so every part lands at the same on-screen position regardless
            // of how far the camera pulled back.
            val (rotatedCenterX, rotatedCenterY) =
                rotatedCenter(bounds, yaw, INITIAL_PITCH_DEG)
            return region to AvatarFrame(
                yawDeg = yaw,
                pitchDeg = INITIAL_PITCH_DEG,
                distanceScale = distanceScale,
                translateX = (windowCenterX + extraPanX) * distanceScale - rotatedCenterX,
                translateY = (FRAME_CENTER_Y + extraPanY) * distanceScale - rotatedCenterY,
                eyeHeight = eyeHeight,
            )
        }

        val frames = LinkedHashMap<BodyMeasurementRegion, AvatarFrame>()
        frames[BodyMeasurementRegion.FullBody] = AvatarFrame(
            yawDeg = 0f,
            pitchDeg = INITIAL_PITCH_DEG,
            distanceScale = FULL_BODY_DISTANCE_SCALE,
            translateX = if (centered) -fullBodyBounds.centerX
            else FULL_BODY_RIGHT_EDGE_X - fullBodyBounds.maxX,
            translateY = FRAME_CENTER_Y - fullBodyBounds.centerY,
            eyeHeight = MetricAvatarCamera.EYE_HEIGHT,
        )

        BodyMeasurementRegion.measurableRegions.forEach { region ->
            val position = cameraPositions[region] ?: return@forEach
            val bounds = cameraBoundsFor(guide, region) ?: return@forEach
            frames += frame(
                region = region,
                yaw = position.yaw,
                bounds = bounds,
                targetSpan = position.targetSpan,
                extraPanX = position.extraPanX,
                extraPanY = position.extraPanY,
                eyeHeight = position.eyeHeight,
            )
        }

        return frames
    }

    private fun cameraBoundsFor(
        guide: MetricAvatarMeasurementGuide,
        region: BodyMeasurementRegion,
    ): ModelBounds? {
        val bounds = when (region) {
            BodyMeasurementRegion.FullBody -> null
            BodyMeasurementRegion.LowerWaist -> lowerWaistCameraBounds(guide)
            BodyMeasurementRegion.HipsGlutes -> manualBounds(
                -0.46f,
                0.46f,
                -0.08f,
                0.24f,
                -0.18f,
                0.18f
            )

            BodyMeasurementRegion.Ankle -> manualBounds(
                -0.36f,
                0.36f,
                -1.30f,
                -0.82f,
                -0.16f,
                0.16f
            )
                .extendedDown(0.45f)

            BodyMeasurementRegion.Forearm -> boundsFor(guide, region)?.extendedDown(0.32f)
            BodyMeasurementRegion.Calf -> boundsFor(guide, region)?.extendedDown(0.50f)
            else -> boundsFor(guide, region)
        } ?: return null

        val padding = boundsPadding[region] ?: return bounds
        return bounds.padded(padding.x, padding.y, padding.z)
    }

    private fun boundsFor(
        guide: MetricAvatarMeasurementGuide,
        region: BodyMeasurementRegion,
    ): ModelBounds? {
        val key = region.measurementGuideKey ?: return null
        val packed = guide.crossSectionPolylines[key]
        val opposite = guide.crossSectionPolylinesOpposite[key]
        val points = ArrayList<FloatArray>()
        appendPacked(points, packed)
        appendPacked(points, opposite)
        guide.anchorPoints[key]?.let { points += it }
        MeasurementVisualAnchors.fallbackAnchorPosition(key)?.let { points += it }
        return boundsOf(points)
    }

    private fun appendPacked(into: MutableList<FloatArray>, packed: FloatArray?) {
        if (packed == null) return
        var i = 0
        var kept = 0
        val stride = maxOf(1, packed.size / (3 * 32))
        while (i + 2 < packed.size) {
            if (kept % stride == 0) {
                into += floatArrayOf(packed[i], packed[i + 1], packed[i + 2])
            }
            kept++
            i += 3
        }
    }

    private fun boundsOf(points: List<FloatArray>): ModelBounds? {
        if (points.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        points.forEach { p ->
            minX = minOf(minX, p[0]); maxX = maxOf(maxX, p[0])
            minY = minOf(minY, p[1]); maxY = maxOf(maxY, p[1])
            minZ = minOf(minZ, p[2]); maxZ = maxOf(maxZ, p[2])
        }
        return ModelBounds(minX, maxX, minY, maxY, minZ, maxZ)
    }

    private fun manualBounds(
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float,
        minZ: Float,
        maxZ: Float,
    ) = ModelBounds(minX, maxX, minY, maxY, minZ, maxZ)

    private fun lowerWaistCameraBounds(guide: MetricAvatarMeasurementGuide): ModelBounds {
        val fallback = manualBounds(-0.42f, 0.42f, -0.08f, 0.16f, -0.18f, 0.18f)
        return boundsFor(guide, BodyMeasurementRegion.LowerWaist)?.including(fallback) ?: fallback
    }

    /**
     * A region's bounds center after the avatar's yaw (about +Y) then pitch (about +X)
     * rotation — the same order [MetricAvatarRenderer.updateAvatarMatrices] draws with.
     * Returns the on-screen-relevant (x, y); the part is centred by translating by its negation.
     */
    private fun rotatedCenter(
        bounds: ModelBounds,
        yawDeg: Float,
        pitchDeg: Float,
    ): Pair<Float, Float> {
        val yaw = yawDeg * DEGREES_TO_RADIANS
        val pitch = pitchDeg * DEGREES_TO_RADIANS
        val cx = bounds.centerX
        val cy = bounds.centerY
        val cz = bounds.centerZ
        val xAfterYaw = cx * cos(yaw) + cz * sin(yaw)
        val zAfterYaw = -cx * sin(yaw) + cz * cos(yaw)
        val yAfterPitch = cy * cos(pitch) - zAfterYaw * sin(pitch)
        return xAfterYaw to yAfterPitch
    }

    /**
     * Avatar-guide cross-section key for a region — the canonical mapping lives in
     * [BodyMeasurementKeys]. Null where the frame solver falls back to [manualBounds]
     * (full body, hips/glutes, ankle).
     */
    private val BodyMeasurementRegion.measurementGuideKey: String?
        get() = when (this) {
            BodyMeasurementRegion.HipsGlutes, BodyMeasurementRegion.Ankle -> null
            else -> BodyMeasurementKeys.visualAnchorKey(this)
        }
}

private object ObjParser {
    private const val NORMALIZED_TARGET_SIZE = 3.18f
    private const val FLOATS_PER_VERTEX = 9
    private const val POSITION_FLOATS_PER_VERTEX = 3
    private const val BYTES_PER_FLOAT = 4
    private const val VERTEX_MERGE_RADIUS = 0.012f
    private const val TRIANGLE_KEY_BITS = 21
    private const val TRIANGLE_KEY_MASK = (1L shl TRIANGLE_KEY_BITS) - 1L
    private const val TRIANGLE_KEY_MAX_INDEX = (1 shl TRIANGLE_KEY_BITS) - 1
    private const val GRID_KEY_BITS = 21
    private const val GRID_KEY_MASK = (1L shl GRID_KEY_BITS) - 1L
    private const val LONG_HASH_SET_MAX_LOAD_PERCENT = 70

    private const val OBJ_URL_CONNECT_TIMEOUT_MS = 10_000
    private const val OBJ_URL_READ_TIMEOUT_MS = 15_000

    private const val OBJ_DISK_CACHE_DIR = "metric_avatar_obj"
    private const val OBJ_DISK_CACHE_MAX_FILES = 16
    private const val OBJ_DISK_CACHE_MAX_BYTES = 64L * 1024L * 1024L

    /** Minimum whitespace-separated tokens for a valid `v x y z` vertex line. */
    private const val OBJ_VERTEX_LINE_MIN_TOKENS = 4

    private const val OBJ_VERTEX_COLOR_LINE_MIN_TOKENS = 7

    private const val DEFAULT_VERTEX_GREY = 0.4f

    private const val MESH_CACHE_MAX_ENTRIES = 6

    private val meshCache = LruCache<String, ObjMesh>(MESH_CACHE_MAX_ENTRIES)

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

    fun parse(url: String, cacheDir: File, includeVertexColors: Boolean = false): ObjMesh {
        val cacheKey = if (includeVertexColors) "$url#colors" else url
        meshCache.get(cacheKey)?.let { return it.withIndependentBuffers() }

        val vertices = ArrayList<FloatArray>()
        val colors = if (includeVertexColors) ArrayList<FloatArray>() else null
        var hasColors = false
        val faces = ArrayList<IntArray>()

        objFileReader(url, cacheDir).use { reader ->
            reader.lineSequence().forEach { line ->
                when {
                    line.startsWith("v ") -> if (parseVertex(line, vertices, colors)) hasColors =
                        true

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
        val modelBounds = boundsOfUsedVertices(vertices, usedVertices)

        val measurementGuide = buildMeasurementGuide(vertices, usedVertices, faces)

        val clusterResult =
            clusterCloseVertices(vertices, usedVertices, measurementGuide.neckClipPlane)
        val clusterColors =
            colors?.takeIf { hasColors }?.let { buildClusterColors(it, clusterResult) }

        val triangleFloatEstimate = estimateTriangleFloatCount(faces)
        val wireFloatEstimate = estimateWireFloatCount(faces)
        val triangles = FloatAccumulator(triangleFloatEstimate)
        val wireLines = FloatAccumulator(wireFloatEstimate)
        val triangleColors = if (clusterColors != null) {
            FloatAccumulator(triangleFloatEstimate / FLOATS_PER_VERTEX * POSITION_FLOATS_PER_VERTEX)
        } else {
            null
        }
        val emittedTriangles = LongHashSet(faces.size)
        val emittedWireEdges = LongHashSet(faces.size * 3)

        faces.forEach { face ->
            appendClusteredFace(
                face,
                clusterResult,
                emittedTriangles,
                triangles,
                emittedWireEdges,
                wireLines,
                clusterColors,
                triangleColors,
            )
        }
        if (triangles.isEmpty()) throw IllegalArgumentException("OBJ contains no supported triangles")

        val buffer = triangles.toDirectFloatBuffer()
        val wireBuffer = wireLines.toDirectFloatBuffer()

        val mesh = ObjMesh(
            vertexBuffer = buffer,
            vertexCount = triangles.size / FLOATS_PER_VERTEX,
            wireVertexBuffer = wireBuffer,
            wireVertexCount = wireLines.size / POSITION_FLOATS_PER_VERTEX,
            bounds = modelBounds,
            measurementGuide = measurementGuide,
            colorBuffer = triangleColors?.toDirectFloatBuffer(),
        )
        meshCache.put(cacheKey, mesh)
        return mesh.withIndependentBuffers()
    }

    // Two previews can render one cached mesh without sharing buffer positions.
    private fun ObjMesh.withIndependentBuffers(): ObjMesh = copy(
        vertexBuffer = vertexBuffer.duplicate(),
        wireVertexBuffer = wireVertexBuffer.duplicate(),
        colorBuffer = colorBuffer?.duplicate(),
    )

    private fun objFileReader(url: String, cacheDir: File): BufferedReader {
        val dir = File(cacheDir, OBJ_DISK_CACHE_DIR).apply { mkdirs() }
        val cacheFile = File(dir, cacheFileName(url))
        if (!cacheFile.exists() || cacheFile.length() == 0L) {
            downloadObj(url, dir, cacheFile)
        }
        cacheFile.setLastModified(System.currentTimeMillis())
        val reader = cacheFile.bufferedReader()
        trimDiskCache(dir, cacheFile)
        return reader
    }

    @Synchronized
    private fun trimDiskCache(dir: File, activeFile: File) {
        val cachedFiles = dir.listFiles { file ->
            file.isFile && file.extension == "obj"
        }.orEmpty().sortedWith(
            compareByDescending<File> { it == activeFile }
                .thenByDescending { it.lastModified() },
        )
        var retainedFiles = 0
        var retainedBytes = 0L

        cachedFiles.forEach { file ->
            val fileSize = file.length()
            val retain = file == activeFile ||
                    (retainedFiles < OBJ_DISK_CACHE_MAX_FILES &&
                            retainedBytes + fileSize <= OBJ_DISK_CACHE_MAX_BYTES)
            if (retain) {
                retainedFiles += 1
                retainedBytes += fileSize
            } else {
                file.delete()
            }
        }
    }

    private fun downloadObj(url: String, dir: File, dest: File) {
        val connection = URL(url).openConnection().apply {
            connectTimeout = OBJ_URL_CONNECT_TIMEOUT_MS
            readTimeout = OBJ_URL_READ_TIMEOUT_MS
        }
        // Do not expose partial downloads to concurrent readers.
        val tmp = File.createTempFile("obj", ".tmp", dir)
        try {
            connection.getInputStream().use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
            }
        } finally {
            tmp.delete()
        }
    }

    private fun cacheFileName(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + ".obj"
    }

    private fun parseVertex(
        line: String,
        vertices: MutableList<FloatArray>,
        colors: MutableList<FloatArray>?,
    ): Boolean {
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < OBJ_VERTEX_LINE_MIN_TOKENS) return false
        vertices.add(
            floatArrayOf(
                parts[1].toFloatOrNull() ?: 0f,
                parts[2].toFloatOrNull() ?: 0f,
                parts[3].toFloatOrNull() ?: 0f,
            )
        )
        if (colors == null) return false
        return if (parts.size >= OBJ_VERTEX_COLOR_LINE_MIN_TOKENS) {
            colors.add(
                floatArrayOf(
                    parts[4].toFloatOrNull() ?: DEFAULT_VERTEX_GREY,
                    parts[5].toFloatOrNull() ?: DEFAULT_VERTEX_GREY,
                    parts[6].toFloatOrNull() ?: DEFAULT_VERTEX_GREY,
                )
            )
            true
        } else {
            colors.add(floatArrayOf(DEFAULT_VERTEX_GREY, DEFAULT_VERTEX_GREY, DEFAULT_VERTEX_GREY))
            false
        }
    }

    private fun buildClusterColors(
        colors: List<FloatArray>,
        clusterResult: ClusterResult,
    ): FloatArray {
        val out = FloatArray(clusterResult.vertices.size * POSITION_FLOATS_PER_VERTEX)
        val assigned = BooleanArray(clusterResult.vertices.size)
        for (originalIndex in colors.indices) {
            val clusterIndex = clusterResult.remap.getOrElse(originalIndex) { -1 }
            if (clusterIndex in 0 until clusterResult.vertices.size && !assigned[clusterIndex]) {
                val color = colors[originalIndex]
                val base = clusterIndex * POSITION_FLOATS_PER_VERTEX
                out[base] = color[0]
                out[base + 1] = color[1]
                out[base + 2] = color[2]
                assigned[clusterIndex] = true
            }
        }
        return out
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
        emittedTriangles: LongHashSet,
        triangles: FloatAccumulator,
        emittedWireEdges: LongHashSet,
        wireLines: FloatAccumulator,
        clusterColors: FloatArray?,
        triangleColors: FloatAccumulator?,
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

            if (triangleColors != null && clusterColors != null) {
                appendVertexColor(triangleColors, clusterColors, c0)
                appendVertexColor(triangleColors, clusterColors, c1)
                appendVertexColor(triangleColors, clusterColors, c2)
            }

            if (mz > 0f) appendWireEdge(wireLines, emittedWireEdges, c0, c1, v0, v1)
            if (mx > 0f) appendWireEdge(wireLines, emittedWireEdges, c1, c2, v1, v2)
            if (my > 0f) appendWireEdge(wireLines, emittedWireEdges, c2, c0, v2, v0)
        }
    }

    private fun appendWireEdge(
        into: FloatAccumulator,
        emittedEdges: LongHashSet,
        aIndex: Int,
        bIndex: Int,
        a: FloatArray,
        b: FloatArray,
    ) {
        if (!emittedEdges.add(edgeKey(aIndex, bIndex))) return
        appendPosition(into, a)
        appendPosition(into, b)
    }

    private fun clusterCloseVertices(
        vertices: List<FloatArray>,
        usedVertices: BooleanArray,
        neckClipPlane: FloatArray,
    ): ClusterResult {
        val clusters = ArrayList<Cluster>()
        val remap = IntArray(vertices.size) { -1 }
        val grid = HashMap<Long, MutableList<Int>>()
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
        val hands = abs(x) >= PROTECTED_REGION_HANDS_ABS_X &&
                y >= PROTECTED_REGION_HANDS_MIN_Y && y <= PROTECTED_REGION_HANDS_MAX_Y
        val feet = y <= PROTECTED_REGION_FEET_MAX_Y
        return towardHead || hands || feet
    }

    private fun addVertexToCluster(
        clusterIndex: Int,
        vertex: FloatArray,
        clusters: MutableList<Cluster>,
        grid: MutableMap<Long, MutableList<Int>>,
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
        into: FloatAccumulator,
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

    private fun appendPosition(into: FloatAccumulator, v: FloatArray) {
        into.add(v[0]); into.add(v[1]); into.add(v[2])
    }

    private fun appendVertexColor(
        into: FloatAccumulator,
        clusterColors: FloatArray,
        clusterIndex: Int
    ) {
        val base = clusterIndex * POSITION_FLOATS_PER_VERTEX
        into.add(clusterColors[base]); into.add(clusterColors[base + 1]); into.add(clusterColors[base + 2])
    }

    private fun estimateTriangleFloatCount(faces: List<IntArray>): Int {
        var count = 0
        faces.forEach { face ->
            count += (face.size - 2).coerceAtLeast(0) * FLOATS_PER_VERTEX * 3
        }
        return count
    }

    private fun estimateWireFloatCount(faces: List<IntArray>): Int {
        var count = 0
        faces.forEach { face ->
            count += (face.size - 2).coerceAtLeast(0) * POSITION_FLOATS_PER_VERTEX * 2 * 3
        }
        return count
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

    private fun boundsOfUsedVertices(
        vertices: List<FloatArray>,
        usedVertices: BooleanArray,
    ): ModelBounds {
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

        return ModelBounds(minX, maxX, minY, maxY, minZ, maxZ)
    }

    private fun cellOf(value: Float): Int =
        kotlin.math.floor((value / VERTEX_MERGE_RADIUS).toDouble()).toInt()

    private fun gridKey(x: Int, y: Int, z: Int): Long {
        val packedX = packGridCell(x)
        val packedY = packGridCell(y)
        val packedZ = packGridCell(z)
        return (packedX shl (GRID_KEY_BITS * 2)) or
                (packedY shl GRID_KEY_BITS) or
                packedZ
    }

    private fun packGridCell(value: Int): Long {
        val longValue = value.toLong()
        val packed = (longValue shl 1) xor (longValue shr (Long.SIZE_BITS - 1))
        check(packed in 0..GRID_KEY_MASK) { "OBJ grid cell is outside packable range" }
        return packed
    }

    private fun triangleKey(a: Int, b: Int, c: Int): Long {
        check(a <= TRIANGLE_KEY_MAX_INDEX && b <= TRIANGLE_KEY_MAX_INDEX && c <= TRIANGLE_KEY_MAX_INDEX) {
            "OBJ has too many clustered vertices to pack triangle keys"
        }
        val x = minOf(a, b, c)
        val z = maxOf(a, b, c)
        val y = a + b + c - x - z
        return (x.toLong() shl (TRIANGLE_KEY_BITS * 2)) or
                (y.toLong() shl TRIANGLE_KEY_BITS) or
                (z.toLong() and TRIANGLE_KEY_MASK)
    }

    private fun edgeKey(a: Int, b: Int): Long {
        val x = minOf(a, b)
        val y = maxOf(a, b)
        return (x.toLong() shl Int.SIZE_BITS) or (y.toLong() and 0xFFFF_FFFFL)
    }

    private class FloatAccumulator(initialCapacity: Int = 0) {
        private var values = FloatArray(initialCapacity.coerceAtLeast(16))
        var size: Int = 0
            private set

        fun isEmpty(): Boolean = size == 0

        fun add(value: Float) {
            ensureCapacity(size + 1)
            values[size] = value
            size += 1
        }

        fun toDirectFloatBuffer(): FloatBuffer =
            ByteBuffer.allocateDirect(size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(values, 0, size)
                    position(0)
                }

        private fun ensureCapacity(required: Int) {
            if (required <= values.size) return
            var newCapacity = values.size
            while (newCapacity < required) {
                newCapacity *= 2
            }
            values = values.copyOf(newCapacity)
        }
    }

    private class LongHashSet(expectedSize: Int = 0) {
        private var keys = LongArray(capacityFor(expectedSize)) { EMPTY }
        private var resizeThreshold = thresholdFor(keys.size)
        private var size = 0

        fun add(key: Long): Boolean {
            check(key != EMPTY) { "LongHashSet key collides with empty sentinel" }
            if (size + 1 > resizeThreshold) resize()
            return insert(key)
        }

        private fun insert(key: Long): Boolean {
            var index = smear(key) and (keys.size - 1)
            while (true) {
                val current = keys[index]
                when (current) {
                    EMPTY -> {
                        keys[index] = key
                        size += 1
                        return true
                    }

                    key -> return false
                    else -> index = (index + 1) and (keys.size - 1)
                }
            }
        }

        private fun resize() {
            val oldKeys = keys
            keys = LongArray(oldKeys.size * 2) { EMPTY }
            resizeThreshold = thresholdFor(keys.size)
            size = 0
            oldKeys.forEach { key ->
                if (key != EMPTY) insert(key)
            }
        }

        private companion object {
            private const val EMPTY = Long.MIN_VALUE

            private fun capacityFor(expectedSize: Int): Int {
                val needed =
                    ((expectedSize.coerceAtLeast(1) * 100) / LONG_HASH_SET_MAX_LOAD_PERCENT) + 1
                var capacity = 16
                while (capacity < needed) {
                    capacity *= 2
                }
                return capacity
            }

            private fun thresholdFor(capacity: Int): Int =
                (capacity * LONG_HASH_SET_MAX_LOAD_PERCENT) / 100

            private fun smear(value: Long): Int {
                var x = value
                x = x xor (x ushr 33)
                x *= -49064778989728563L
                x = x xor (x ushr 33)
                x *= -4265267296055464877L
                x = x xor (x ushr 33)
                return x.toInt()
            }
        }
    }
}

private class MetricAvatarRenderer {
    private var mesh: ObjMesh? = null
    private var program = 0
    private var aPosition = 0
    private var aBary = 0
    private var aEdgeMask = 0
    private var aColor = 0
    private var uMvp = 0
    private var uSkinColor = 0
    private var uSuitColor = 0
    private var uShowSkin = 0
    private var uModelView = 0
    private var uMeshColor = 0
    private var uUseVertexColor = 0
    private var uMeshGlow = 0
    private var meshGlow = 0f
    private var showSkinAreas = false

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

    /** GL clear / gradient uniforms — loaded from [@color/metric_avatar_preview_gradient_*] via [setPreviewGradientFromResources]. */
    private var previewGradientInnerR = 254f / 255f
    private var previewGradientInnerG = 254f / 255f
    private var previewGradientInnerB = 254f / 255f
    private var previewGradientOuterR = 195f / 255f
    private var previewGradientOuterG = 192f / 255f
    private var previewGradientOuterB = 197f / 255f

    private var drawBackground = true
    private var rotationLink: CompareRotationLink? = null

    private val projection = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    private val view = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    private val model = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    private val temp = FloatArray(MetricAvatarCamera.MATRIX_SIZE)
    private val mvp = FloatArray(MetricAvatarCamera.MATRIX_SIZE)

    private var yaw = 0f
    private var pitch = INITIAL_PITCH_DEG
    private var viewDistance = MetricAvatarCamera.INITIAL_VIEW_DISTANCE
    private var framingRegion: BodyMeasurementRegion? = null

    /** When true, framing keeps the figure horizontally centered (Compare) instead of right-shifted (Visual). */
    private var centerFraming = false

    /** User drag-rotation applied on top of the active frame's yaw; reset when the part changes. */
    private var framingYawOffsetDeg = 0f

    private var userZoom = MetricAvatarCamera.MIN_USER_ZOOM

    private var baseDistanceScale = 1f

    private var userPanX = 0f
    private var userPanY = 0f
    private var frameCache: Map<BodyMeasurementRegion, AvatarFrame> = emptyMap()
    private var animatedFrame: AvatarFrame? = null
    private var frameAnimationStart: AvatarFrame? = null
    private var frameAnimationTarget: AvatarFrame? = null
    private var frameAnimationStartTimeMs = 0L

    private var viewportWidth = 0
    private var viewportHeight = 0
    private var visualTransformListener: ((Float, Float, Int, Int) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastPostedYaw = Float.NaN
    private var lastPostedPitch = Float.NaN

    fun setPreviewGradientFromResources(context: Context) {
        val inner = ContextCompat.getColor(context, R.color.metric_avatar_preview_gradient_inner)
        val outer = ContextCompat.getColor(context, R.color.metric_avatar_preview_gradient_outer)
        previewGradientInnerR = android.graphics.Color.red(inner) / 255f
        previewGradientInnerG = android.graphics.Color.green(inner) / 255f
        previewGradientInnerB = android.graphics.Color.blue(inner) / 255f
        previewGradientOuterR = android.graphics.Color.red(outer) / 255f
        previewGradientOuterG = android.graphics.Color.green(outer) / 255f
        previewGradientOuterB = android.graphics.Color.blue(outer) / 255f
    }

    fun setVisualTransformListener(listener: ((Float, Float, Int, Int) -> Unit)?) {
        visualTransformListener = listener
        if (listener == null) {
            lastPostedYaw = Float.NaN
            lastPostedPitch = Float.NaN
        } else {
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
            abs(y - lastPostedYaw) < VISUAL_TRANSFORM_POST_MIN_DELTA &&
            abs(p - lastPostedPitch) < VISUAL_TRANSFORM_POST_MIN_DELTA
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

    fun setDrawBackground(draw: Boolean) {
        drawBackground = draw
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
                previewGradientInnerR,
                previewGradientInnerG,
                previewGradientInnerB,
                PREVIEW_CLEAR_ALPHA,
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
            previewGradientInnerR,
            previewGradientInnerG,
            previewGradientInnerB,
        )
        GLES20.glUniform3f(
            gradientLocOuter,
            previewGradientOuterR,
            previewGradientOuterG,
            previewGradientOuterB,
        )

        buf.position(0)
        GLES20.glVertexAttribPointer(gradientAClip, 2, GLES20.GL_FLOAT, false, 0, buf)
        GLES20.glEnableVertexAttribArray(gradientAClip)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        GLES20.glDisableVertexAttribArray(gradientAClip)

        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun drawLeaderOverlay() {
        val prog = leaderProgram
        if (prog == 0) return
        val lines = leaderLineBuffer ?: return

        GLES20.glUseProgram(prog)
        GLES20.glUniformMatrix4fv(leaderUMvp, 1, false, mvp, 0)
        GLES20.glUniform4f(leaderUColor, LEADER_LINE_R, LEADER_LINE_G, LEADER_LINE_B, LEADER_LINE_A)

        GLES20.glDepthMask(false)

        lines.position(0)
        GLES20.glVertexAttribPointer(leaderAPos, 3, GLES20.GL_FLOAT, false, 0, lines)
        GLES20.glEnableVertexAttribArray(leaderAPos)
        GLES20.glLineWidth(LEADER_LINE_WIDTH_PX)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, leaderLineVertCount)

        GLES20.glDisableVertexAttribArray(leaderAPos)
        GLES20.glDepthMask(true)
    }

    private fun drawMeshWireOverlay(m: ObjMesh) {
        val prog = leaderProgram
        if (prog == 0 || leaderAPos < 0 || m.wireVertexCount <= 0) return

        GLES20.glUseProgram(prog)
        GLES20.glUniformMatrix4fv(leaderUMvp, 1, false, mvp, 0)
        GLES20.glUniform4f(leaderUColor, MESH_R, MESH_G, MESH_B, MESH_WIRE_ALPHA)

        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        m.wireVertexBuffer.position(0)
        GLES20.glVertexAttribPointer(leaderAPos, 3, GLES20.GL_FLOAT, false, 0, m.wireVertexBuffer)
        GLES20.glEnableVertexAttribArray(leaderAPos)
        GLES20.glLineWidth(MESH_WIRE_LINE_WIDTH_PX)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, m.wireVertexCount)

        GLES20.glDisableVertexAttribArray(leaderAPos)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthFunc(GLES20.GL_LESS)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(true)
    }

    fun setRotationLink(link: CompareRotationLink?) {
        rotationLink = link
    }

    fun setMesh(value: ObjMesh) {
        mesh = value
        resetUserView()
        rebuildFrameCache()
    }

    private fun resetUserView() {
        userZoom = MetricAvatarCamera.MIN_USER_ZOOM
        userPanX = 0f
        userPanY = 0f
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

    fun setFramingRegion(region: BodyMeasurementRegion?) {
        if (framingRegion == region) return

        val startFrame = currentAvatarFrame()
        framingRegion = region
        framingYawOffsetDeg = 0f
        resetUserView()
        val targetFrame = targetAvatarFrame()
        if (startFrame == null || targetFrame == null) {
            rotationLink?.resetAdjustmentsImmediate()
            animatedFrame = targetFrame
            frameAnimationStart = null
            frameAnimationTarget = null
            frameAnimationStartTimeMs = 0L
            return
        }

        rotationLink?.beginResetAnimation()
        animatedFrame = startFrame
        frameAnimationStart = startFrame
        frameAnimationTarget = targetFrame
        frameAnimationStartTimeMs = SystemClock.uptimeMillis()
    }

    fun setCenterFraming(enabled: Boolean) {
        if (centerFraming == enabled) return
        centerFraming = enabled
        rebuildFrameCache()
    }

    fun setShowSkinAreas(show: Boolean) {
        showSkinAreas = show
    }

    fun setMeshGlow(glow: Float) {
        meshGlow = glow.coerceIn(0f, 1f)
    }

    fun rotateBy(dyaw: Float, dpitch: Float) {
        val link = rotationLink
        when {
            link != null -> link.applyRotationDelta(dyaw, dpitch)
            framingRegion != null -> framingYawOffsetDeg += dyaw
            else -> {
                yaw += dyaw
                pitch = (pitch + dpitch).coerceIn(MIN_PITCH_DEG, MAX_PITCH_DEG)
            }
        }
    }

    fun setBaseDistanceScale(scale: Float) {
        baseDistanceScale = scale
    }

    fun zoomBy(factor: Float) {
        val link = rotationLink
        if (link != null) {
            link.applyZoomFactor(factor)
            val (maxX, maxY) = panBoundsWorld()
            link.clampPan(maxX, maxY)
        } else {
            userZoom = (userZoom * factor).coerceIn(
                MetricAvatarCamera.MIN_USER_ZOOM,
                MetricAvatarCamera.MAX_USER_ZOOM,
            )
            val (maxX, maxY) = panBoundsWorld()
            userPanX = userPanX.coerceIn(-maxX, maxX)
            userPanY = userPanY.coerceIn(-maxY, maxY)
        }
    }

    fun panBy(dxPixels: Float, dyPixels: Float) {
        if (viewportHeight <= 0) return
        val worldPerPixel =
            2f * currentDrawDistance() * MetricAvatarCamera.HALF_FOV_TAN / viewportHeight
        val dxWorld = dxPixels * worldPerPixel
        val dyWorld = -dyPixels * worldPerPixel
        val (maxX, maxY) = panBoundsWorld()
        val link = rotationLink
        if (link != null) {
            link.applyPanDelta(dxWorld, dyWorld, maxX, maxY)
        } else {
            userPanX = (userPanX + dxWorld).coerceIn(-maxX, maxX)
            userPanY = (userPanY + dyWorld).coerceIn(-maxY, maxY)
        }
    }

    private fun currentDrawDistance(): Float =
        viewDistance * (animatedFrame?.distanceScale ?: baseDistanceScale) / modelZoom()

    // Keep panning inside model bounds as zoom changes the visible area.
    private fun panBoundsWorld(): Pair<Float, Float> {
        if (viewportHeight <= 0) return 0f to 0f
        val bounds = mesh?.bounds ?: return 0f to 0f
        val keep = MetricAvatarCamera.PAN_EDGE_KEEP_FRACTION
        val halfWindowY = currentDrawDistance() * MetricAvatarCamera.HALF_FOV_TAN
        val halfWindowX = halfWindowY * (viewportWidth.toFloat() / viewportHeight)
        val maxX = ((bounds.maxX - bounds.minX) / 2f - halfWindowX * keep).coerceAtLeast(0f)
        val maxY = ((bounds.maxY - bounds.minY) / 2f - halfWindowY * keep).coerceAtLeast(0f)
        return maxX to maxY
    }

    private fun modelYaw(): Float {
        val link = rotationLink
        return if (link != null) link.displayYaw() else yaw
    }

    /**
     * Yaw spin applied on top of the active framing frame. Compare links rotate via the shared
     * link (so both models spin together); the unlinked Visual tab uses the local offset.
     */
    private fun framingSpinYaw(): Float {
        val link = rotationLink
        return if (link != null) link.displayYaw() else framingYawOffsetDeg
    }

    private fun modelPitch(): Float {
        val link = rotationLink
        return if (link != null) synchronized(link) { link.pitch } else pitch
    }

    private fun modelZoom(): Float {
        val link = rotationLink
        return if (link != null) link.displayZoom() else userZoom
    }

    private fun modelPanX(): Float {
        val link = rotationLink
        return if (link != null) link.displayPanX() else userPanX
    }

    private fun modelPanY(): Float {
        val link = rotationLink
        return if (link != null) link.displayPanY() else userPanY
    }

    private fun rebuildFrameCache() {
        val mesh = mesh ?: run {
            frameCache = emptyMap()
            animatedFrame = null
            frameAnimationStart = null
            frameAnimationTarget = null
            frameAnimationStartTimeMs = 0L
            return
        }
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            frameCache = emptyMap()
            return
        }

        frameCache = buildFrames(mesh.measurementGuide, mesh.bounds, centered = centerFraming)
        animatedFrame = targetAvatarFrame()
        frameAnimationStart = null
        frameAnimationTarget = null
        frameAnimationStartTimeMs = 0L
    }

    fun onSurfaceCreated() {
        GLES20.glClearColor(
            previewGradientInnerR,
            previewGradientInnerG,
            previewGradientInnerB,
            PREVIEW_CLEAR_ALPHA,
        )
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        // MSAA is requested via the EGL config (EGL_SAMPLES); OpenGL ES 2.0 has no
        // glEnable(GL_MULTISAMPLE) toggle — calling it would raise GL_INVALID_ENUM.

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
                CLIP_TRIANGLE_X0, CLIP_TRIANGLE_Y0,
                CLIP_TRIANGLE_X1, CLIP_TRIANGLE_Y1,
                CLIP_TRIANGLE_X2, CLIP_TRIANGLE_Y2,
            ),
        )
        clipBb.position(0)
        fullscreenClipBuffer = clipBb

        program = createProgram()
        if (program == 0) return

        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aBary = GLES20.glGetAttribLocation(program, "aBary")
        aEdgeMask = GLES20.glGetAttribLocation(program, "aEdgeMask")
        aColor = GLES20.glGetAttribLocation(program, "aColor")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
        uUseVertexColor = GLES20.glGetUniformLocation(program, "uUseVertexColor")
        uMeshGlow = GLES20.glGetUniformLocation(program, "uMeshGlow")
        uSkinColor = GLES20.glGetUniformLocation(program, "uSkinColor")
        uSuitColor = GLES20.glGetUniformLocation(program, "uSuitColor")
        uShowSkin = GLES20.glGetUniformLocation(program, "uShowSkin")
        uModelView = GLES20.glGetUniformLocation(program, "uModelView")
        uMeshColor = GLES20.glGetUniformLocation(program, "uMeshColor")
        leaderProgram = createLeaderProgram()
        if (leaderProgram != 0) {
            leaderAPos = GLES20.glGetAttribLocation(leaderProgram, "aPos")
            leaderUMvp = GLES20.glGetUniformLocation(leaderProgram, "uMvp")
            leaderUColor = GLES20.glGetUniformLocation(leaderProgram, "uColor")
        }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
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
        rebuildFrameCache()
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

    fun onDrawFrame() {
        drawFrameBackground()
        val m = mesh ?: return
        if (program == 0) return

        GLES20.glUseProgram(program)
        updateAvatarMatrices(currentAvatarFrame())
        bindAvatarUniforms()
        drawAvatarBody(m)
        drawRepairWireOverlay(m)
        drawLeaderOverlay()

        scheduleVisualTransformNotify(force = false)
    }

    private fun drawFrameBackground() {
        if (drawBackground) {
            drawPreviewBackgroundGradient()
        } else {
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
    }

    fun isFrameAnimationRunning(): Boolean =
        frameAnimationStart != null && frameAnimationTarget != null

    private fun targetAvatarFrame(): AvatarFrame? =
        framingRegion?.let { frameCache[it] }

    private fun currentAvatarFrame(): AvatarFrame? {
        val start = frameAnimationStart
        val target = frameAnimationTarget
        if (start == null || target == null) {
            return animatedFrame ?: targetAvatarFrame()
        }

        val elapsed = SystemClock.uptimeMillis() - frameAnimationStartTimeMs
        val linearProgress = elapsed.toFloat() / FRAME_ANIMATION_DURATION_MS
        if (linearProgress >= 1f) {
            animatedFrame = target
            frameAnimationStart = null
            frameAnimationTarget = null
            frameAnimationStartTimeMs = 0L
            return target
        }

        val easedProgress = easeInOutSine(linearProgress)
        return start.lerpTo(target, easedProgress).also { animatedFrame = it }
    }

    private fun updateAvatarMatrices(frame: AvatarFrame?) {
        val drawYaw = if (frame != null) frame.yawDeg + framingSpinYaw() else modelYaw()
        val drawPitch = frame?.pitchDeg ?: modelPitch()
        val drawDistance = viewDistance * (frame?.distanceScale ?: baseDistanceScale) / modelZoom()
        val drawTranslateX = (frame?.translateX ?: 0f) + modelPanX()
        val drawTranslateY = (frame?.translateY ?: 0f) + modelPanY()
        val drawEyeHeight = frame?.eyeHeight ?: MetricAvatarCamera.EYE_HEIGHT

        Matrix.setLookAtM(
            view, 0,
            0f, drawEyeHeight, drawDistance,
            0f, 0f, 0f,
            0f, 1f, 0f,
        )

        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, drawTranslateX, drawTranslateY, 0f)
        Matrix.rotateM(model, 0, drawPitch, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, drawYaw, 0f, 1f, 0f)

        Matrix.multiplyMM(temp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0)
    }

    private fun bindAvatarUniforms() {
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModelView, 1, false, temp, 0)
        GLES20.glUniform4f(uSkinColor, SKIN_R, SKIN_G, SKIN_B, 1f)
        GLES20.glUniform4f(uSuitColor, SUIT_R, SUIT_G, SUIT_B, 1f)
        GLES20.glUniform4f(uMeshColor, MESH_R, MESH_G, MESH_B, 1f)
        GLES20.glUniform1f(uShowSkin, if (showSkinAreas) 1f else 0f)
        if (uMeshGlow >= 0) GLES20.glUniform1f(uMeshGlow, meshGlow)
    }

    private fun drawAvatarBody(m: ObjMesh) {
        val colored = m.colorBuffer != null
        if (uUseVertexColor >= 0) GLES20.glUniform1f(uUseVertexColor, if (colored) 1f else 0f)
        bindAvatarVertexAttributes(m.vertexBuffer, m.colorBuffer)
        GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL)
        GLES20.glPolygonOffset(BODY_FILL_DEPTH_OFFSET_FACTOR, BODY_FILL_DEPTH_OFFSET_UNITS)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, m.vertexCount)
        GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL)
        unbindAvatarVertexAttributes()
    }

    private fun bindAvatarVertexAttributes(buf: FloatBuffer, colorBuf: FloatBuffer?) {
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

        if (aColor >= 0 && colorBuf != null) {
            colorBuf.position(0)
            GLES20.glVertexAttribPointer(
                aColor,
                FLOATS_PER_ATTRIB,
                GLES20.GL_FLOAT,
                false,
                0,
                colorBuf
            )
            GLES20.glEnableVertexAttribArray(aColor)
        }
    }

    private fun unbindAvatarVertexAttributes() {
        GLES20.glDisableVertexAttribArray(aPosition)
        if (aBary >= 0) GLES20.glDisableVertexAttribArray(aBary)
        if (aEdgeMask >= 0) GLES20.glDisableVertexAttribArray(aEdgeMask)
        if (aColor >= 0) GLES20.glDisableVertexAttribArray(aColor)
    }

    private fun drawRepairWireOverlay(m: ObjMesh) {
        // Low-alpha repair pass: shader barycentric lines own the look, while this fills
        // tiny per-triangle interpolation gaps without changing mesh geometry.
        drawMeshWireOverlay(m)
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
        private const val SKIN_R = 0.090f
        private const val SKIN_G = 0.871f
        private const val SKIN_B = 1f
        private const val SUIT_R = 0f
        private const val SUIT_G = 0.012f
        private const val SUIT_B = 0.018f
        private const val MESH_R = 0.090f
        private const val MESH_G = 0.871f
        private const val MESH_B = 1f
        private const val MESH_WIRE_ALPHA = 0.18f
        private const val MESH_WIRE_LINE_WIDTH_PX = 0.2f
        private const val BODY_FILL_DEPTH_OFFSET_FACTOR = 1f
        private const val BODY_FILL_DEPTH_OFFSET_UNITS = 1f

        /** XYZ components per vertex attribute in the packed interleaved buffer. */
        private const val FLOATS_PER_ATTRIB = 3

        /** Stride in bytes: position(3) + bary(3) + edge mask(3) floats × 4 bytes. */
        private const val FLOATS_PER_INTERLEAVED_VERTEX = 9
        private const val BYTES_PER_FLOAT_GL = 4
        private const val VERTEX_STRIDE_BYTES = FLOATS_PER_INTERLEAVED_VERTEX * BYTES_PER_FLOAT_GL

        private const val BARY_FLOAT_OFFSET = FLOATS_PER_ATTRIB
        private const val EDGE_MASK_FLOAT_OFFSET = FLOATS_PER_ATTRIB * 2

        private const val PREVIEW_CLEAR_ALPHA = 1f

        private const val LEADER_LINE_R = 1f
        private const val LEADER_LINE_G = 0.84f
        private const val LEADER_LINE_B = 0.15f
        private const val LEADER_LINE_A = 1f
        private const val LEADER_LINE_WIDTH_PX = 2f

        private const val VISUAL_TRANSFORM_POST_MIN_DELTA = 0.025f

        private const val CLIP_TRIANGLE_X0 = -1f
        private const val CLIP_TRIANGLE_Y0 = -1f
        private const val CLIP_TRIANGLE_X1 = 3f
        private const val CLIP_TRIANGLE_Y1 = -1f
        private const val CLIP_TRIANGLE_X2 = -1f
        private const val CLIP_TRIANGLE_Y2 = 3f

        /** Radial gradient matching [@color/metric_avatar_preview_gradient_inner] / _outer (Compose + GL). */
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

        const val VERTEX_SHADER = """
            attribute vec3 aPosition; attribute vec3 aBary; attribute vec3 aEdgeMask;
            attribute vec3 aColor;
            uniform mat4 uMvp; varying float vModelY; varying vec3 vModelPos;
            varying vec3 vBary; varying vec3 vEdgeMask; varying vec3 vColor;
            void main() {
                vModelY = aPosition.y; vModelPos = aPosition; vBary = aBary; vEdgeMask = aEdgeMask;
                vColor = aColor;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_standard_derivatives : enable
            #ifdef GL_FRAGMENT_PRECISION_HIGH
            precision highp float;
            #else
            precision mediump float;
            #endif
            varying float vModelY; varying vec3 vModelPos; varying vec3 vBary; varying vec3 vEdgeMask;
            varying vec3 vColor;
            uniform vec4 uSkinColor; uniform vec4 uSuitColor; uniform vec4 uMeshColor;
            uniform float uShowSkin; uniform mat4 uModelView; uniform float uUseVertexColor;
            uniform float uMeshGlow;

            const float WIRE_WIDTH = 0.7;
            const float WIRE_GLOW_RIM_POWER = 3.5;
            const float WIRE_INTENSITY = 0.46;
            const float ANALYSIS_WIRE_INTENSITY_BOOST = 0.08;
            const float STITCH_START = 0.91;
            const float STITCH_END = 0.985;
            const float STITCH_INTENSITY = 0.28;
            const float ANALYSIS_ACCENT_STRENGTH = 0.92;
            const float ANALYSIS_LUMA_RECOVERY = 0.65;
            const float ANALYSIS_TEAL_HIGHLIGHT = 0.06;
            const float ANALYSIS_COLOR_CONTRAST = 1.16;

            float skinMaskForModelPosition(vec3 modelPos) {
                float hands = step(0.36, abs(modelPos.x)) * step(-1.05, modelPos.y) * step(modelPos.y, 0.05);
                float feet = step(modelPos.y, -1.32);
                return min(1.0, hands + feet) * uShowSkin;
            }

            vec3 viewNormalFromModelDerivatives(vec3 modelPos) {
                vec3 fdx = dFdx(modelPos);
                vec3 fdy = dFdy(modelPos);
                return normalize((uModelView * vec4(normalize(cross(fdx, fdy)), 0.0)).xyz);
            }

            float lightingFalloff(vec3 viewNormal, vec3 lightDir) {
                float brightness = smoothstep(-0.15, 0.55, dot(viewNormal, lightDir));
                return pow(brightness, 1.8);
            }

            vec3 bodyBaseColor(float falloff) {
                return mix(vec3(0.0, 0.004, 0.008), uSuitColor.rgb + uMeshColor.rgb * 0.10, falloff);
            }

            float meshEdgeCoverage(vec3 bary, vec3 edgeMask) {
                vec3 baryWidth = fwidth(bary);
                vec3 baryAA = smoothstep(vec3(0.0), baryWidth * WIRE_WIDTH, bary);
                vec3 edgeVec = mix(vec3(1.0), baryAA, edgeMask);
                return 1.0 - min(min(edgeVec.x, edgeVec.y), edgeVec.z);
            }

            vec3 flatWireColor(vec3 viewNormal, vec3 lightDir) {
                vec3 halfDir = normalize(lightDir + vec3(0.0, 0.0, 1.0));
                float spec = pow(max(dot(viewNormal, halfDir), 0.0), 64.0);
                return min(vec3(1.0), uMeshColor.rgb * (1.10 + spec * 1.20));
            }

            vec3 colorAnalysisWire(vec3 tealWire, vec3 analysisColor) {
                vec3 contrastedColor = clamp(
                    (analysisColor - vec3(0.5)) * ANALYSIS_COLOR_CONTRAST + vec3(0.5),
                    vec3(0.0),
                    vec3(1.0)
                );
                vec3 accentedWire = mix(tealWire, contrastedColor, ANALYSIS_ACCENT_STRENGTH);
                float tealLuma = dot(tealWire, vec3(0.2126, 0.7152, 0.0722));
                float accentLuma = dot(accentedWire, vec3(0.2126, 0.7152, 0.0722));
                float lift = max(tealLuma - accentLuma, 0.0) * ANALYSIS_LUMA_RECOVERY;
                vec3 brightenedAccent = min(
                    vec3(1.0),
                    accentedWire + vec3(lift) + tealWire * ANALYSIS_TEAL_HIGHLIGHT
                );
                return mix(tealWire, brightenedAccent, uUseVertexColor);
            }

            vec3 applyMeshWire(vec3 baseColor, vec3 wireColor, float edgeCoverage, float analysisEnabled) {
                float intensity = WIRE_INTENSITY + ANALYSIS_WIRE_INTENSITY_BOOST * analysisEnabled;
                return mix(baseColor, wireColor, edgeCoverage * intensity);
            }

            vec3 applyJunctionStitch(vec3 baseColor, vec3 wireColor, vec3 bary) {
                // Junction stitch: not a highlight/glow. It uses the same flat wire color to
                // cover tiny barycentric gaps where mesh edges meet at triangle vertices.
                float maxBaryForStitch = max(max(bary.x, bary.y), bary.z);
                float vertexStitch = smoothstep(STITCH_START, STITCH_END, maxBaryForStitch);
                return mix(baseColor, wireColor, vertexStitch * STITCH_INTENSITY);
            }

            vec3 applyPointHighlightNotUsedForNow(vec3 baseColor, vec3 wireColor, vec3 bary) {
                // Not used for now: design uses a flat, dim wireframe without cyan point glow.
                float maxBary = max(max(bary.x, bary.y), bary.z);
                float pointMask = smoothstep(0.88, 0.95, maxBary);
                vec3 pointColor = min(vec3(1.0), wireColor * 1.18);
                return mix(baseColor, pointColor, pointMask * 0.02);
            }

            vec3 applyRimGlowNotUsedForNow(vec3 baseColor, vec3 viewNormal) {
                // Not used for now: keep this function so we can restore/tune rim if design changes.
                float rim = pow(1.0 - max(viewNormal.z, 0.0), 8.0);
                return baseColor + uMeshColor.rgb * rim * 0.12;
            }

            void main() {
                float skinMask = skinMaskForModelPosition(vModelPos);
                vec3 nView = viewNormalFromModelDerivatives(vModelPos);
                vec3 lightDir = normalize(vec3(0.0, 0.6, 0.8));
                float falloff = lightingFalloff(nView, lightDir);

                vec3 body = bodyBaseColor(falloff);
                float edge = meshEdgeCoverage(vBary, vEdgeMask);
                vec3 wireCol = flatWireColor(nView, lightDir);
                wireCol = colorAnalysisWire(wireCol, vColor);
                vec3 finalBodyCol = applyMeshWire(body, wireCol, edge, uUseVertexColor);
                finalBodyCol = applyJunctionStitch(finalBodyCol, wireCol, vBary);

                float rim = pow(1.0 - max(nView.z, 0.0), WIRE_GLOW_RIM_POWER);
                finalBodyCol += uMeshColor.rgb * rim * clamp(uMeshGlow, 0.0, 1.0);

                // Not used for now:
                // finalBodyCol = applyPointHighlightNotUsedForNow(finalBodyCol, wireCol, vBary);
                // finalBodyCol = applyRimGlowNotUsedForNow(finalBodyCol, nView);

                gl_FragColor = vec4(mix(finalBodyCol, uSkinColor.rgb, skinMask), 1.0);
            }
        """
    }
}
