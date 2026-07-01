package com.hexis.bi.ui.main.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.AppVerticalGradientDivider
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.avatar.MetricAvatarBodyRing
import com.hexis.bi.ui.avatar.MetricAvatarPreview
import com.hexis.bi.ui.avatar.MetricAvatarStatusText
import com.hexis.bi.ui.theme.MeasurementValueStyle
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.TitleHighlightTextStyle
import java.util.Locale

@Composable
internal fun MyBodyContent(
    visualState: VisualState,
    proportionState: BodyProportionState,
    cardHeightPx: Int,
    modifier: Modifier = Modifier,
    bottomClearance: Dp = Dp.Unspecified,
    onAvatarReady: () -> Unit = {},
    onInfoClick: () -> Unit = {},
) {
    val horizontalPadding = dimensionResource(R.dimen.padding_medium)
    val navClearance =
        if (bottomClearance.isSpecified) bottomClearance
        else dimensionResource(R.dimen.size_bottom_nav_center) +
                dimensionResource(R.dimen.spacer_l) +
                dimensionResource(R.dimen.spacer_l)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val cardHeight = with(density) { cardHeightPx.toDp() }
        val modelAreaHeight = (maxHeight - navClearance - cardHeight)
            .coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MyBodyModelArea(
                visualState = visualState,
                proportionState = proportionState,
                height = modelAreaHeight,
                onAvatarReady = onAvatarReady,
                modifier = Modifier.fillMaxWidth(),
            )
            BodyProportionCard(
                state = proportionState,
                onInfoClick = onInfoClick,
            )
            Spacer(Modifier.height(navClearance))
        }
    }
}

@Composable
private fun MyBodyModelArea(
    visualState: VisualState,
    proportionState: BodyProportionState,
    height: Dp,
    onAvatarReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.height(height),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        Text(
            text = stringResource(R.string.body_my_body_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        BodyProportionModel(
            visualState = visualState,
            proportionState = proportionState,
            onAvatarReady = onAvatarReady,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun BodyProportionModel(
    visualState: VisualState,
    proportionState: BodyProportionState,
    onAvatarReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modelUrl = visualState.latestModel3dUrl
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (modelUrl.isNullOrBlank()) {
            MetricAvatarStatusText(
                messageRes = R.string.body_visual_model_placeholder,
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            )
        } else {
            key(modelUrl) {
                MetricAvatarPreview(
                    modelUrl = modelUrl,
                    useModelVertexColors = false,
                    onInteractionChanged = {},
                    modifier = Modifier.fillMaxSize(),
                    drawBackground = false,
                    useGradientBackground = false,
                    touchRotationEnabled = true,
                    zoomPanEnabled = true,
                    showSkinAreas = false,
                    framingRegion = BodyMeasurementRegion.FullBody,
                    centerFraming = true,
                    baseDistanceScale = 1.08f,
                    meshGlow = 0.22f,
                    bodyRings = if (proportionState.hasData) {
                        bodyRingSpecs(proportionState)
                    } else {
                        emptyList()
                    },
                    loadingMessageRes = R.string.scan_results_avatar_loading,
                    onAvatarReady = onAvatarReady,
                )
            }
        }
    }
}

private fun bodyRingSpecs(state: BodyProportionState): List<MetricAvatarBodyRing> {
    val upper = state.groups.getOrNull(0)?.markers.orEmpty().averageProgress(default = 0.62f)
    val mid = state.groups.getOrNull(1)?.markers.orEmpty().averageProgress(default = 0.55f)
    val lower = state.groups.getOrNull(2)?.markers.orEmpty().averageProgress(default = 0.58f)
    return listOf(
        MetricAvatarBodyRing(
            region = BodyMeasurementRegion.Shoulders,
            radiusScale = lerp(1.18f, 1.28f, upper),
            verticalOffsetFraction = if (state.isFemaleProfile) 0f else -0.03f,
            fitFullCrossSection = !state.isFemaleProfile,
        ),
        MetricAvatarBodyRing(
            region = BodyMeasurementRegion.Waist,
            radiusScale = lerp(1.06f, 1.14f, mid),
        ),
        MetricAvatarBodyRing(
            region = BodyMeasurementRegion.Thigh,
            radiusScale = lerp(1.14f, 1.24f, lower),
        ),
    )
}

private fun List<BodyProportionMarker>.averageProgress(default: Float): Float =
    takeIf { it.isNotEmpty() }
        ?.map { it.progress.coerceIn(0f, 1f) }
        ?.average()
        ?.toFloat()
        ?: default

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

@Composable
private fun BodyProportionCard(
    state: BodyProportionState,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_l)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.body_proportion_card_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = stringResource(R.string.cd_info),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onInfoClick)
                    .width(dimensionResource(R.dimen.icon_medium))
                    .height(dimensionResource(R.dimen.icon_medium)),
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        if (!state.hasData) {
            Text(
                text = stringResource(R.string.body_proportion_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.groups.forEachIndexed { index, group ->
                if (index > 0) {
                    AppHorizontalGradientDivider(
                        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_l)),
                    )
                }
                BodyProportionGroup(group = group)
            }
        }
    }
}

