package com.hexis.bi.ui.main.body

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.dark.AppHorizontalGradientDivider
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.body.components.BodyPartHorizontalScrollList
import com.hexis.bi.ui.main.body.components.BodySegmentedToggleChip
import com.hexis.bi.ui.main.body.components.BodySegmentedToggleTrack
import com.hexis.bi.ui.main.body.components.MeasurementDateHeader
import com.hexis.bi.ui.main.body.components.MeasurementValueBlock
import com.hexis.bi.ui.main.body.components.VisualScanDateDropdown
import com.hexis.bi.ui.main.body.components.measurementValue
import com.hexis.bi.ui.main.body.components.modelBlur
import com.hexis.bi.ui.main.scan.results.CompareRotationLink
import com.hexis.bi.ui.main.scan.results.MetricAvatarLoading
import com.hexis.bi.ui.main.scan.results.MetricAvatarPreview
import com.hexis.bi.ui.main.scan.results.MetricAvatarStatusText
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.utils.constants.BodyVisualConstants
import com.hexis.bi.utils.constants.BodyVisualConstants.FULL_BODY_MEASUREMENT_ROWS
import com.hexis.bi.utils.shortMonthDayFormatter
import com.hexis.bi.utils.shortMonthDayYearFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CompareContent(
    state: CompareState,
    cardHeightPx: Int,
    isMetric: Boolean,
    onSelectLeftScan: (Long) -> Unit,
    onSelectRightScan: (Long) -> Unit,
    onModeSelected: (BodyVisualMode) -> Unit,
    onBodyPartSelected: (BodyMeasurementRegion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = dimensionResource(R.dimen.padding_medium)
    val navClearance =
        dimensionResource(R.dimen.size_bottom_nav_center) +
                dimensionResource(R.dimen.spacer_l) +
                dimensionResource(R.dimen.spacer_l)

    if (!state.hasData) {
        VisualEmptyState(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    horizontal = horizontalPadding,
                    vertical = dimensionResource(R.dimen.spacer_2xl),
                ),
        )
        return
    }

    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.ROOT
    val fullDateFormatter = remember(locale) { shortMonthDayYearFormatter(locale) }
    val shortDateFormatter = remember(locale) { shortMonthDayFormatter(locale) }
    val usesLatestAndPreviousScans = state.usesLatestAndPreviousScans()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val cardHeight = with(density) { cardHeightPx.toDp() }
        val selectorHeight = dimensionResource(R.dimen.body_part_selector_horizontal_height)
        val cardTop = maxHeight - navClearance - cardHeight
        val modelAreaHeight = (cardTop - selectorHeight).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            CompareTopArea(
                state = state,
                usesLatestAndPreviousScans = usesLatestAndPreviousScans,
                modelAreaHeight = modelAreaHeight,
                dateFormatter = fullDateFormatter,
                onSelectLeftScan = onSelectLeftScan,
                onSelectRightScan = onSelectRightScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(modelAreaHeight),
            )
            BodyPartHorizontalScrollList(
                selected = state.selectedBodyPart,
                onSelect = onBodyPartSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(selectorHeight),
            )
            CompareSummaryCard(
                state = state,
                usesLatestAndPreviousScans = usesLatestAndPreviousScans,
                shortDateFormatter = shortDateFormatter,
                isMetric = isMetric,
                onModeSelected = onModeSelected,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.body_visual_bottom_spacer)))
        }
    }
}

