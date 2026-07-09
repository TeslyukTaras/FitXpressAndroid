package com.hexis.bi.ui.main.body.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.utils.constants.BodyVisualConstants
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect

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

    float darkenTop = darkenTopOpacity *
        (1.0 - smoothstep(0.0, 1.0, clamp(y / darkenTopBand, 0.0, 1.0)));
    float darkenBottom = darkenBottomOpacity *
        smoothstep(0.0, 1.0, clamp((y - (size.y - darkenBottomBand)) / darkenBottomBand, 0.0, 1.0));
    float darken = max(darkenTop, darkenBottom);
    half4 shaded = half4(color.rgb * (1.0 - darken), color.a * (1.0 - darken) + darken);

    float dither = (fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
    return shaded + half4(half3(dither), 0.0);
}
"""

@Composable
internal fun rememberModelBlurProgress(zoomLevel: State<Float>): State<Float> =
    remember(zoomLevel) {
        derivedStateOf {
            val span = BodyVisualConstants.MODEL_BLUR_ZOOM_MAX -
                    BodyVisualConstants.MODEL_BLUR_ZOOM_START
            ((zoomLevel.value - BodyVisualConstants.MODEL_BLUR_ZOOM_START) / span)
                .coerceIn(0f, 1f)
        }
    }

@Composable
internal fun Modifier.modelBlur(
    blurProgress: State<Float>,
    topBlurBandFraction: Float = BodyVisualConstants.MODEL_BLUR_TOP_BAND_FRACTION,
    bottomBlurBandFraction: Float = BodyVisualConstants.MODEL_BLUR_BOTTOM_BAND_FRACTION,
    darkenTopOpacity: Float = BodyVisualConstants.MODEL_DARKEN_TOP_OPACITY,
    darkenBottomOpacity: Float = BodyVisualConstants.MODEL_DARKEN_BOTTOM_OPACITY,
): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val density = LocalDensity.current
        val topRadiusPx = with(density) {
            dimensionResource(R.dimen.body_visual_model_blur_top_radius).toPx()
        }
        val bottomRadiusPx = with(density) {
            dimensionResource(R.dimen.body_visual_model_blur_bottom_radius).toPx()
        }
        val effects = remember { ModelBlurEffectCache() }
        // The block is remembered and reads the progress itself: a lambda rebuilt per recomposition
        // is a new modifier value, and this layer wraps the GL TextureView.
        val block: GraphicsLayerScope.() -> Unit = remember(
            effects, blurProgress, topBlurBandFraction, bottomBlurBandFraction,
            darkenTopOpacity, darkenBottomOpacity, topRadiusPx, bottomRadiusPx,
        ) {
            {
                if (size.minDimension > 0f) {
                    val progress = blurProgress.value
                    renderEffect = effects.effectFor(
                        size = size,
                        topBlurPx = topRadiusPx * progress,
                        bottomBlurPx = bottomRadiusPx * progress,
                        topBlurBandFraction = topBlurBandFraction,
                        bottomBlurBandFraction = bottomBlurBandFraction,
                        darkenTopOpacity = darkenTopOpacity,
                        darkenBottomOpacity = darkenBottomOpacity,
                    )
                }
            }
        }
        this.then(Modifier.graphicsLayer(block))
    } else this

/** Rebuilds the shader effect only when a uniform actually changes — the layer size, mostly. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class ModelBlurEffectCache {
    private val shader = RuntimeShader(MODEL_BLUR_AGSL)
    private var key: List<Float>? = null
    private var effect: ComposeRenderEffect? = null

    fun effectFor(
        size: Size,
        topBlurPx: Float,
        bottomBlurPx: Float,
        topBlurBandFraction: Float,
        bottomBlurBandFraction: Float,
        darkenTopOpacity: Float,
        darkenBottomOpacity: Float,
    ): ComposeRenderEffect {
        val next = listOf(
            size.width, size.height, topBlurPx, bottomBlurPx,
            topBlurBandFraction, bottomBlurBandFraction, darkenTopOpacity, darkenBottomOpacity,
        )
        effect?.let { if (key == next) return it }
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("topBand", topBlurBandFraction * size.height)
        shader.setFloatUniform("bottomBand", bottomBlurBandFraction * size.height)
        shader.setFloatUniform("topBlur", topBlurPx)
        shader.setFloatUniform("bottomBlur", bottomBlurPx)
        shader.setFloatUniform(
            "darkenTopBand",
            BodyVisualConstants.MODEL_DARKEN_TOP_BAND_FRACTION * size.height,
        )
        shader.setFloatUniform(
            "darkenBottomBand",
            BodyVisualConstants.MODEL_DARKEN_BOTTOM_BAND_FRACTION * size.height,
        )
        shader.setFloatUniform("darkenTopOpacity", darkenTopOpacity)
        shader.setFloatUniform("darkenBottomOpacity", darkenBottomOpacity)
        return RenderEffect.createRuntimeShaderEffect(shader, "contents")
            .asComposeRenderEffect()
            .also { key = next; effect = it }
    }
}

private const val DITHER_AGSL = """
uniform shader contents;

half4 main(float2 coord) {
    half4 c = contents.eval(coord);
    float n = (fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
    return c + half4(half3(n), 0.0);
}
"""

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
