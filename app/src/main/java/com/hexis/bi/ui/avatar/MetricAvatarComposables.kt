package com.hexis.bi.ui.avatar

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Radial preview backdrop behind the GL mesh — uses [@color/metric_avatar_preview_gradient_inner] /
 * [@color/metric_avatar_preview_gradient_outer] (same as [MetricAvatarRenderer] GL uniforms).
 */
@Composable
internal fun Modifier.metricAvatarPreviewGradientBackground(): Modifier {
    val inner = colorResource(R.color.metric_avatar_preview_gradient_inner)
    val outer = colorResource(R.color.metric_avatar_preview_gradient_outer)
    val brush = remember(inner, outer) {
        Brush.radialGradient(
            colors = listOf(inner, outer),
            center = Offset.Unspecified,
            radius = Float.POSITIVE_INFINITY,
        )
    }
    return this.background(brush)
}

@Composable
internal fun MetricAvatarPreview(
    modifier: Modifier = Modifier,
    modelUrl: String,
    useModelVertexColors: Boolean = false,
    onInteractionChanged: (Boolean) -> Unit,
    showSkinAreas: Boolean = false,
    drawBackground: Boolean = true,
    touchRotationEnabled: Boolean = true,
    yawOnlyRotation: Boolean = false,
    zoomPanEnabled: Boolean = false,
    /** Bounding-box height for the full-body figure; the renderer solves the distance from it. */
    fullBodyFigureHeight: Dp = Dp.Unspecified,
    fullBodyCenterY: Float = DEFAULT_FULL_BODY_CENTER_Y,
    meshGlow: Float = 0f,
    /** When false, the parent already drew [metricAvatarPreviewGradientBackground]. */
    useGradientBackground: Boolean = true,
    initialYawDegrees: Float = 0f,
    initialPitchDegrees: Float = INITIAL_PITCH_DEG,
    compareRotationLink: CompareRotationLink? = null,
    /** Invoked on the main thread when the mesh is loaded and the GL surface is visible (opaque). */
    onAvatarReady: (() -> Unit)? = null,
    onZoomChanged: ((Float) -> Unit)? = null,
    /** Visual tab: model-space auto-framing region. Null keeps the default camera/orientation behavior. */
    framingRegion: BodyMeasurementRegion? = null,
    /** When true, framing keeps the figure horizontally centered (Compare) instead of right-shifted (Visual). */
    centerFraming: Boolean = false,
    bodyRings: List<MetricAvatarBodyRing> = emptyList(),
    @StringRes loadingMessageRes: Int = R.string.scan_results_avatar_loading,
) {
    val context = LocalContext.current
    val latestOnInteraction by rememberUpdatedState(onInteractionChanged)
    val latestOnAvatarReady by rememberUpdatedState(onAvatarReady)
    val latestOnZoomChanged by rememberUpdatedState(onZoomChanged)
    val renderHost = remember(context) {
        MetricAvatarTextureView(context) { latestOnInteraction(it) }
    }
    var loadState by remember { mutableStateOf(MetricAvatarLoadState.Loading) }
    // Wait for a rendered frame to avoid a white flash on model switches.
    var firstFrameReady by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val latestRenderFailure by rememberUpdatedState {
        loadState = MetricAvatarLoadState.Error
    }

    LaunchedEffect(renderHost, showSkinAreas) { renderHost.setShowSkinAreas(showSkinAreas) }
    LaunchedEffect(renderHost, drawBackground) { renderHost.setDrawBackground(drawBackground) }
    LaunchedEffect(renderHost, touchRotationEnabled) {
        renderHost.setTouchRotationEnabled(
            touchRotationEnabled
        )
    }
    LaunchedEffect(renderHost, yawOnlyRotation) { renderHost.setYawOnlyRotation(yawOnlyRotation) }
    LaunchedEffect(renderHost, zoomPanEnabled) { renderHost.setZoomPanEnabled(zoomPanEnabled) }
    val density = LocalDensity.current
    val fullBodyFigureHeightPx = remember(density, fullBodyFigureHeight) {
        if (fullBodyFigureHeight.isSpecified) with(density) { fullBodyFigureHeight.toPx() } else 0f
    }
    LaunchedEffect(renderHost, fullBodyFigureHeightPx) {
        renderHost.setFullBodyFigureHeightPx(fullBodyFigureHeightPx)
    }
    LaunchedEffect(renderHost, meshGlow) { renderHost.setMeshGlow(meshGlow) }
    LaunchedEffect(renderHost, initialPitchDegrees) { renderHost.setFramePitch(initialPitchDegrees) }
    LaunchedEffect(renderHost, fullBodyCenterY) { renderHost.setFullBodyCenterY(fullBodyCenterY) }
    LaunchedEffect(renderHost, framingRegion) { renderHost.setFramingRegion(framingRegion) }
    LaunchedEffect(renderHost, centerFraming) { renderHost.setCenterFraming(centerFraming) }
    LaunchedEffect(renderHost, bodyRings) { renderHost.setBodyRings(bodyRings) }

    DisposableEffect(renderHost) {
        renderHost.setZoomLevelListener { latestOnZoomChanged?.invoke(it) }
        renderHost.setRenderFailureListener { latestRenderFailure() }
        renderHost.setOnFirstFrameRendered { firstFrameReady = true }
        renderHost.onResumeHost()
        onDispose {
            renderHost.setZoomLevelListener(null)
            renderHost.setRenderFailureListener(null)
            renderHost.setOnFirstFrameRendered(null)
            renderHost.onPauseHost()
        }
    }

    DisposableEffect(renderHost, compareRotationLink) {
        renderHost.setCompareRotationLink(compareRotationLink)
        onDispose { renderHost.setCompareRotationLink(null) }
    }

    LaunchedEffect(firstFrameReady, modelUrl, useModelVertexColors, reloadKey) {
        if (firstFrameReady) {
            latestOnAvatarReady?.invoke()
        }
    }

    val latestYawAtLoad by rememberUpdatedState(initialYawDegrees)
    val latestPitchAtLoad by rememberUpdatedState(initialPitchDegrees)

    LaunchedEffect(modelUrl, useModelVertexColors, reloadKey, compareRotationLink) {
        compareRotationLink?.let { renderHost.setCompareRotationLink(it) }
        loadState = MetricAvatarLoadState.Loading
        firstFrameReady = false
        try {
            val cacheDir = context.cacheDir
            val mesh = withContext(Dispatchers.IO) {
                ObjParser.parse(modelUrl, cacheDir, includeVertexColors = useModelVertexColors)
            }
            if (compareRotationLink != null) {
                renderHost.applyLoadedMeshForCompare(mesh)
            } else {
                renderHost.applyLoadedMesh(mesh, latestYawAtLoad, latestPitchAtLoad)
            }
            loadState = MetricAvatarLoadState.Ready
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(
                e,
                "Metric avatar OBJ load failed model=%s useColors=%s",
                modelUrl,
                useModelVertexColors
            )
            loadState = MetricAvatarLoadState.Error
        }
    }

    LaunchedEffect(
        initialYawDegrees,
        initialPitchDegrees,
        loadState,
        compareRotationLink,
        framingRegion
    ) {
        if (loadState != MetricAvatarLoadState.Ready || compareRotationLink != null) return@LaunchedEffect
        if (framingRegion != null) return@LaunchedEffect
        renderHost.setBaseOrientation(initialYawDegrees, initialPitchDegrees)
    }

    val containerModifier = if (useGradientBackground && drawBackground) {
        modifier.metricAvatarPreviewGradientBackground()
    } else {
        modifier
    }
    Box(modifier = containerModifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .graphicsLayer {
                    alpha = if (firstFrameReady) 1f else 0f
                    // Keep GL layer composited so the surface still lays out while loading (avoids 0×0 / stuck first frame on some devices).
                    compositingStrategy = CompositingStrategy.Auto
                },
            factory = { renderHost.view },
        )
        when {
            loadState == MetricAvatarLoadState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarRetryButton(onClick = { reloadKey++ })
                }
            }

            !firstFrameReady -> {
                MetricAvatarLoading(
                    messageRes = loadingMessageRes,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

internal data class MetricAvatarBodyRing(
    val region: BodyMeasurementRegion,
    val radiusScale: Float = 1f,
    val verticalOffsetFraction: Float = 0f,
    val fitFullCrossSection: Boolean = false,
)

@Composable
internal fun MetricAvatarLoading(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            MetricAvatarStatusText(messageRes = messageRes)
        }
    }
}

@Composable
internal fun MetricAvatarStatusText(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.widthIn(
            max = dimensionResource(R.dimen.metric_avatar_text_max_width),
        ),
    )
}

internal suspend fun prefetchMetricAvatarModel(
    context: Context,
    modelUrl: String,
    includeVertexColors: Boolean = false,
) {
    withContext(Dispatchers.IO) {
        ObjParser.parse(modelUrl, context.cacheDir, includeVertexColors)
    }
}

@Composable
private fun AvatarRetryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = dimensionResource(R.dimen.border_line),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacer_l),
            vertical = dimensionResource(R.dimen.spacer_xxs),
        ),
        modifier = modifier
            .height(dimensionResource(R.dimen.metric_avatar_retry_button_height)),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.metric_avatar_retry_icon_size)),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
        Text(
            text = stringResource(R.string.action_retry),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private enum class MetricAvatarLoadState {
    Loading,
    Ready,
    Error,
}
