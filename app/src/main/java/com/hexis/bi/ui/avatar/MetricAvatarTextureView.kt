package com.hexis.bi.ui.avatar

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.TextureView
import android.view.VelocityTracker
import android.view.View
import com.hexis.bi.domain.body.BodyMeasurementRegion
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow

private const val TOUCH_YAW_SENSITIVITY = 0.15f
private const val TOUCH_PITCH_SENSITIVITY = 0.11f

private const val TWO_FINGER_ZOOM_SENSITIVITY = 0.5f
private const val FRAME_ANIMATION_FRAME_DELAY_MS = 16L
private const val RENDER_THREAD_JOIN_TIMEOUT_MS = 250L

private const val FLING_VELOCITY_UNITS = 1000

/** Fraction of the spin velocity surviving one second of coasting. */
private const val FLING_VELOCITY_RETAINED_PER_SECOND = 0.0008f

/** Damping the lift-off velocity is what reads as a heavier model. */
private const val FLING_RELEASE_DAMPING = 0.35f

private const val FLING_MIN_DEG_PER_SECOND = 12f

/** Caps the coast's frame delta so a stall cannot jump the model. */
private const val FLING_MAX_FRAME_SECONDS = 0.032f
private const val MILLIS_PER_SECOND = 1000f

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
    fun setFullBodyFigureHeightPx(heightPx: Float)
    fun setFramePitch(pitchDeg: Float)
    fun setFullBodyCenterY(centerY: Float)
    fun setBaseOrientation(yawDeg: Float, pitchDeg: Float)
    fun setFramingRegion(region: BodyMeasurementRegion?)
    fun setCenterFraming(enabled: Boolean)
    fun setBodyRings(rings: List<MetricAvatarBodyRing>)
    fun setZoomLevelListener(listener: ((Float) -> Unit)?)
    fun setRenderFailureListener(listener: (() -> Unit)?)
    fun setOnFirstFrameRendered(listener: (() -> Unit)?)
}


