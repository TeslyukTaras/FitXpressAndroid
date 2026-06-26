package com.hexis.bi.ui.avatar

import androidx.compose.ui.util.lerp
import java.nio.FloatBuffer
import kotlin.math.cos

/**
 * Plain geometry value types shared across the avatar renderer: the parsed [ObjMesh], the per-region
 * camera [AvatarFrame] (plus its interpolation helpers), the [BodyRingGeometry] line buffer, and the
 * axis-aligned [ModelBounds] used for framing.
 */

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

internal data class AvatarFrame(
    val yawDeg: Float,
    val pitchDeg: Float,
    val distanceScale: Float,
    val translateX: Float,
    val translateY: Float,
    /** Camera eye height vs the part center: >0 looks down from above, 0 is level, <0 from below. */
    val eyeHeight: Float,
)

internal fun AvatarFrame.lerpTo(target: AvatarFrame, fraction: Float): AvatarFrame =
    AvatarFrame(
        yawDeg = lerpAngleDegrees(yawDeg, target.yawDeg, fraction),
        pitchDeg = lerp(pitchDeg, target.pitchDeg, fraction),
        distanceScale = lerp(distanceScale, target.distanceScale, fraction),
        translateX = lerp(translateX, target.translateX, fraction),
        translateY = lerp(translateY, target.translateY, fraction),
        eyeHeight = lerp(eyeHeight, target.eyeHeight, fraction),
    )

internal fun lerpAngleDegrees(start: Float, stop: Float, fraction: Float): Float {
    var delta = (stop - start) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return start + delta * fraction
}

internal fun easeInOutSine(fraction: Float): Float =
    ((1f - cos(fraction.coerceIn(0f, 1f) * Math.PI.toFloat())) / 2f)

internal data class BodyRingGeometry(
    val points: FloatArray,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is BodyRingGeometry && points.contentEquals(other.points))

    override fun hashCode(): Int = points.contentHashCode()
}

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
