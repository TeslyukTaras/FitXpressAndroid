package com.hexis.bi.ui.avatar

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.SystemClock
import androidx.compose.ui.util.lerp
import androidx.core.content.ContextCompat
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.avatar.MetricAvatarFrameSolver.buildFrames
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal class MetricAvatarRenderer {
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

    private var ringProgram = 0
    private var ringAPos = 0
    private var ringAAlpha = 0
    private var ringUMvp = 0
    private var ringUColor = 0
    private var bodyRings: List<MetricAvatarBodyRing> = emptyList()
    private var cachedRingMesh: ObjMesh? = null
    private var cachedRingSpecs: List<MetricAvatarBodyRing> = emptyList()
    private var cachedRingGeometry: List<BodyRingGeometry> = emptyList()

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

    fun setDrawBackground(draw: Boolean) {
        drawBackground = draw
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
        clearBodyRingCache()
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

    fun setBodyRings(rings: List<MetricAvatarBodyRing>) {
        bodyRings = rings
        clearBodyRingCache()
    }

    private fun clearBodyRingCache() {
        cachedRingMesh = null
        cachedRingSpecs = emptyList()
        cachedRingGeometry = emptyList()
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
        return link?.displayYaw() ?: yaw
    }

    /**
     * Yaw spin applied on top of the active framing frame. Compare links rotate via the shared
     * link (so both models spin together); the unlinked Visual tab uses the local offset.
     */
    private fun framingSpinYaw(): Float {
        val link = rotationLink
        return link?.displayYaw() ?: framingYawOffsetDeg
    }

    private fun modelPitch(): Float {
        val link = rotationLink
        return if (link != null) synchronized(link) { link.pitch } else pitch
    }

    private fun modelZoom(): Float {
        val link = rotationLink
        return link?.displayZoom() ?: userZoom
    }

    private fun modelPanX(): Float {
        val link = rotationLink
        return link?.displayPanX() ?: userPanX
    }

    private fun modelPanY(): Float {
        val link = rotationLink
        return link?.displayPanY() ?: userPanY
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

        gradientProgram = linkProgram(GRADIENT_VERTEX_SHADER, GRADIENT_FRAGMENT_SHADER, "Gradient ")
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

        program = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
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
        leaderProgram = linkProgram(LEADER_VERTEX_SHADER, LEADER_FRAGMENT_SHADER, "Leader ")
        if (leaderProgram != 0) {
            leaderAPos = GLES20.glGetAttribLocation(leaderProgram, "aPos")
            leaderUMvp = GLES20.glGetUniformLocation(leaderProgram, "uMvp")
            leaderUColor = GLES20.glGetUniformLocation(leaderProgram, "uColor")
        }
        ringProgram = linkProgram(RING_VERTEX_SHADER, RING_FRAGMENT_SHADER, "Ring ")
        if (ringProgram != 0) {
            ringAPos = GLES20.glGetAttribLocation(ringProgram, "aPos")
            ringAAlpha = GLES20.glGetAttribLocation(ringProgram, "aAlpha")
            ringUMvp = GLES20.glGetUniformLocation(ringProgram, "uMvp")
            ringUColor = GLES20.glGetUniformLocation(ringProgram, "uColor")
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
    }

    fun onDrawFrame() {
        drawFrameBackground()
        val m = mesh ?: return
        if (program == 0) return

        GLES20.glUseProgram(program)
        updateAvatarMatrices(currentAvatarFrame())
        bindAvatarUniforms()
        drawAvatarBody(m)
        drawBodyRings(m)
        drawRepairWireOverlay(m)
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

    private fun drawBodyRings(m: ObjMesh) {
        val rings = bodyRings
        if (rings.isEmpty() || ringProgram == 0 || ringAPos < 0 || ringAAlpha < 0) return

        GLES20.glUseProgram(ringProgram)
        GLES20.glUniformMatrix4fv(ringUMvp, 1, false, mvp, 0)
        GLES20.glUniform4f(ringUColor, BODY_RING_R, BODY_RING_G, BODY_RING_B, 1f)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        bodyRingGeometry(m, rings).forEach { geometry ->
            val buffer = bodyRingBuffer(geometry)
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            drawBodyRingLine(buffer, BODY_RING_GLOW_LINE_WIDTH_PX, BODY_RING_GLOW_ALPHA)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDepthFunc(GLES20.GL_LEQUAL)
            drawBodyRingLine(buffer, BODY_RING_LINE_WIDTH_PX, 1f)
        }

        GLES20.glDisableVertexAttribArray(ringAPos)
        GLES20.glDisableVertexAttribArray(ringAAlpha)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(true)
    }

    private fun bodyRingGeometry(
        mesh: ObjMesh,
        rings: List<MetricAvatarBodyRing>,
    ): List<BodyRingGeometry> {
        if (cachedRingMesh === mesh && cachedRingSpecs == rings) return cachedRingGeometry
        val geometry = rings.mapNotNull { buildBodyRingGeometry(mesh, it) }
        cachedRingMesh = mesh
        cachedRingSpecs = rings.toList()
        cachedRingGeometry = geometry
        return geometry
    }

    private fun drawBodyRingLine(
        buffer: FloatBuffer,
        lineWidth: Float,
        alpha: Float,
    ) {
        GLES20.glUniform4f(ringUColor, BODY_RING_R, BODY_RING_G, BODY_RING_B, alpha)
        GLES20.glLineWidth(lineWidth)
        buffer.position(0)
        GLES20.glVertexAttribPointer(
            ringAPos,
            FLOATS_PER_ATTRIB,
            GLES20.GL_FLOAT,
            false,
            BODY_RING_VERTEX_STRIDE_BYTES,
            buffer,
        )
        GLES20.glEnableVertexAttribArray(ringAPos)
        buffer.position(FLOATS_PER_ATTRIB)
        GLES20.glVertexAttribPointer(
            ringAAlpha,
            1,
            GLES20.GL_FLOAT,
            false,
            BODY_RING_VERTEX_STRIDE_BYTES,
            buffer,
        )
        GLES20.glEnableVertexAttribArray(ringAAlpha)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, BODY_RING_SEGMENTS + 1)
    }

    private fun buildBodyRingGeometry(
        mesh: ObjMesh,
        ring: MetricAvatarBodyRing,
    ): BodyRingGeometry? {
        val bounds = ringBounds(mesh, ring.region) ?: return null
        val y = bounds.centerY + mesh.bounds.spanY * ring.verticalOffsetFraction
        val radiusScale = ring.radiusScale.coerceAtLeast(0.1f)
        val section = when (ring.region) {
            BodyMeasurementRegion.Waist -> waistTorsoExtrema(mesh, y)
            else -> meshExtremaAtY(
                mesh = mesh,
                y = y,
                region = ring.region,
                sectionBounds = bounds,
                fitFullCrossSection = ring.fitFullCrossSection,
            )
        }
        val xRadius =
            (section.spanX * 0.5f * radiusScale + section.spanX * BODY_RING_PADDING_FRACTION)
                .coerceAtLeast(0.01f)
        val zRadius = maxOf(
            section.spanZ * 0.5f * radiusScale + mesh.bounds.spanZ * BODY_RING_DEPTH_PADDING_FRACTION,
            mesh.bounds.spanX * BODY_RING_MIN_DEPTH_RADIUS_FRACTION,
        ).coerceAtLeast(0.01f)
        val points = FloatArray((BODY_RING_SEGMENTS + 1) * FLOATS_PER_ATTRIB)
        var cursor = 0
        for (i in 0..BODY_RING_SEGMENTS) {
            val angle = (i.toFloat() / BODY_RING_SEGMENTS) * TWO_PI
            val x = section.centerX + cos(angle) * xRadius
            val z = section.centerZ + sin(angle) * zRadius
            points[cursor++] = x
            points[cursor++] = section.centerY
            points[cursor++] = z
        }
        return BodyRingGeometry(points = points)
    }

    private fun bodyRingBuffer(geometry: BodyRingGeometry): FloatBuffer {
        val points = geometry.points
        val vertexCount = points.size / FLOATS_PER_ATTRIB
        val values = FloatArray(vertexCount * BODY_RING_FLOATS_PER_VERTEX)
        var minViewZ = Float.MAX_VALUE
        var maxViewZ = -Float.MAX_VALUE
        var pointCursor = 0
        repeat(vertexCount) {
            val viewZ =
                viewSpaceZ(points[pointCursor], points[pointCursor + 1], points[pointCursor + 2])
            minViewZ = minOf(minViewZ, viewZ)
            maxViewZ = maxOf(maxViewZ, viewZ)
            pointCursor += FLOATS_PER_ATTRIB
        }

        val viewZRange = (maxViewZ - minViewZ).coerceAtLeast(1e-5f)
        var cursor = 0
        pointCursor = 0
        repeat(vertexCount) {
            val x = points[pointCursor]
            val y = points[pointCursor + 1]
            val z = points[pointCursor + 2]
            val viewZ = viewSpaceZ(x, y, z)
            val frontT = ((viewZ - minViewZ) / viewZRange).coerceIn(0f, 1f)
            val alpha = ((frontT - BODY_RING_TRANSPARENT_STOP) /
                    (1f - BODY_RING_TRANSPARENT_STOP)).coerceIn(BODY_RING_BACK_ALPHA, 1f)
            values[cursor++] = x
            values[cursor++] = y
            values[cursor++] = z
            values[cursor++] = alpha
            pointCursor += FLOATS_PER_ATTRIB
        }
        return ByteBuffer.allocateDirect(values.size * BYTES_PER_FLOAT_GL)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }
    }

    private fun viewSpaceZ(x: Float, y: Float, z: Float): Float =
        temp[2] * x + temp[6] * y + temp[10] * z + temp[14]

    private fun meshExtremaAtY(
        mesh: ObjMesh,
        y: Float,
        region: BodyMeasurementRegion,
        sectionBounds: ModelBounds,
        fitFullCrossSection: Boolean,
    ): ModelBounds {
        val bandHalfHeight = mesh.bounds.spanY * when (region) {
            BodyMeasurementRegion.Shoulders -> 0.045f
            BodyMeasurementRegion.Thigh -> 0.045f
            BodyMeasurementRegion.HipsGlutes -> 0.04f
            else -> 0.035f
        }
        val torsoHalfWidth = maxOf(
            sectionBounds.spanX * when (region) {
                BodyMeasurementRegion.Shoulders -> 0.56f
                BodyMeasurementRegion.Thigh -> 0.90f
                BodyMeasurementRegion.HipsGlutes -> 0.72f
                else -> 0.42f
            },
            mesh.bounds.spanX * when (region) {
                BodyMeasurementRegion.Shoulders -> 0.18f
                BodyMeasurementRegion.Thigh -> 0.24f
                BodyMeasurementRegion.HipsGlutes -> 0.20f
                else -> 0.11f
            },
        )
        val centerX = sectionBounds.centerX
        val buffer = mesh.vertexBuffer.duplicate()
        buffer.position(0)
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        var found = false
        repeat(mesh.vertexCount) {
            val x = buffer.get()
            val vy = buffer.get()
            val z = buffer.get()
            buffer.position(buffer.position() + FLOATS_PER_INTERLEAVED_VERTEX - FLOATS_PER_ATTRIB)
            val insideWidth = fitFullCrossSection || abs(x - centerX) <= torsoHalfWidth
            if (abs(vy - y) <= bandHalfHeight && insideWidth) {
                found = true
                minX = minOf(minX, x); maxX = maxOf(maxX, x)
                minY = minOf(minY, vy); maxY = maxOf(maxY, vy)
                minZ = minOf(minZ, z); maxZ = maxOf(maxZ, z)
            }
        }
        return if (found) {
            ModelBounds(minX, maxX, minY, maxY, minZ, maxZ)
        } else {
            ringBounds(mesh, region) ?: mesh.bounds
        }
    }

    private fun waistTorsoExtrema(mesh: ObjMesh, y: Float): ModelBounds {
        val bandHalfHeight = mesh.bounds.spanY * 0.035f
        val midX = mesh.bounds.centerX
        val maxHalfWindow = mesh.bounds.spanX * WAIST_RING_TORSO_HALF_WIDTH_FRACTION
        val buffer = mesh.vertexBuffer.duplicate()
        buffer.position(0)
        var halfWidth = 0f
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        var found = false
        repeat(mesh.vertexCount) {
            val x = buffer.get()
            val vy = buffer.get()
            val z = buffer.get()
            buffer.position(buffer.position() + FLOATS_PER_INTERLEAVED_VERTEX - FLOATS_PER_ATTRIB)
            val dx = abs(x - midX)
            if (abs(vy - y) <= bandHalfHeight && dx <= maxHalfWindow) {
                found = true
                halfWidth = maxOf(halfWidth, dx)
                minZ = minOf(minZ, z); maxZ = maxOf(maxZ, z)
            }
        }
        if (!found) return ringBounds(mesh, BodyMeasurementRegion.Waist) ?: mesh.bounds
        return ModelBounds(
            minX = midX - halfWidth,
            maxX = midX + halfWidth,
            minY = y - bandHalfHeight,
            maxY = y + bandHalfHeight,
            minZ = minZ,
            maxZ = maxZ,
        )
    }

    private fun ringBounds(
        mesh: ObjMesh,
        region: BodyMeasurementRegion,
    ): ModelBounds? {
        if (region == BodyMeasurementRegion.Waist) {
            waistRingBounds(mesh)?.let { return it }
        }
        val key = BodyMeasurementKeys.visualAnchorKey(region)
        val points = ArrayList<FloatArray>()
        appendRingPacked(points, key?.let { mesh.measurementGuide.crossSectionPolylines[it] })
        appendRingPacked(
            points,
            key?.let { mesh.measurementGuide.crossSectionPolylinesOpposite[it] })
        key?.let { mesh.measurementGuide.anchorPoints[it] }?.let { points += it }
        val bounds = ringBoundsOf(points)
        val resolved = when {
            bounds != null -> bounds
            region == BodyMeasurementRegion.HipsGlutes -> ModelBounds(
                minX = mesh.bounds.minX * 0.54f,
                maxX = mesh.bounds.maxX * 0.54f,
                minY = mesh.bounds.minY + mesh.bounds.spanY * 0.36f,
                maxY = mesh.bounds.minY + mesh.bounds.spanY * 0.40f,
                minZ = mesh.bounds.minZ * 0.7f,
                maxZ = mesh.bounds.maxZ * 0.7f,
            )

            else -> null
        }
        return if (region == BodyMeasurementRegion.Shoulders) {
            resolved?.copy(
                minY = resolved.minY - mesh.bounds.spanY * SHOULDER_RING_DOWNWARD_OFFSET_FRACTION,
                maxY = resolved.maxY - mesh.bounds.spanY * SHOULDER_RING_DOWNWARD_OFFSET_FRACTION,
            )
        } else {
            resolved
        }
    }

    private fun waistRingBounds(mesh: ObjMesh): ModelBounds? {
        val upper = ringBoundsForKey(mesh, BodyMeasurementKeys.UpperWaist)
        val waist = ringBoundsForKey(mesh, BodyMeasurementKeys.Waist)
        val lower = ringBoundsForKey(mesh, BodyMeasurementKeys.LowerWaist)
        return when {
            upper != null && waist != null -> blendBounds(upper, waist, 0.24f)
            upper != null && lower != null -> blendBounds(upper, lower, 0.18f)
            waist != null -> waist.copy(
                minY = waist.minY + mesh.bounds.spanY * WAIST_RING_UPWARD_OFFSET_FRACTION,
                maxY = waist.maxY + mesh.bounds.spanY * WAIST_RING_UPWARD_OFFSET_FRACTION,
            )

            upper != null -> upper
            lower != null -> lower.copy(
                minY = lower.minY + mesh.bounds.spanY * WAIST_RING_UPWARD_OFFSET_FRACTION,
                maxY = lower.maxY + mesh.bounds.spanY * WAIST_RING_UPWARD_OFFSET_FRACTION,
            )

            else -> null
        }
    }

    private fun ringBoundsForKey(mesh: ObjMesh, key: String): ModelBounds? {
        val points = ArrayList<FloatArray>()
        appendRingPacked(points, mesh.measurementGuide.crossSectionPolylines[key])
        appendRingPacked(points, mesh.measurementGuide.crossSectionPolylinesOpposite[key])
        mesh.measurementGuide.anchorPoints[key]?.let { points += it }
        return ringBoundsOf(points)
    }

    private fun blendBounds(
        start: ModelBounds,
        end: ModelBounds,
        fraction: Float,
    ): ModelBounds = ModelBounds(
        minX = lerp(start.minX, end.minX, fraction),
        maxX = lerp(start.maxX, end.maxX, fraction),
        minY = lerp(start.minY, end.minY, fraction),
        maxY = lerp(start.maxY, end.maxY, fraction),
        minZ = lerp(start.minZ, end.minZ, fraction),
        maxZ = lerp(start.maxZ, end.maxZ, fraction),
    )

    private fun appendRingPacked(into: MutableList<FloatArray>, packed: FloatArray?) {
        if (packed == null) return
        var i = 0
        while (i + 2 < packed.size) {
            into += floatArrayOf(packed[i], packed[i + 1], packed[i + 2])
            i += 3
        }
    }

    private fun ringBoundsOf(points: List<FloatArray>): ModelBounds? {
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

    /**
     * Compiles + links a GL program from the given shader sources, returning the program id (0 on
     * failure). [label] tags the link-error log (e.g. "Gradient ", "Leader ", "Ring "; "" for the body).
     */
    private fun linkProgram(
        vertexSource: String,
        fragmentSource: String,
        label: String = "",
    ): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (vs == 0 || fs == 0) return 0

        val programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vs)
        GLES20.glAttachShader(programId, fs)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Timber.e("GL ${label}Link Error: %s", GLES20.glGetProgramInfoLog(programId))
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
        private const val BODY_RING_R = 24f / 255f
        private const val BODY_RING_G = 202f / 255f
        private const val BODY_RING_B = 236f / 255f
        private const val BODY_RING_LINE_WIDTH_PX = 3f
        private const val BODY_RING_GLOW_LINE_WIDTH_PX = 9f
        private const val BODY_RING_GLOW_ALPHA = 0.32f
        private const val BODY_RING_SEGMENTS = 96
        private const val BODY_RING_FLOATS_PER_VERTEX = 4
        private const val BODY_RING_VERTEX_STRIDE_BYTES = BODY_RING_FLOATS_PER_VERTEX * 4
        private const val BODY_RING_TRANSPARENT_STOP = 0.1571f
        private const val BODY_RING_BACK_ALPHA = 0.16f
        private const val WAIST_RING_TORSO_HALF_WIDTH_FRACTION = 0.18f
        private const val BODY_RING_PADDING_FRACTION = 0.045f
        private const val BODY_RING_DEPTH_PADDING_FRACTION = 0.06f
        private const val BODY_RING_MIN_DEPTH_RADIUS_FRACTION = 0.16f
        private const val WAIST_RING_UPWARD_OFFSET_FRACTION = 0.095f
        private const val SHOULDER_RING_DOWNWARD_OFFSET_FRACTION = 0.018f
        private const val TWO_PI = 6.2831855f
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

        private const val CLIP_TRIANGLE_X0 = -1f
        private const val CLIP_TRIANGLE_Y0 = -1f
        private const val CLIP_TRIANGLE_X1 = 3f
        private const val CLIP_TRIANGLE_Y1 = -1f
        private const val CLIP_TRIANGLE_X2 = -1f
        private const val CLIP_TRIANGLE_Y2 = 3f
    }
}
