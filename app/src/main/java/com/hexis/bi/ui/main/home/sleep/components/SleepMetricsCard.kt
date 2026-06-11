package com.hexis.bi.ui.main.home.sleep.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.home.sleep.ChartPoint
import com.hexis.bi.ui.theme.Gray200
import com.hexis.bi.ui.theme.TitleHighlightTextStyle
import com.hexis.bi.ui.theme.dark.ChartGridLineHorizontal
import com.hexis.bi.ui.theme.dark.ChartGridLineVertical
import com.hexis.bi.ui.theme.dark.ChartHrvLine
import com.hexis.bi.ui.theme.dark.ChartTeal
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.formatHour
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun SleepMetricsCard(
    hrv: Int,
    restingHeartRate: Int,
    hrvSeries: List<ChartPoint>,
    rhrSeries: List<ChartPoint>,
    timeStartHour: Int,
    timeEndHour: Int,
    modifier: Modifier = Modifier,
) {
    val rhrColor = DarkTheme.extendedColors.accentBlue
    val axis = remember(hrvSeries, rhrSeries) { axisFor(hrvSeries, rhrSeries) }
    BodyGlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sleep_heart_metrics_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.sleep_last_night),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.sleep_metric_hrv_unit),
                style = TitleHighlightTextStyle,
                color = ChartHrvLine,
            )
            Text(
                text = stringResource(R.string.sleep_metric_rhr_unit),
                style = TitleHighlightTextStyle,
                color = rhrColor,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Row(modifier = Modifier.fillMaxWidth()) {
            ChartAxisLabels(
                values = axis?.labels.orEmpty(),
                color = ChartHrvLine,
                modifier = Modifier
                    .height(dimensionResource(R.dimen.sleep_heart_chart_height))
                    .padding(end = dimensionResource(R.dimen.spacer_xs)),
            )
            Column(modifier = Modifier.weight(1f)) {
                HeartMetricsChart(
                    hrvSeries = hrvSeries,
                    rhrSeries = rhrSeries,
                    range = axis?.range,
                    rhrColor = rhrColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.sleep_heart_chart_height)),
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

                TimelineLabels(
                    timeStartHour = timeStartHour,
                    timeEndHour = timeEndHour,
                )
            }
            ChartAxisLabels(
                values = axis?.labels.orEmpty(),
                color = rhrColor,
                alignEnd = true,
                modifier = Modifier
                    .height(dimensionResource(R.dimen.sleep_heart_chart_height))
                    .padding(start = dimensionResource(R.dimen.spacer_xs)),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeartMetricValue(
                label = stringResource(R.string.sleep_metric_hrv),
                value = hrv.coerceAtLeast(0),
                unit = stringResource(R.string.unit_ms),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            AppVerticalGradientDivider()
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            HeartMetricValue(
                label = stringResource(R.string.sleep_metric_rhr_short),
                value = restingHeartRate.coerceAtLeast(0),
                unit = stringResource(R.string.unit_bpm),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeartMetricsChart(
    hrvSeries: List<ChartPoint>,
    rhrSeries: List<ChartPoint>,
    range: ClosedFloatingPointRange<Float>?,
    rhrColor: Color,
    modifier: Modifier = Modifier,
) {
    val gridWidth = dimensionResource(R.dimen.sleep_heart_grid_width)
    val gridDashLength = dimensionResource(R.dimen.sleep_heart_grid_dash)
    val lineWidth = dimensionResource(R.dimen.sleep_heart_line_width)
    val lineCorner = dimensionResource(R.dimen.sleep_heart_line_corner)
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val gridStroke = gridWidth.toPx()
            val dashLength = gridDashLength.toPx()
            val gridDash = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashLength), 0f)
            val lineStroke = Stroke(
                width = lineWidth.toPx(),
                pathEffect = PathEffect.cornerPathEffect(lineCorner.toPx()),
            )
            repeat(AXIS_TICKS + 1) { index ->
                val y = size.height * index / AXIS_TICKS.toFloat()
                // The bottom line is the x-axis baseline: solid, the rest dashed.
                val effect = if (index == AXIS_TICKS) null else gridDash
                drawLine(
                    ChartGridLineHorizontal, Offset(0f, y), Offset(size.width, y),
                    gridStroke, pathEffect = effect,
                )
            }
            repeat(AXIS_TICKS + 1) { index ->
                val x = size.width * index / AXIS_TICKS.toFloat()
                drawLine(
                    ChartGridLineVertical, Offset(x, 0f), Offset(x, size.height),
                    gridStroke, pathEffect = gridDash,
                )
            }

            val hrvBrush = Brush.verticalGradient(
                colorStops = cssGradientStops(
                    start = ChartTeal.copy(alpha = HRV_FILL_ALPHA), startStop = HRV_FILL_START_STOP,
                    end = HrvGradientEnd, endStop = HRV_FILL_END_STOP,
                ),
                startY = 0f,
                endY = size.height,
            )
            val rhrBrush = Brush.verticalGradient(
                colorStops = cssGradientStops(
                    start = rhrColor.copy(alpha = RHR_FILL_ALPHA), startStop = 0f,
                    end = Color.Transparent, endStop = 1f,
                ),
                startY = 0f,
                endY = size.height,
            )

            if (range != null) {
                drawSeries(hrvSeries, range, ChartHrvLine, hrvBrush, lineStroke)
                drawSeries(rhrSeries, range, rhrColor, rhrBrush, lineStroke)
            }
        }
    }
}