internal class MetricAvatarTextureView(
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
    private var twoFingerActive = false
    private var gestureLastSpan = 0f
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

    private val renderPending = AtomicBoolean(false)

    private val pendingRotationLock = Any()
    private var pendingDeltaYawDeg = 0f
    private var pendingDeltaPitchDeg = 0f

    private var velocityTracker: VelocityTracker? = null
    private var flingYawDegPerSecond = 0f
    private var flingPitchDegPerSecond = 0f
    private var flingLastFrameMs = 0L
    private val flingStep = Runnable { stepFling() }
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
        cancelFling()
        endVelocityTracking()
        stopRenderThread()
    }

    override fun onDetachedFromWindow() {
        cancelFling()
        endVelocityTracking()
        super.onDetachedFromWindow()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        val (renderWidth, renderHeight) = applySupersampledBufferSize(surface, width, height)
        startRenderThread(surface, renderWidth, renderHeight)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        val (renderWidth, renderHeight) = applySupersampledBufferSize(surface, width, height)
        queueRenderEvent {
            avatarRenderer.onSurfaceChanged(renderWidth, renderHeight)
            drawFrame()
        }
    }

    private fun applySupersampledBufferSize(
        surface: SurfaceTexture,
        width: Int,
        height: Int,
    ): Pair<Int, Int> {
        val renderWidth = (width * RENDER_SUPERSAMPLE).toInt().coerceAtLeast(1)
        val renderHeight = (height * RENDER_SUPERSAMPLE).toInt().coerceAtLeast(1)
        surface.setDefaultBufferSize(renderWidth, renderHeight)
        return renderWidth to renderHeight
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRenderThread()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // The queued render event for a newly loaded mesh sets the mesh and draws in one shot, so
        // the first presented frame already contains it. Reveal immediately rather than waiting for
        // a second surface update — while the GL layer is still alpha 0 the platform may not draw
        // (and therefore not deliver) that second update until an unrelated invalidation (a touch),
        // which left the loader stuck on screen until the user interacted with it.
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
        renderPending.set(false)
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

    /**
     * Coalesced: touch samples outpace [EGL14.eglSwapBuffers], so one draw per sample backs the queue
     * up and the model trails the finger. State is written on the touch thread; one draw is queued.
     */
    private fun requestRenderOnThread() {
        val handler = renderHandler ?: return
        if (!renderPending.compareAndSet(false, true)) return
        val posted = handler.post {
            renderPending.set(false)
            drawFrame()
        }
        // A quitting looper rejects the post; a stuck flag would wedge the view into never redrawing.
        if (!posted) renderPending.set(false)
    }

    private fun clearPendingRotation() {
        synchronized(pendingRotationLock) {
            pendingDeltaYawDeg = 0f
            pendingDeltaPitchDeg = 0f
        }
    }

    /** Render thread. */
    private fun drainPendingRotation() {
        var dyaw: Float
        var dpitch: Float
        synchronized(pendingRotationLock) {
            dyaw = pendingDeltaYawDeg
            dpitch = pendingDeltaPitchDeg
            pendingDeltaYawDeg = 0f
            pendingDeltaPitchDeg = 0f
        }
        if (dyaw != 0f || dpitch != 0f) avatarRenderer.rotateBy(dyaw, dpitch)
    }

    private fun drawFrame() {
        animationFrameScheduled = false
        val display = eglDisplay ?: return
        val surface = eglSurface ?: return
        drainPendingRotation()
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

    override fun setFullBodyFigureHeightPx(heightPx: Float) {
        queueRenderEvent {
            avatarRenderer.setFullBodyFigureHeightPx(heightPx)
            drawFrame()
        }
    }

    override fun setFramePitch(pitchDeg: Float) {
        queueRenderEvent {
            avatarRenderer.setFramePitch(pitchDeg)
            drawFrame()
        }
    }

    override fun setFullBodyCenterY(centerY: Float) {
        queueRenderEvent {
            avatarRenderer.setFullBodyCenterY(centerY)
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
        // The part list is a sibling view, so tapping it never reaches onTouchEvent. A coast left
        // running would feed a delta in and cancel the eased reset this change just started.
        cancelFling()
        clearPendingRotation()
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

    override fun setBodyRings(rings: List<MetricAvatarBodyRing>) {
        queueRenderEvent {
            avatarRenderer.setBodyRings(rings)
            drawFrame()
        }
    }

    override fun setZoomLevelListener(listener: ((Float) -> Unit)?) {
        queueRenderEvent {
            avatarRenderer.setZoomLevelListener(
                listener?.let { target -> { zoom -> mainHandler.post { target(zoom) } } },
            )
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
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            cancelFling()
            beginVelocityTracking(event)
        } else {
            velocityTracker?.addMovement(event)
        }
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
                    twoFingerActive = false
                    val survivor = if (event.actionIndex == 0) 1 else 0
                    lastX = event.getX(survivor)
                    lastY = event.getY(survivor)
                } else if (zoomPanEnabled) {
                    beginTwoFinger(event)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (zoomPanEnabled && twoFingerActive && event.pointerCount >= 2) {
                    handleTwoFingerMove(event)
                } else if (!twoFingerActive) {
                    // A move event batches samples; replay them all or the spin under-reads the drag.
                    if (event.pointerCount == 1) {
                        for (i in 0 until event.historySize) {
                            dragTo(event.getHistoricalX(i), event.getHistoricalY(i))
                        }
                    }
                    dragTo(event.x, event.y)
                }
            }

            MotionEvent.ACTION_UP -> {
                performClick()
                if (!twoFingerActive) startFlingFromVelocity()
                endVelocityTracking()
                onInteractionChanged(false)
                twoFingerActive = false
            }

            MotionEvent.ACTION_CANCEL -> {
                endVelocityTracking()
                onInteractionChanged(false)
                twoFingerActive = false
            }
        }
        return true
    }

    private fun dragTo(x: Float, y: Float) {
        applyRotationDelta(
            dyaw = (x - lastX) * TOUCH_YAW_SENSITIVITY,
            dpitch = (y - lastY) * TOUCH_PITCH_SENSITIVITY,
        )
        lastX = x
        lastY = y
    }

    /** The single funnel for drag and coast. A linked pair routes through the link; others park it. */
    private fun applyRotationDelta(dyaw: Float, dpitch: Float) {
        if (dyaw == 0f && dpitch == 0f) return
        val link = boundCompareLink
        if (link != null) {
            link.applyRotationDelta(dyaw, dpitch)
            return
        }
        synchronized(pendingRotationLock) {
            pendingDeltaYawDeg += dyaw
            pendingDeltaPitchDeg += dpitch
        }
        requestRenderOnThread()
    }

    private fun beginVelocityTracking(event: MotionEvent) {
        velocityTracker?.recycle()
        velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
    }

    private fun endVelocityTracking() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun startFlingFromVelocity() {
        val tracker = velocityTracker ?: return
        tracker.computeCurrentVelocity(FLING_VELOCITY_UNITS)
        val yawVelocity = tracker.xVelocity * TOUCH_YAW_SENSITIVITY * FLING_RELEASE_DAMPING
        val pitchVelocity = tracker.yVelocity * TOUCH_PITCH_SENSITIVITY * FLING_RELEASE_DAMPING
        if (abs(yawVelocity) < FLING_MIN_DEG_PER_SECOND &&
            abs(pitchVelocity) < FLING_MIN_DEG_PER_SECOND
        ) {
            return
        }
        flingYawDegPerSecond = yawVelocity
        flingPitchDegPerSecond = pitchVelocity
        flingLastFrameMs = SystemClock.uptimeMillis()
        postOnAnimation(flingStep)
    }

    private fun stepFling() {
        val now = SystemClock.uptimeMillis()
        val frameSeconds = ((now - flingLastFrameMs) / MILLIS_PER_SECOND)
            .coerceIn(0f, FLING_MAX_FRAME_SECONDS)
        flingLastFrameMs = now

        applyRotationDelta(
            dyaw = flingYawDegPerSecond * frameSeconds,
            dpitch = flingPitchDegPerSecond * frameSeconds,
        )

        val retained = FLING_VELOCITY_RETAINED_PER_SECOND.pow(frameSeconds)
        flingYawDegPerSecond *= retained
        flingPitchDegPerSecond *= retained
        if (abs(flingYawDegPerSecond) < FLING_MIN_DEG_PER_SECOND &&
            abs(flingPitchDegPerSecond) < FLING_MIN_DEG_PER_SECOND
        ) {
            cancelFling()
            return
        }
        postOnAnimation(flingStep)
    }

    private fun cancelFling() {
        removeCallbacks(flingStep)
        flingYawDegPerSecond = 0f
        flingPitchDegPerSecond = 0f
    }

    private fun beginTwoFinger(event: MotionEvent) {
        twoFingerActive = true
        gestureLastSpan = spanOf(event)
        lastCentroidX = averagePointer(event, excludeIndex = -1, axisX = true)
        lastCentroidY = averagePointer(event, excludeIndex = -1, axisX = false)
    }

    private fun handleTwoFingerMove(event: MotionEvent) {
        val span = spanOf(event)
        val cx = averagePointer(event, excludeIndex = -1, axisX = true)
        val cy = averagePointer(event, excludeIndex = -1, axisX = false)
        val zoomFactor = if (gestureLastSpan > 0f && span > 0f) span / gestureLastSpan else 1f
        applyTwoFingerTransform(
            panDx = cx - lastCentroidX,
            panDy = cy - lastCentroidY,
            zoomFactor = zoomFactor,
        )
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

    private fun applyTwoFingerTransform(panDx: Float, panDy: Float, zoomFactor: Float) {
        val zoom = if (zoomFactor != 1f) zoomFactor.pow(TWO_FINGER_ZOOM_SENSITIVITY) else 1f
        val hasPan = panDx != 0f || panDy != 0f
        if (zoom == 1f && !hasPan) return
        val renderPanDx = panDx * RENDER_SUPERSAMPLE
        val renderPanDy = panDy * RENDER_SUPERSAMPLE
        queueRenderEvent {
            if (zoom != 1f) avatarRenderer.zoomBy(zoom)
            if (hasPan) avatarRenderer.panBy(renderPanDx, renderPanDy)
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
