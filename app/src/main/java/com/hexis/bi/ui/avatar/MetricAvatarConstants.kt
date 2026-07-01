package com.hexis.bi.ui.avatar

/**
 * Orientation and animation constants shared across the avatar units — the composable preview,
 * [CompareRotationLink], the [MetricAvatarTextureView] host, and the GL [MetricAvatarRenderer].
 * Unit-local tuning (touch sensitivity, EGL config, render-thread timing) lives with its owner.
 */

/** Default starting orientation for the rotatable mesh, in degrees. */
internal const val INITIAL_PITCH_DEG = -12f
internal const val MIN_PITCH_DEG = -55f
internal const val MAX_PITCH_DEG = 35f

/** Duration of the eased camera transition when the framed region or orientation changes. */
internal const val FRAME_ANIMATION_DURATION_MS = 360L
