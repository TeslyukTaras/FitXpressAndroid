package com.hexis.bi.ui.main.scan.results

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.hexis.bi.R
import com.hexis.bi.utils.cmToFeetAndInches


/** Resolved measurement pill copy (body part name + primary value line). */
private data class LeaderLabelLines(val title: String, val valueLine: String)

@Composable
internal fun MeasurementLeaderOverlay(
    measurements: List<MeasurementRow>,
    isMetric: Boolean,
    transform: VisualAvatarTransform?,
    modifier: Modifier = Modifier,
    /** Parsed OBJ guide (anchors + mesh slice outlines). Null uses [MeasurementVisualAnchors] fallbacks only. */
    measurementGuide: MetricAvatarMeasurementGuide? = null,
) {
    if (transform == null || transform.widthPx <= 0 || transform.heightPx <= 0) return

    val strokeWidthDp = dimensionResource(R.dimen.measurement_leader_stroke_width)
    val leaderStrokeColor = colorResource(R.color.measurement_leader_stroke)
    val pillColumnMaxWidth = dimensionResource(R.dimen.measurement_leader_pill_column_max_width)

    val edgePadding = dimensionResource(R.dimen.padding_small)
    val pillCorner = dimensionResource(R.dimen.scan_corner_radius)
    val pillPaddingH = dimensionResource(R.dimen.spacer_xs)
    val pillPaddingV = dimensionResource(R.dimen.spacer_xxs)
    val captionGap = dimensionResource(R.dimen.spacer_3xs)

    val measurementKeySig = remember(measurements) {
        measurements.joinToString("|") { it.visualAnchorKey }
    }
    val labelStarts = remember(measurementKeySig) {
        mutableStateMapOf<String, Offset>()
    }

    val leftRows = remember(measurements) {
        measurements.filterIndexed { index, _ -> index % 2 == 0 }
    }
    val rightRows = remember(measurements) {
        measurements.filterIndexed { index, _ -> index % 2 != 0 }
    }

    var overlayLayout by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInteropFilter(onTouchEvent = { false })
            .onGloballyPositioned { overlayLayout = it },
    ) {
        Canvas(
            Modifier.fillMaxSize(),
        ) {
            val sx =
                if (transform.widthPx > 0) size.width / transform.widthPx.toFloat() else ScanResultsLayout.OVERLAY_SCALE_FALLBACK
            val sy =
                if (transform.heightPx > 0) size.height / transform.heightPx.toFloat() else ScanResultsLayout.OVERLAY_SCALE_FALLBACK

            for (row in measurements) {
                val anchor = measurementGuide?.anchorPoints?.get(row.visualAnchorKey)
                    ?: MeasurementVisualAnchors.fallbackAnchorPosition(row.visualAnchorKey)
                    ?: continue
                val start = labelStarts[row.visualAnchorKey] ?: continue

                if (row.visualAnchorKey in CircumferenceVisualKeys) {
                    val packedPrimary =
                        measurementGuide?.crossSectionPolylines?.get(row.visualAnchorKey)
                    val packedOpp = measurementGuide?.crossSectionPolylinesOpposite
                        ?.get(row.visualAnchorKey)
                    val polys = ArrayList<List<Offset>>(2)
                    if (packedPrimary != null && packedPrimary.size >= MetricAvatarPackedGeometry.MIN_PACKED_POLYLINE_FLOATS) {
                        val poly = projectPackedPolylineToOverlay(
                            packedPrimary,
                            transform,
                            sx,
                            sy,
                        )
                        if (poly.size >= MetricAvatarPackedGeometry.MIN_PROJECTED_POLYLINE_VERTICES) polys.add(
                            poly
                        )
                    }
                    if (packedOpp != null && packedOpp.size >= MetricAvatarPackedGeometry.MIN_PACKED_POLYLINE_FLOATS) {
                        val poly = projectPackedPolylineToOverlay(
                            packedOpp,
                            transform,
                            sx,
                            sy,
                        )
                        if (poly.size >= MetricAvatarPackedGeometry.MIN_PROJECTED_POLYLINE_VERTICES) polys.add(
                            poly
                        )
                    }
                    if (polys.isNotEmpty()) {
                        val attach = if (polys.size == 1) {
                            leaderAttachPointForCircumferenceSlice(polys[0], start)
                        } else {
                            leaderAttachPointForCircumferenceSlices(polys, start)
                        }
                        drawPath(
                            path = leaderLinePath(start = start, end = attach),
                            color = leaderStrokeColor,
                            style = Stroke(width = strokeWidthDp.toPx()),
                        )
                        continue
                    }
                }

                val proj = projectModelPointToViewPx(
                    anchor[0],
                    anchor[1],
                    anchor[2],
                    transform.widthPx,
                    transform.heightPx,
                    transform.yawDeg,
                    transform.pitchDeg,
                ) ?: continue

                val end = Offset(proj.x * sx, proj.y * sy)
                drawPath(
                    path = leaderLinePath(start = start, end = end),
                    color = leaderStrokeColor,
                    style = Stroke(width = strokeWidthDp.toPx()),
                )
            }
        }

        key(overlayLayout != null) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = edgePadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .widthIn(max = pillColumnMaxWidth),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.Start,
                ) {
                    for (row in leftRows) {
                        MeasurementEdgePill(
                            row = row,
                            isMetric = isMetric,
                            isLeftColumn = true,
                            overlayLayout = overlayLayout,
                            pillCorner = pillCorner,
                            pillPaddingH = pillPaddingH,
                            pillPaddingV = pillPaddingV,
                            captionGap = captionGap,
                            onPlaced = { anchorKey: String, attachOffset: Offset ->
                                labelStarts[anchorKey] = attachOffset
                            },
                        )
                    }
                }

                Spacer(Modifier.weight(2.2f))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .widthIn(max = pillColumnMaxWidth),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.End,
                ) {
                    for (row in rightRows) {
                        MeasurementEdgePill(
                            row = row,
                            isMetric = isMetric,
                            isLeftColumn = false,
                            overlayLayout = overlayLayout,
                            pillCorner = pillCorner,
                            pillPaddingH = pillPaddingH,
                            pillPaddingV = pillPaddingV,
                            captionGap = captionGap,
                            onPlaced = { anchorKey: String, attachOffset: Offset ->
                                labelStarts[anchorKey] = attachOffset
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Straight leader segment in overlay coordinates.
 */
private fun leaderLinePath(start: Offset, end: Offset): Path {
    return Path().apply {
        moveTo(start.x, start.y)
        lineTo(end.x, end.y)
    }
}

@Composable
private fun MeasurementEdgePill(
    row: MeasurementRow,
    isMetric: Boolean,
    isLeftColumn: Boolean,
    overlayLayout: LayoutCoordinates?,
    pillCorner: Dp,
    pillPaddingH: Dp,
    pillPaddingV: Dp,
    captionGap: Dp,
    onPlaced: (anchorKey: String, attachOffset: Offset) -> Unit,
) {
    val lines = formatLeaderLabelLines(row, isMetric)

    val pillSurface = colorResource(R.color.measurement_leader_pill_surface)
    val pillValueColor = colorResource(R.color.measurement_leader_pill_value)
    val pillLabelColor = colorResource(R.color.black)
    val valueTextSize = dimensionResource(R.dimen.measurement_leader_pill_value_text_size).value.sp
    val titleTextSize = dimensionResource(R.dimen.measurement_leader_pill_title_text_size).value.sp
    val zeroElevation = dimensionResource(R.dimen.elevation_none)

    Surface(
        shape = RoundedCornerShape(pillCorner),
        color = pillSurface,
        shadowElevation = zeroElevation,
        tonalElevation = zeroElevation,
        modifier = Modifier.onGloballyPositioned { coords ->
            val ov = overlayLayout ?: return@onGloballyPositioned
            val topLeft = ov.localPositionOf(coords, Offset.Zero)
            val w = coords.size.width.toFloat()
            val h = coords.size.height.toFloat()
            val yMid = ScanResultsLayout.PILL_LEADER_ATTACH_Y_FRACTION
            val attach = if (isLeftColumn) {
                Offset(topLeft.x + w, topLeft.y + h * yMid)
            } else {
                Offset(topLeft.x, topLeft.y + h * yMid)
            }
            onPlaced(row.visualAnchorKey, attach)
        },
    ) {
        Column(
            Modifier.padding(horizontal = pillPaddingH, vertical = pillPaddingV),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = lines.valueLine,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = valueTextSize,
                    color = pillValueColor,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(Modifier.height(captionGap))
            Text(
                text = lines.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = titleTextSize,
                    color = pillLabelColor,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun formatLeaderLabelLines(row: MeasurementRow, isMetric: Boolean): LeaderLabelLines {
    val name = stringResource(row.bodyPartRes)
    val valueLine = if (isMetric) {
        stringResource(
            R.string.measurement_leader_value_metric,
            stringResource(R.string.format_value_decimal, row.today.cm),
            stringResource(R.string.unit_cm),
        )
    } else {
        val (feet, inches) = row.today.cm.cmToFeetAndInches()
        stringResource(
            R.string.measurement_leader_value_imperial,
            stringResource(R.string.format_value_int, feet),
            stringResource(R.string.unit_ft),
            stringResource(R.string.format_value_decimal, inches),
            stringResource(R.string.unit_in),
        )
    }
    return LeaderLabelLines(title = name, valueLine = valueLine)
}
