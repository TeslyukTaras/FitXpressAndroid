package com.hexis.bi.ui.avatar

import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Computes the per-region [AvatarFrame] camera setups (yaw / zoom / on-screen offset) for the Visual
 * and Compare tabs from a mesh's [MetricAvatarMeasurementGuide]. Pure geometry — no GL state.
 */
internal object MetricAvatarFrameSolver {
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
