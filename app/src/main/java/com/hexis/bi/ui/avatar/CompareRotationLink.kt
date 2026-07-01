package com.hexis.bi.ui.avatar

import android.os.SystemClock
import androidx.compose.ui.util.lerp
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Shared orientation / zoom / pan state that links two [MetricAvatarPreview]s (the Compare tab) so
 * they rotate together, with an eased reset back to defaults when the framed part changes. Thread-safe:
 * mutated from the GL touch thread and read from render threads.
 */
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
        val linear =
            (SystemClock.uptimeMillis() - resetStartMs).toFloat() / FRAME_ANIMATION_DURATION_MS
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
