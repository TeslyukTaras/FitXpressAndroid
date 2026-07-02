package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.AppVerticalGradientDivider
import com.hexis.bi.ui.theme.MeasurementValueStyle

data class MetricSegment(val number: String, val unit: String)

data class ActivityGridCell(
    val label: String,
    val segments: List<MetricSegment>,
)

@Composable
fun ActivityMetricsGrid(
    cells: List<ActivityGridCell>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val rows = cells.chunked(2)
        rows.forEachIndexed { rowIndex, rowCells ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
            ) {
                MetricCell(
                    cell = rowCells[0],
                    modifier = Modifier.weight(1f),
                )
                if (rowCells.size > 1) {
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
                    AppVerticalGradientDivider()
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
                    MetricCell(
                        cell = rowCells[1],
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            if (rowIndex < rows.lastIndex) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                AppHorizontalGradientDivider()
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            }
        }
    }
}

@Composable
private fun MetricCell(
    cell: ActivityGridCell?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (cell == null) return@Column
        Text(
            text = cell.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        Row(verticalAlignment = Alignment.Bottom) {
            cell.segments.forEachIndexed { index, segment ->
                if (index > 0) Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xs)))
                Text(
                    text = segment.number,
                    style = MeasurementValueStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_3xs)))
                Text(
                    text = segment.unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Composable
fun rememberDurationSegments(
    totalSeconds: Int,
    includeSeconds: Boolean,
): List<MetricSegment> {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    val h = stringResource(R.string.activity_unit_hours_short)
    val m = stringResource(R.string.activity_unit_minutes_short)
    val s = stringResource(R.string.activity_unit_seconds_short)

    return buildList {
        if (hours > 0) add(MetricSegment(hours.toString(), h))
        if (includeSeconds) {
            add(MetricSegment(minutes.toString(), m))
            add(MetricSegment(seconds.toString(), s))
        } else if (minutes > 0 || hours == 0) {
            add(MetricSegment(minutes.toString(), m))
        }
    }
}