@Composable
private fun BodyProportionGroup(group: BodyProportionGroup) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(dimensionResource(R.dimen.spacer_m))
                .height(dimensionResource(R.dimen.border_thin))
                .background(MaterialTheme.colorScheme.onBackground, CircleShape),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
        Text(
            text = stringResource(group.titleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_l)),
    ) {
        val statusInRow = group.markers.size == 1
        group.markers.forEachIndexed { index, marker ->
            if (index > 0) AppVerticalGradientDivider()
            BodyProportionMarkerBlock(
                marker = marker,
                statusInRow = statusInRow,
                modifier = Modifier.weight(1f),
            )
        }
        if (group.markers.size == 1) {
            Spacer(Modifier.width(dimensionResource(R.dimen.border_line)))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun BodyProportionMarkerBlock(
    marker: BodyProportionMarker,
    statusInRow: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = NocturnePulseTheme.extendedColors.positive
    val valueText = marker.value?.let { String.format(Locale.US, "%.2f", it) }
        ?: stringResource(R.string.body_proportion_value_empty)
    val value: @Composable (Modifier) -> Unit = { textModifier ->
        Text(
            text = valueText,
            style = MeasurementValueStyle,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = textModifier,
        )
    }
    val status: @Composable (Modifier) -> Unit = { textModifier ->
        Text(
            text = stringResource(marker.statusRes),
            style = TitleHighlightTextStyle,
            color = accent,
            modifier = textModifier,
        )
    }
    Column(modifier = modifier) {
        Text(
            text = stringResource(marker.labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        if (statusInRow) {
            Row {
                value(Modifier.alignByBaseline())
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
                status(Modifier.alignByBaseline())
            }
        } else {
            value(Modifier)
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
            status(Modifier)
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        ProportionProgressIndicator(progress = marker.progress)
    }
}

@Composable
private fun ProportionProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val extendedColors = NocturnePulseTheme.extendedColors
    val trackColors = listOf(
        extendedColors.proportionScaleStart,
        extendedColors.proportionScaleMid,
        extendedColors.proportionScaleEnd,
    )
    val markerColor = extendedColors.proportionScaleMarker
    val trackHeightDp = dimensionResource(R.dimen.proportion_indicator_track_height)
    val markerInsetDp = dimensionResource(R.dimen.proportion_indicator_marker_inset)
    val markerHalfWidthDp = dimensionResource(R.dimen.proportion_indicator_marker_half_width)
    val markerHeightDp = dimensionResource(R.dimen.proportion_indicator_marker_height)
    val markerGapDp = dimensionResource(R.dimen.proportion_indicator_marker_gap)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.proportion_indicator_height)),
    ) {
        val trackHeight = trackHeightDp.toPx()
        val trackTop = size.height - trackHeight
        val trackWidth = size.width
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = trackColors,
                startX = 0f,
                endX = trackWidth,
            ),
            topLeft = Offset(0f, trackTop),
            size = Size(trackWidth, trackHeight),
            cornerRadius = CornerRadius(x = trackHeight / 2f, y = trackHeight / 2f),
        )

        val markerInset = markerInsetDp.toPx()
        val markerX = (trackWidth * progress.coerceIn(0f, 1f))
            .coerceIn(markerInset, trackWidth - markerInset)
        val triangleHalfWidth = markerHalfWidthDp.toPx()
        val triangleHeight = markerHeightDp.toPx()
        val triangleBottom = trackTop - markerGapDp.toPx()
        val path = Path().apply {
            moveTo(markerX - triangleHalfWidth, triangleBottom - triangleHeight)
            lineTo(markerX + triangleHalfWidth, triangleBottom - triangleHeight)
            lineTo(markerX, triangleBottom)
            close()
        }
        drawPath(path = path, color = markerColor)
    }
}