@Composable
private fun CompareTopArea(
    state: CompareState,
    usesLatestAndPreviousScans: Boolean,
    modelAreaHeight: Dp,
    dateFormatter: SimpleDateFormat,
    onSelectLeftScan: (Long) -> Unit,
    onSelectRightScan: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier) {
            CompareModelsPanel(
                state = state,
                framingRegion = state.selectedBodyPart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(modelAreaHeight)
                    .align(Alignment.TopCenter),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            ) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
                Text(
                    text = stringResource(
                        if (usesLatestAndPreviousScans) R.string.body_compare_subtitle_latest
                        else R.string.body_compare_subtitle,
                    ),
                    style = TitleDimTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
                Row(modifier = Modifier.fillMaxWidth()) {
                    VisualScanDateDropdown(
                        label = "",
                        selectedTimestamp = state.leftScanTimestamp,
                        options = state.scanOptions,
                        dateFormatter = dateFormatter,
                        onScanSelected = onSelectLeftScan,
                        modifier = Modifier.weight(1f),
                    )
                    VisualScanDateDropdown(
                        label = "",
                        selectedTimestamp = state.rightScanTimestamp,
                        options = state.scanOptions,
                        dateFormatter = dateFormatter,
                        onScanSelected = onSelectRightScan,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareModelsPanel(
    state: CompareState,
    framingRegion: BodyMeasurementRegion,
    modifier: Modifier = Modifier,
) {
    val rotationLink =
        remember(state.leftModel3dUrl, state.rightModel3dUrl) { CompareRotationLink() }
    val leftTs = state.leftScanTimestamp
    val rightTs = state.rightScanTimestamp
    val leftIsCurrent = leftTs != null && (rightTs == null || leftTs >= rightTs)
    val rightIsCurrent = rightTs != null && (leftTs == null || rightTs > leftTs)
    Row(modifier = modifier) {
        CompareModelColumn(
            baseModelUrl = state.leftModel3dUrl,
            colorModel = state.leftColorModel,
            mode = state.mode,
            rotationLink = rotationLink,
            framingRegion = framingRegion,
            meshGlow = if (leftIsCurrent) BodyVisualConstants.CURRENT_SCAN_MESH_GLOW else 0f,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        AppVerticalGradientDivider(
            modifier = Modifier.padding(
                top = dimensionResource(R.dimen.spacer_6xl),
                bottom = dimensionResource(R.dimen.spacer_xl)
            )
        )
        CompareModelColumn(
            baseModelUrl = state.rightModel3dUrl,
            colorModel = state.rightColorModel,
            mode = state.mode,
            rotationLink = rotationLink,
            framingRegion = framingRegion,
            meshGlow = if (rightIsCurrent) BodyVisualConstants.CURRENT_SCAN_MESH_GLOW else 0f,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun CompareModelColumn(
    baseModelUrl: String?,
    colorModel: BodyVisualColorModel,
    mode: BodyVisualMode,
    rotationLink: CompareRotationLink,
    framingRegion: BodyMeasurementRegion,
    meshGlow: Float,
    modifier: Modifier = Modifier,
) {
    val readyColor = colorModel as? BodyVisualColorModel.Ready
    val showColor = mode == BodyVisualMode.Color && readyColor != null
    val modelUrl = if (showColor) readyColor.coloredModelUrl else baseModelUrl

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .modelBlur(
                blurEnabled = true,
                topBlurBandFraction = BodyVisualConstants.COMPARE_MODEL_BLUR_TOP_BAND_FRACTION,
                darkenTopOpacity = 0f,
                darkenBottomOpacity = 0f,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            mode == BodyVisualMode.Color && readyColor == null -> when (colorModel) {
                BodyVisualColorModel.Idle,
                BodyVisualColorModel.Loading -> MetricAvatarLoading(
                    messageRes = R.string.body_visual_color_loading,
                    modifier = Modifier.fillMaxSize(),
                )

                BodyVisualColorModel.Unavailable -> MetricAvatarStatusText(
                    messageRes = R.string.body_visual_color_unavailable,
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
                )
                BodyVisualColorModel.Error -> MetricAvatarStatusText(
                    messageRes = R.string.body_visual_color_error,
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
                )
                is BodyVisualColorModel.Ready -> Unit
            }

            modelUrl.isNullOrBlank() -> MetricAvatarStatusText(
                messageRes = R.string.body_visual_model_placeholder,
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            )

            else -> key(modelUrl) {
                MetricAvatarPreview(
                    modelUrl = modelUrl,
                    useModelVertexColors = showColor,
                    onInteractionChanged = {},
                    modifier = Modifier.fillMaxSize(),
                    drawBackground = false,
                    useGradientBackground = false,
                    compareRotationLink = rotationLink,
                    zoomPanEnabled = true,
                    framingRegion = framingRegion,
                    centerFraming = true,
                    baseDistanceScale = BodyVisualConstants.COMPARE_MODEL_DISTANCE_SCALE,
                    meshGlow = meshGlow,
                    loadingMessageRes = if (showColor) R.string.body_visual_color_loading
                    else R.string.scan_results_avatar_loading,
                )
            }
        }
    }
}

@Composable
private fun CompareSummaryCard(
    state: CompareState,
    usesLatestAndPreviousScans: Boolean,
    shortDateFormatter: SimpleDateFormat,
    isMetric: Boolean,
    onModeSelected: (BodyVisualMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_l)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.body_visual_part_bullet),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
            Text(
                text = stringResource(BodyVisualConstants.visualHeaderLabelRes(state.selectedBodyPart)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            BodySegmentedToggleTrack {
                BodySegmentedToggleChip(
                    label = stringResource(R.string.body_visual_mode_base),
                    isSelected = state.mode == BodyVisualMode.Base,
                    onClick = { onModeSelected(BodyVisualMode.Base) },
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
                BodySegmentedToggleChip(
                    label = stringResource(R.string.body_visual_mode_color),
                    isSelected = state.mode == BodyVisualMode.Color,
                    onClick = { onModeSelected(BodyVisualMode.Color) },
                )
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        val leftLabelRes = if (usesLatestAndPreviousScans) {
            if (state.leftScanTimestamp == state.scanOptions.first().timestamp) {
                R.string.body_visual_latest_scan
            } else {
                R.string.body_visual_previous_scan
            }
        } else {
            R.string.body_compare_left_scan
        }
        val rightLabelRes = if (usesLatestAndPreviousScans) {
            if (state.rightScanTimestamp == state.scanOptions.first().timestamp) {
                R.string.body_visual_latest_scan
            } else {
                R.string.body_visual_previous_scan
            }
        } else {
            R.string.body_compare_right_scan
        }
        MeasurementDateHeader(
            leftLabel = stringResource(leftLabelRes),
            leftDate = state.leftScanTimestamp?.let { shortDateFormatter.format(Date(it)) },
            rightLabel = stringResource(rightLabelRes),
            rightDate = state.rightScanTimestamp?.let { shortDateFormatter.format(Date(it)) },
        )

        val rows = if (state.selectedBodyPart == BodyMeasurementRegion.FullBody) {
            FULL_BODY_MEASUREMENT_ROWS
        } else {
            FULL_BODY_MEASUREMENT_ROWS.filter { it.region == state.selectedBodyPart }
        }
        rows.forEachIndexed { index, row ->
            if (index > 0) AppHorizontalGradientDivider(
                modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_m)),
            )
            else Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            CompareMeasurementRow(
                region = row.region,
                label = stringResource(row.labelRes),
                leftCm = measurementValue(state.leftMeasurements, row.region),
                leftPreviousCm = measurementValue(state.leftPreviousMeasurements, row.region),
                rightCm = measurementValue(state.rightMeasurements, row.region),
                rightPreviousCm = measurementValue(state.rightPreviousMeasurements, row.region),
                isMetric = isMetric,
            )
        }
    }
}

private fun CompareState.usesLatestAndPreviousScans(): Boolean {
    val latest = scanOptions.firstOrNull()?.timestamp ?: return false
    val previous = scanOptions.getOrNull(1)?.timestamp ?: return false
    return (leftScanTimestamp == latest && rightScanTimestamp == previous) ||
            (leftScanTimestamp == previous && rightScanTimestamp == latest)
}

@Composable
private fun CompareMeasurementRow(
    region: BodyMeasurementRegion,
    label: String,
    leftCm: Float?,
    leftPreviousCm: Float?,
    rightCm: Float?,
    rightPreviousCm: Float?,
    isMetric: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.body_visual_part_bullet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            MeasurementValueBlock(
                valueCm = leftCm,
                deltaCm = if (leftCm != null && leftPreviousCm != null) leftCm - leftPreviousCm else null,
                isMetric = isMetric,
                decreaseIsPositive = region.decreaseIsPositive,
                modifier = Modifier.weight(1f),
                hideValue = true,
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            AppVerticalGradientDivider()
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

            MeasurementValueBlock(
                valueCm = rightCm,
                deltaCm = if (rightCm != null && rightPreviousCm != null) rightCm - rightPreviousCm else null,
                isMetric = isMetric,
                decreaseIsPositive = region.decreaseIsPositive,
                modifier = Modifier.weight(1f),
                hideValue = true,
            )
        }
    }
}
