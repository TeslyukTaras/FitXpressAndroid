package com.hexis.bi.ui.avatar

/**
 * Orientation and animation constants shared across the avatar units — the composable preview,
 * [CompareRotationLink], the [MetricAvatarTextureView] host, and the GL [MetricAvatarRenderer].
 * Unit-local tuning (touch sensitivity, EGL config, render-thread timing) lives with its owner.
 */

/** Neutral tilt, and the seed [CompareRotationLink] measures drag offsets from. */
internal const val INITIAL_PITCH_DEG = -12f
internal const val MIN_PITCH_DEG = -55f
internal const val MAX_PITCH_DEG = 35f

/** Negative sits lower on screen. */
internal const val DEFAULT_FULL_BODY_CENTER_Y = -0.02f

/** Render above view resolution and let [MetricAvatarTextureView] filter down: MSAA cannot antialias
 * the shader's own analytic wire, supersampling can. */
internal const val RENDER_SUPERSAMPLE = 1.5f

/** Apparent wire half-width in *view* pixels. The shader scales it by [RENDER_SUPERSAMPLE]. */
internal const val WIRE_WIDTH_VIEW_PX = 0.7f

/** Duration of the eased camera transition when the framed region or orientation changes. */
internal const val FRAME_ANIMATION_DURATION_MS = 620L
