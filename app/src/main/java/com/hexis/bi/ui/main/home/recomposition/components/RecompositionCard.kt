package com.hexis.bi.ui.main.home.recomposition.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.AppVerticalGradientDivider
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.recomposition.RecompositionCardUi
import com.hexis.bi.ui.main.home.recomposition.RecompositionMetricUi
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun RecompositionCard(
    card: RecompositionCardUi,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_l)),
    ) {
        Text(
            text = stringResource(card.window.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = NocturnePulseTheme.extendedColors.accentBlue,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
        ) {
            RecomposedCell(card, Modifier.weight(1f))

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            AppVerticalGradientDivider()
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

            WeightChangeCell(card, Modifier.weight(1f))
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        AppHorizontalGradientDivider()
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
        ) {
            MassCell(R.string.recomposition_fat_mass, card.fat, Modifier.weight(1f))
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            AppVerticalGradientDivider()
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            MassCell(R.string.recomposition_lean_mass, card.lean, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RecomposedCell(card: RecompositionCardUi, modifier: Modifier = Modifier) {
    CellColumn(modifier) {
        CellLabel(R.string.recomposition_recomposed)
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        MetricValueText(
            text = card.recomposedValue,
            recomposed = true,
            valueColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun WeightChangeCell(card: RecompositionCardUi, modifier: Modifier = Modifier) {
    CellColumn(modifier) {
        CellLabel(R.string.recomposition_weight_change)
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
        MetricValueText(text = card.weightChangeText)
        if (card.weightSubtitle.isNotBlank()) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            Text(
                text = card.weightSubtitle,
                style = TitleDimTextStyle,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun MassCell(
    @StringRes labelRes: Int,
    metric: RecompositionMetricUi,
    modifier: Modifier = Modifier,
) {
    CellColumn(modifier) {
        CellLabel(labelRes)
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
        ) {
            MetricValueText(text = metric.valueText)
            metric.favorable?.let { favorable ->
                DirectionArrow(
                    pointsUp = favorable,
                    color = recompositionTrendColor(metric.markerFraction),
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        RecompositionTrendBar(
            markerFraction = metric.markerFraction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MetricValueText(
    text: String,
    modifier: Modifier = Modifier,
    recomposed: Boolean = false,
    valueColor: Color = Color.White,
) {
    val isPlaceholder = text == stringResource(R.string.stat_unknown)
    val valueStyle = when {
        isPlaceholder -> MaterialTheme.typography.labelLarge
        recomposed -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold)
        else -> MaterialTheme.typography.labelLarge
    }
    val resolvedValueColor =
        if (isPlaceholder) NocturnePulseTheme.extendedColors.gray200 else valueColor
    val unitStyle = if (recomposed) MaterialTheme.typography.bodyLarge else TitleDimTextStyle
    Text(
        text = buildAnnotatedString {
            withStyle(valueStyle.copy(color = resolvedValueColor).toSpanStyle()) { append(text) }
            if (!isPlaceholder) withStyle(
                unitStyle.copy(color = NocturnePulseTheme.extendedColors.gray200).toSpanStyle(),
            ) {
                append(" ")
                append(stringResource(R.string.recomposition_kg_suffix).trim())
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun DirectionArrow(
    pointsUp: Boolean,
    color: Color,
) {
    Icon(
        painter = painterResource(if (pointsUp) R.drawable.ic_trend_up else R.drawable.ic_trend_down),
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
    )
}

@Composable
private fun CellColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun CellLabel(@StringRes labelRes: Int) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.bodyMedium,
        color = NocturnePulseTheme.extendedColors.gray200,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RecompositionTrendBar(
    markerFraction: Float?,
    modifier: Modifier = Modifier,
) {
    val extendedColors = NocturnePulseTheme.extendedColors
    val trackColors = listOf(
        extendedColors.proportionScaleStart,
        extendedColors.proportionScaleMid,
        extendedColors.proportionScaleEnd,
    )
    val markerColor = NocturnePulseTheme.extendedColors.textEmphasis
    val trackHeightDp = dimensionResource(R.dimen.proportion_indicator_track_height)
    val markerInsetDp = dimensionResource(R.dimen.proportion_indicator_marker_inset)
    val markerHalfWidthDp = dimensionResource(R.dimen.proportion_indicator_marker_half_width)
    val markerHeightDp = dimensionResource(R.dimen.proportion_indicator_marker_height)
    val markerGapDp = dimensionResource(R.dimen.proportion_indicator_marker_gap)
    Box(modifier = modifier.height(dimensionResource(R.dimen.proportion_indicator_height))) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.proportion_indicator_height))
        ) {
            val trackHeight = trackHeightDp.toPx()
            val trackTop = size.height - trackHeight
            val trackWidth = size.width

            if (markerFraction == null) {
                drawRoundRect(
                    color = extendedColors.gray200.copy(alpha = EMPTY_TREND_TRACK_ALPHA),
                    topLeft = Offset(0f, trackTop),
                    size = Size(trackWidth, trackHeight),
                    cornerRadius = CornerRadius(x = trackHeight / 2f, y = trackHeight / 2f),
                )
                return@Canvas
            }

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = trackColors,
                    startX = 0f,
                    endX = trackWidth
                ),
                topLeft = Offset(0f, trackTop),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(x = trackHeight / 2f, y = trackHeight / 2f),
            )

            val markerInset = markerInsetDp.toPx()
            val markerX = (trackWidth * markerFraction.coerceIn(0f, 1f))
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
}

private const val EMPTY_TREND_TRACK_ALPHA = 0.45f

@Composable
private fun recompositionTrendColor(markerFraction: Float?): Color {
    val extendedColors = NocturnePulseTheme.extendedColors
    val fraction = markerFraction?.coerceIn(0f, 1f) ?: 0.5f
    return if (fraction <= 0.5f) lerp(
        extendedColors.proportionScaleStart,
        extendedColors.proportionScaleMid,
        fraction / 0.5f,
    )
    else lerp(
        extendedColors.proportionScaleMid,
        extendedColors.proportionScaleEnd,
        (fraction - 0.5f) / 0.5f,
    )
}