/** Draws one metric line as straight segments through [series], with its gradient area fill. */
private fun DrawScope.drawSeries(
    series: List<ChartPoint>,
    range: ClosedFloatingPointRange<Float>,
    lineColor: Color,
    areaBrush: Brush,
    lineStroke: Stroke,
) {
    if (series.size < 2) return

    fun xFor(fraction: Float) = fraction.coerceIn(0f, 1f) * size.width
    fun yFor(value: Int): Float {
        val normalized =
            ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        return size.height - normalized * size.height
    }

    val line = Path()
    series.forEachIndexed { index, point ->
        val x = xFor(point.fraction)
        val y = yFor(point.value)
        if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
    }

    val area = Path().apply {
        addPath(line)
        lineTo(xFor(series.last().fraction), size.height)
        lineTo(xFor(series.first().fraction), size.height)
        close()
    }

    drawPath(path = area, brush = areaBrush)
    drawPath(path = line, color = lineColor, style = lineStroke)
}

private const val AXIS_TICKS = 4 // intervals; the axis and grid show AXIS_TICKS + 1 lines/labels
private const val HOURS_PER_DAY = 24

// HRV/RHR area-fill gradients (sRGB stops sampled from the Figma linear-gradients).
private const val HRV_FILL_ALPHA = 0.48f
private const val HRV_FILL_START_STOP = 0.2231f
private const val HRV_FILL_END_STOP = 0.9953f
private const val RHR_FILL_ALPHA = 0.5f
private const val GRADIENT_END_GRAY = 60f / 255f

/** A chart axis fitted to the data: its [range] for plotting and its 5 [labels] top-to-bottom. */
private data class ChartAxis(
    val range: ClosedFloatingPointRange<Float>,
    val labels: List<Int>,
)

/** Fits one nicely-rounded axis covering every plottable [series], or null when nothing plots. */
private fun axisFor(vararg series: List<ChartPoint>): ChartAxis? {
    val values = series.filter { it.size >= 2 }.flatMap { points -> points.map { it.value } }
    if (values.isEmpty()) return null
    val min = values.min()
    val max = values.max()
    val step = niceStep((max - min) / AXIS_TICKS.toFloat())
    val niceMin = floor(min / step) * step
    val niceMax = niceMin + step * AXIS_TICKS
    val labels = List(AXIS_TICKS + 1) { i -> (niceMax - step * i).roundToInt() }
    return ChartAxis(range = niceMin..niceMax, labels = labels)
}

/** Rounds a rough tick interval up to the nearest 1·2·5 × 10ⁿ so labels read cleanly. */
private fun niceStep(rough: Float): Float {
    val value = rough.coerceAtLeast(1f)
    val pow = 10f.pow(floor(log10(value)))
    val fraction = value / pow
    val niceFraction = when {
        fraction <= 1f -> 1f
        fraction <= 2f -> 2f
        fraction <= 5f -> 5f
        else -> 10f
    }
    return niceFraction * pow
}

private val HrvGradientEnd =
    Color(red = GRADIENT_END_GRAY, green = GRADIENT_END_GRAY, blue = GRADIENT_END_GRAY, alpha = 0f)

private const val GRADIENT_SAMPLES = 8

/** Samples a gradient into sRGB stops so the fade desaturates toward the end colour. */
private fun cssGradientStops(
    start: Color,
    startStop: Float,
    end: Color,
    endStop: Float,
): Array<Pair<Float, Color>> {
    val stops = mutableListOf<Pair<Float, Color>>()
    if (startStop > 0f) stops += 0f to start
    repeat(GRADIENT_SAMPLES + 1) { i ->
        val t = i / GRADIENT_SAMPLES.toFloat()
        stops += (startStop + (endStop - startStop) * t) to Color(
            red = start.red + (end.red - start.red) * t,
            green = start.green + (end.green - start.green) * t,
            blue = start.blue + (end.blue - start.blue) * t,
            alpha = start.alpha + (end.alpha - start.alpha) * t,
        )
    }
    if (endStop < 1f) stops += 1f to end
    return stops.toTypedArray()
}

/** Y-axis labels, each vertically centered on its horizontal gridline (rows at i/4 of the height). */
@Composable
private fun ChartAxisLabels(
    values: List<Int>,
    color: Color,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Layout(
        modifier = modifier,
        content = {
            values.forEach { value ->
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                )
            }
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }
        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = constraints.maxHeight
        val lastIndex = (placeables.size - 1).coerceAtLeast(1)
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val centerY = height.toLong() * index / lastIndex
                val x = if (alignEnd) width - placeable.width else 0
                placeable.place(x, centerY.toInt() - placeable.height / 2)
            }
        }
    }
}

@Composable
private fun TimelineLabels(
    timeStartHour: Int,
    timeEndHour: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        timelineLabelHours(timeStartHour, timeEndHour).forEach { hour ->
            Text(
                text = formatHour(hour),
                style = MaterialTheme.typography.labelSmall,
                color = Gray200,
            )
        }
    }
}

@Composable
private fun HeartMetricValue(
    label: String,
    value: Int,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        Text(text = heartMetricValueText(value, unit))
    }
}

@Composable
private fun heartMetricValueText(value: Int, unit: String): AnnotatedString {
    val numberStyle =
        MaterialTheme.typography.bodyLarge.toSpanStyle()
            .copy(color = MaterialTheme.colorScheme.onSurface)
    val unitStyle =
        MaterialTheme.typography.bodyMedium.toSpanStyle()
            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    return buildAnnotatedString {
        withStyle(numberStyle) { append(value.toString()) }
        withStyle(unitStyle) { append(" $unit") }
    }
}

private fun timelineLabelHours(startHour: Int, endHour: Int): List<Int> {
    val normalizedEnd = if (endHour <= startHour) endHour + HOURS_PER_DAY else endHour
    val span = (normalizedEnd - startHour).coerceAtLeast(1)
    return List(AXIS_TICKS + 1) { index -> (startHour + span * index / AXIS_TICKS) % HOURS_PER_DAY }
}
