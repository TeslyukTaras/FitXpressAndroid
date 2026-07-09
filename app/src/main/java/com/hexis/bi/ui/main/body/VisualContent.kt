package com.hexis.bi.ui.main.body

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.zIndex
import androidx.core.os.ConfigurationCompat
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.main.body.components.BodyTabLayout
import com.hexis.bi.ui.main.body.components.VisualSummaryCard
import com.hexis.bi.ui.main.body.components.VisualTopArea
import com.hexis.bi.ui.main.body.components.dithered
import com.hexis.bi.utils.constants.BodyVisualConstants
import com.hexis.bi.utils.shortMonthDayFormatter
import com.hexis.bi.utils.shortMonthDayYearFormatter
import java.util.Locale

@Composable
internal fun VisualContent(
    state: VisualState,
    isMetric: Boolean,
    onBodyPartSelected: (BodyMeasurementRegion) -> Unit,
    onModeSelected: (BodyVisualMode) -> Unit,
    onScanSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    bottomClearance: Dp = Dp.Unspecified,
    showScanSelector: Boolean = true,
    onAvatarReady: () -> Unit = {},
) {
    val horizontalPadding = dimensionResource(R.dimen.padding_medium)
    val navClearance =
        if (bottomClearance.isSpecified) bottomClearance
        else dimensionResource(R.dimen.size_bottom_nav_center) +
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

    val selectedScanLabel = stringResource(
        if (state.isLatestScanSelected) R.string.body_visual_latest_scan
        else R.string.body_visual_current_scan
    )

    BodyTabLayout(
        modifier = modifier,
        navClearance = navClearance,
        cardHorizontalPadding = horizontalPadding,
        compactCard = { VisualCompactSummaryCard(state = state, isMetric = isMetric) },
    ) { visualAreaHeight, compactCardHeight, fullBodyFigureHeight ->
        val topShadowHeight =
            visualAreaHeight * BodyVisualConstants.MODEL_DARKEN_TOP_BAND_FRACTION
        val bottomShadowHeight =
            visualAreaHeight * BodyVisualConstants.MODEL_DARKEN_BOTTOM_BAND_FRACTION

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                VisualTopArea(
                    state = state,
                    selectedScanLabel = selectedScanLabel,
                    dateFormatter = fullDateFormatter,
                    modelAreaHeight = visualAreaHeight,
                    onBodyPartSelected = onBodyPartSelected,
                    onScanSelected = onScanSelected,
                    showScanSelector = showScanSelector,
                    onAvatarReady = onAvatarReady,
                    fullBodyFigureHeight = fullBodyFigureHeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(visualAreaHeight),
                )
                ModelEdgeShadow(
                    height = bottomShadowHeight,
                    colors = listOf(
                        Color.Black.copy(alpha = BodyVisualConstants.MODEL_DARKEN_BOTTOM_OPACITY),
                        Color.Transparent,
                    ),
                )
                VisualSummaryCard(
                    state = state,
                    selectedScanLabel = selectedScanLabel,
                    shortDateFormatter = shortDateFormatter,
                    isMetric = isMetric,
                    onModeSelected = onModeSelected,
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding)
                        .heightIn(min = compactCardHeight),
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.body_visual_bottom_spacer)))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topShadowHeight)
                    .offset(y = -topShadowHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = BodyVisualConstants.MODEL_DARKEN_TOP_OPACITY),
                            )
                        )
                    )
                    .dithered(),
            )
        }
    }
}

/** The reference all three tabs place their cards against; My Body has no compact card of its own. */
@Composable
internal fun VisualCompactSummaryCard(state: VisualState, isMetric: Boolean) {
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.ROOT
    val shortDateFormatter = remember(locale) { shortMonthDayFormatter(locale) }
    VisualSummaryCard(
        state = compactCardSampleState(state),
        selectedScanLabel = stringResource(R.string.body_visual_latest_scan),
        shortDateFormatter = shortDateFormatter,
        isMetric = isMetric,
        onModeSelected = {},
    )
}

/** [BodyMeasurementRegion.Bicep]'s two-line label makes it the tallest, so the card never grows. */
@Composable
private fun compactCardSampleState(state: VisualState): VisualState = remember(
    state.mode,
    state.latestScanTimestamp != null,
    state.previousScanTimestamp != null,
    state.latestMeasurements.isNotEmpty(),
    state.previousMeasurements.isNotEmpty(),
    state.beforePreviousMeasurements.isNotEmpty(),
) {
    state.copy(selectedBodyPart = BodyMeasurementRegion.Bicep)
}

@Composable
internal fun ModelEdgeShadow(
    height: Dp,
    colors: List<Color>,
) {
    Box(
        modifier = Modifier
            .zIndex(1f)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, 0) { placeable.place(0, 0) }
            }
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(colors))
            .dithered(),
    )
}

@Composable
internal fun VisualEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.body_visual_no_data),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
