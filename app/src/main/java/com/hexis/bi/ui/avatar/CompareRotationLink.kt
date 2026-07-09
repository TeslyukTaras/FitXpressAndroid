package com.hexis.bi.ui.avatar

import android.os.SystemClock
import androidx.compose.ui.util.lerp
import java.util.concurrent.CopyOnWriteArrayList

private const val NOT_ANIMATING = 0L

/**
 * Shared orientation / zoom / pan state that links two [MetricAvatarPreview]s (the Compare tab) so
 * they rotate together, with an eased reset back to defaults when the framed part changes.
 *
 * Mutated from the touch thread, read from two render threads: a reset must snapshot every field as
 * one consistent pose, so they are guarded by the instance lock and not just `@Volatile`.
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

    /** Guarded by `this`. */
    private var resetStartMs = NOT_ANIMATING
    private var fromYaw = 0f
    private var fromPitch = INITIAL_PITCH_DEG
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

    private fun notifyInvalidate() {
        invalidateCallbacks.forEach { it() }
    }

    /** Answered from the clock: a preview whose mesh never draws would otherwise never retire it. */
    fun isResetAnimating(): Boolean = synchronized(this) {
        isResetAnimatingLocked(SystemClock.uptimeMillis())
    }

    private fun isResetAnimatingLocked(now: Long): Boolean =
        resetStartMs != NOT_ANIMATING && now - resetStartMs < FRAME_ANIMATION_DURATION_MS

    /** Both previews call this for one change; only the first snapshots, or it would ease from zero. */
    fun beginResetAnimation() {
        synchronized(this) {
            val now = SystemClock.uptimeMillis()
            if (!isResetAnimatingLocked(now)) {
                fromYaw = yaw
                fromPitch = pitch
                fromZoom = zoom
                fromPanX = panX
                fromPanY = panY
                resetStartMs = now
            }
            yaw = 0f
            pitch = INITIAL_PITCH_DEG
            zoom = MetricAvatarCamera.MIN_USER_ZOOM
            panX = 0f
            panY = 0f
        }
        notifyInvalidate()
    }

    fun resetAdjustmentsImmediate() {
        synchronized(this) {
            resetStartMs = NOT_ANIMATING
            yaw = 0f
            pitch = INITIAL_PITCH_DEG
            zoom = MetricAvatarCamera.MIN_USER_ZOOM
            panX = 0f
            panY = 0f
        }
        notifyInvalidate()
    }

    fun applyZoomFactor(factor: Float) {
        synchronized(this) {
            resetStartMs = NOT_ANIMATING
            zoom = (zoom * factor).coerceIn(
                MetricAvatarCamera.MIN_USER_ZOOM,
                MetricAvatarCamera.MAX_USER_ZOOM,
            )
        }
        notifyInvalidate()
    }

    fun applyPanDelta(dxWorld: Float, dyWorld: Float, maxX: Float, maxY: Float) {
        synchronized(this) {
            resetStartMs = NOT_ANIMATING
            panX = (panX + dxWorld).coerceIn(-maxX, maxX)
            panY = (panY + dyWorld).coerceIn(-maxY, maxY)
        }
        notifyInvalidate()
    }

    fun applyRotationDelta(dyaw: Float, dpitch: Float) {
        synchronized(this) {
            resetStartMs = NOT_ANIMATING
            yaw += dyaw
            pitch = (pitch + dpitch).coerceIn(MIN_PITCH_DEG, MAX_PITCH_DEG)
        }
        notifyInvalidate()
    }

    fun clampPan(maxX: Float, maxY: Float) {
        synchronized(this) {
            panX = panX.coerceIn(-maxX, maxX)
            panY = panY.coerceIn(-maxY, maxY)
        }
        notifyInvalidate()
    }

    fun replaceOrientation(yawDeg: Float, pitchDeg: Float) {
        synchronized(this) {
            yaw = yawDeg
            pitch = pitchDeg.coerceIn(MIN_PITCH_DEG, MAX_PITCH_DEG)
        }
        notifyInvalidate()
    }

    /** Call under `this`. */
    private fun resetProgress(): Float {
        if (resetStartMs == NOT_ANIMATING) return 1f
        val linear =
            (SystemClock.uptimeMillis() - resetStartMs).toFloat() / FRAME_ANIMATION_DURATION_MS
        if (linear >= 1f) {
            resetStartMs = NOT_ANIMATING
            return 1f
        }
        return easeInOutSine(linear)
    }

    fun displayYaw(): Float = synchronized(this) {
        val progress = resetProgress()
        if (progress >= 1f) yaw else lerpAngleDegrees(fromYaw, yaw, progress)
    }

    fun displayPitch(): Float = synchronized(this) {
        val progress = resetProgress()
        if (progress >= 1f) pitch else lerp(fromPitch, pitch, progress)
    }

    fun displayZoom(): Float = synchronized(this) {
        val progress = resetProgress()
        if (progress >= 1f) zoom else lerp(fromZoom, zoom, progress)
    }

    fun displayPanX(): Float = synchronized(this) {
        val progress = resetProgress()
        if (progress >= 1f) panX else lerp(fromPanX, panX, progress)
    }

    fun displayPanY(): Float = synchronized(this) {
        val progress = resetProgress()
        if (progress >= 1f) panY else lerp(fromPanY, panY, progress)
    }
}
