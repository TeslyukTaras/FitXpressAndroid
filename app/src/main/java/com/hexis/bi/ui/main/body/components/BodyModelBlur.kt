package com.hexis.bi.ui.main.body.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.utils.constants.BodyVisualConstants

// Blurs the model's top and bottom bands with a 2D gaussian (7x7 weighted taps — soft in
// every direction, no vertical smear), then composites a black gradient over the same
// edges so they fade into the dark. Edge samples are clamped, so the model doesn't ghost
// against transparent space.
private const val MODEL_BLUR_AGSL = """
uniform shader contents;
uniform float2 size;
uniform float topBand;
uniform float bottomBand;
uniform float topBlur;
uniform float bottomBlur;
uniform float darkenTopBand;
uniform float darkenBottomBand;
uniform float darkenTopOpacity;
uniform float darkenBottomOpacity;

half4 sampleContent(float2 coord) {
    float2 safeCoord = clamp(coord, float2(0.0), size - float2(1.0));
    return contents.eval(safeCoord);
}

half4 main(float2 coord) {
    float y = coord.y;
    float topAmount = 1.0 - smoothstep(0.0, 1.0, clamp(y / topBand, 0.0, 1.0));
    float bottomAmount = smoothstep(0.0, 1.0, clamp((y - (size.y - bottomBand)) / bottomBand, 0.0, 1.0));
    float radius = max(topBlur * topAmount, bottomBlur * bottomAmount);

    half4 color = half4(0.0);
    if (radius < 0.5) {
        color = sampleContent(coord);
    } else {
        float sigma = radius * 0.5;
        float twoSigmaSq = 2.0 * sigma * sigma;
        float stepPx = radius / 3.0;
        half4 sum = half4(0.0);
        float weightSum = 0.0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                float2 offset = float2(float(dx), float(dy)) * stepPx;
                float weight = exp(-(offset.x * offset.x + offset.y * offset.y) / twoSigmaSq);
                sum += sampleContent(coord + offset) * weight;
                weightSum += weight;
            }
        }
        color = sum / weightSum;
    }

    // Black gradient overlay: composite semi-transparent black over the top and bottom
    // edges (premultiplied alpha).
    float darkenTop = darkenTopOpacity * (1.0 - clamp(y / darkenTopBand, 0.0, 1.0));
    float darkenBottom = darkenBottomOpacity *
        clamp((y - (size.y - darkenBottomBand)) / darkenBottomBand, 0.0, 1.0);
    float darken = max(darkenTop, darkenBottom);
    half4 shaded = half4(color.rgb * (1.0 - darken), color.a * (1.0 - darken) + darken);

    // Dither: sub-pixel hash noise breaks up 8-bit banding so the shade gradient is smooth.
    float dither = (fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
    return shaded + half4(half3(dither), 0.0);
}
"""

/**
 * Darkens the model's top and bottom edges with a black gradient and — when [blurEnabled] —
 * also blurs those bands with a 2D gaussian, via an AGSL runtime shader. The shade always
 * applies (so it can cover Full Body too); the blur is gated so it can be limited to single
 * body parts. Band heights are fractions of the model height; blur radii come from dp
 * resources. No-op below Android 13 (API 33), where runtime shader effects are unavailable.
 */
@Composable
internal fun Modifier.modelBlur(blurEnabled: Boolean): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val density = LocalDensity.current
        val topRadius = dimensionResource(R.dimen.body_visual_model_blur_top_radius)
        val bottomRadius = dimensionResource(R.dimen.body_visual_model_blur_bottom_radius)
        val topRadiusPx = with(density) { topRadius.toPx() }
        val bottomRadiusPx = with(density) { bottomRadius.toPx() }
        val shader = remember { RuntimeShader(MODEL_BLUR_AGSL) }
        this.then(Modifier.graphicsLayer {
            if (size.minDimension <= 0f) return@graphicsLayer
            shader.setFloatUniform("size", size.width, size.height)
            // Bands scale with the model height rather than being a fixed dp.
            shader.setFloatUniform(
                "topBand",
                BodyVisualConstants.MODEL_BLUR_TOP_BAND_FRACTION * size.height,
            )
            shader.setFloatUniform(
                "bottomBand",
                BodyVisualConstants.MODEL_BLUR_BOTTOM_BAND_FRACTION * size.height,
            )
            // A zero radius makes the shader early-out of the blur loop, leaving only the
            // shade — so the blur is per-body-part while the shade always applies.
            shader.setFloatUniform("topBlur", if (blurEnabled) topRadiusPx else 0f)
            shader.setFloatUniform("bottomBlur", if (blurEnabled) bottomRadiusPx else 0f)
            shader.setFloatUniform(
                "darkenTopBand",
                BodyVisualConstants.MODEL_DARKEN_TOP_BAND_FRACTION * size.height,
            )
            shader.setFloatUniform(
                "darkenBottomBand",
                BodyVisualConstants.MODEL_DARKEN_BOTTOM_BAND_FRACTION * size.height,
            )
            shader.setFloatUniform(
                "darkenTopOpacity",
                BodyVisualConstants.MODEL_DARKEN_TOP_OPACITY,
            )
            shader.setFloatUniform(
                "darkenBottomOpacity",
                BodyVisualConstants.MODEL_DARKEN_BOTTOM_OPACITY,
            )
            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "contents")
                .asComposeRenderEffect()
        })
    } else this

private const val DITHER_AGSL = """
uniform shader contents;

half4 main(float2 coord) {
    half4 c = contents.eval(coord);
    float n = (fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
    return c + half4(half3(n), 0.0);
}
"""

/**
 * Adds sub-pixel hash dither over the content to break up 8-bit banding on smooth gradients
 * (e.g. the edge-shadow boxes). No-op below Android 13 (API 33), where runtime shader
 * effects are unavailable.
 */
@Composable
internal fun Modifier.dithered(): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val effect = remember {
            RenderEffect
                .createRuntimeShaderEffect(RuntimeShader(DITHER_AGSL), "contents")
                .asComposeRenderEffect()
        }
        this.graphicsLayer { renderEffect = effect }
    } else this
