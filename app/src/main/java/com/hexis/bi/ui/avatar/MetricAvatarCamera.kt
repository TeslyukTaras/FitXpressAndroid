package com.hexis.bi.ui.avatar

import kotlin.math.max
import kotlin.math.tan

/**
 * Avatar **camera / projection constants** (matching the GL MVP), packed-geometry thresholds, and the
 * static front-of-body leader anchors used by the Visual measurement overlay. Pure configuration — no
 * mesh math (see [buildMeasurementGuide] and the cross-section helpers for that).
 */
internal object MetricAvatarCamera {
    const val MATRIX_SIZE = 16
    const val INITIAL_VIEW_DISTANCE = 3.2f
    const val FOV_DEG = 42f
    const val FRUSTUM_NEAR = 0.1f
    const val FRUSTUM_FAR = 100f
    const val MIN_TAN_HALF_FOV = 0.001f
    const val MIN_ASPECT_FOR_FRAMING = 0.6f
    const val VIEW_DISTANCE_SAFETY_MARGIN = 1.25f
    const val EYE_HEIGHT = 0.85f

    /**
     * Neck slice plane vs horizontal (degrees): rotates plane normal from +Y toward +Z so the neck
     * ring tilts forward (chin side slightly lower). Matches [neckSlicePlaneNormal].
     */
    const val NECK_SLICE_FORWARD_TILT_DEG = 15f

    /** Camera distance multiplier — lower = closer camera / larger figure in the preview. */
    const val PREVIEW_DISTANCE_SCALE = 0.82f

    /** User pinch-zoom bounds, as a multiplier on the figure's on-screen size (1 = default framing). */
    const val MIN_USER_ZOOM = 1f
    const val MAX_USER_ZOOM = 3f

    /** Pre-computed tan(FOV/2), used to convert a screen-pixel pan into world units. */
    val HALF_FOV_TAN: Float = tan(Math.toRadians(FOV_DEG.toDouble() / 2.0)).toFloat()

    /**
     * Fraction of the visible half-window subtracted from the figure half-extent when clamping pan.
     * 1 = the figure edge stops exactly at the screen edge; lower lets you pan a bit further so the
     * extremes (head / feet) sit comfortably in view rather than flush against the edge.
     */
    const val PAN_EDGE_KEEP_FRACTION = 0.5f
}

/**
 * Shared thresholds for packed XYZ rings / polylines (OBJ slice data + Visual overlay leaders).
 */
internal object MetricAvatarPackedGeometry {
    /** Minimum packed floats for a ring sample (three model-space points × XYZ). */
    const val MIN_PACKED_POLYLINE_FLOATS = 9

    /** Minimum projected screen vertices before treating a contour as drawable / closed. */
    const val MIN_PROJECTED_POLYLINE_VERTICES = 4
}

internal fun computeMetricAvatarViewDistance(viewWidth: Int, viewHeight: Int): Float {
    val aspect = viewWidth.toFloat() / viewHeight.toFloat().coerceAtLeast(1f)
    val tanHalfFov = tan(Math.toRadians(MetricAvatarCamera.FOV_DEG / 2.0)).toFloat()
        .coerceAtLeast(MetricAvatarCamera.MIN_TAN_HALF_FOV)
    val distForHeight = 1.0f / tanHalfFov
    val distForWidth =
        1.0f / (tanHalfFov * aspect.coerceAtLeast(MetricAvatarCamera.MIN_ASPECT_FOR_FRAMING))
    return max(distForHeight, distForWidth) *
            MetricAvatarCamera.VIEW_DISTANCE_SAFETY_MARGIN *
            MetricAvatarCamera.PREVIEW_DISTANCE_SCALE
}

internal data class ModelLeaderSegment(
    val ax: Float,
    val ay: Float,
    val az: Float,
    val ex: Float,
    val ey: Float,
    val ez: Float,
)

internal object MeasurementVisualAnchors {

    /** Reference surface points in bbox-normalized model space (front-of-body attachment targets). */
    private val segmentsByKey: Map<String, ModelLeaderSegment> = mapOf(
        /* Neck: below cranium, front midline */
        "neck" to ModelLeaderSegment(0f, 1.06f, 0.13f, 0.42f, 1.22f, 0.18f),
        /* Shoulder shelf - slightly above the original shoulder line. */
        "shoulders" to ModelLeaderSegment(0.48f, 1.03f, 0.11f, 0.72f, 1.02f, 0.16f),
        /* Upper chest, below the armpits without overlapping the shoulder band. */
        "chest" to ModelLeaderSegment(0f, 0.74f, 0.17f, 0.38f, 0.77f, 0.22f),
        /* Mid forearm - right arm, clear of the elbow crease. */
        "forearm" to ModelLeaderSegment(0.86f, 0.29f, 0.11f, 1.12f, 0.24f, 0.18f),
        /* Upper arm - middle of the biceps; horizontal slicing avoids elbow drift. */
        "bicep" to ModelLeaderSegment(-0.58f, 0.74f, 0.11f, -0.88f, 0.68f, 0.18f),
        "upperWaist" to ModelLeaderSegment(0f, 0.38f, 0.15f, 0.38f, 0.38f, 0.20f),
        /* Natural waist — above iliac crest */
        "waist" to ModelLeaderSegment(0f, 0.24f, 0.15f, 0.42f, 0.18f, 0.22f),
        "lowerWaist" to ModelLeaderSegment(0f, 0.07f, 0.14f, 0.38f, 0.04f, 0.20f),
        /* Left upper thigh - above the knee-side drift seen in mesh centroids. */
        "thigh" to ModelLeaderSegment(-0.24f, -0.29f, 0.11f, -0.55f, -0.35f, 0.18f),
        /* Calf — right leg */
        "calf" to ModelLeaderSegment(0.20f, -0.95f, 0.10f, 0.52f, -1.02f, 0.16f),
    )

    fun fallbackAnchorPosition(key: String): FloatArray? =
        segmentsByKey[key]?.let { floatArrayOf(it.ax, it.ay, it.az) }
}

/** Keys where the UI draws a **mesh cross-section** slice (horizontal plane), not a single point. */
internal val CircumferenceVisualKeys = setOf(
    "neck",
    "shoulders",
    "chest",
    "forearm",
    "bicep",
    "upperWaist",
    "waist",
    "lowerWaist",
    "thigh",
    "calf",
)

/** Limb circumferences: UI draws **two** rings (left + right); leader picks the nearer ring in screen space. */
internal val BilateralCircumferenceKeys = setOf("bicep", "forearm", "thigh", "calf")
