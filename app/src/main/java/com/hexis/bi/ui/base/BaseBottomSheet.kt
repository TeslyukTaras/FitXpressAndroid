package com.hexis.bi.ui.base

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.ui.theme.overlayBorder
import com.hexis.bi.utils.constants.GradientDividerConstants

/**
 * Standard modal bottom sheet used across the app.
 *
 * Provides consistent shape, drag handle, and optional [title].
 * Content is provided via the [content] slot.
 *
 * Usage:
 * ```
 * var showSheet by remember { mutableStateOf(false) }
 *
 * if (showSheet) {
 *     BaseBottomSheet(
 *         title = "Sleep settings",
 *         onDismiss = { showSheet = false },
 *     ) {
 *         // sheet content
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val cornerRadius = dimensionResource(R.dimen.corner_sheet)
    val topBorderWidth = dimensionResource(R.dimen.border_thin)
    val topBorderColor = MaterialTheme.colorScheme.overlayBorder()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        scrimColor = MaterialTheme.colorScheme.scrim,
        // Drag handle lives in the content so the top border can be drawn on the sheet's actual
        // top edge (the ModalBottomSheet modifier wraps a full-height container, not the sheet top).
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 1px top edge only — traces the two rounded top corners, no sides. Contrasting on
                // dark, blends with the surface on light.
                .drawWithContent {
                    drawContent()
                    val r = cornerRadius.toPx()
                    val sw = topBorderWidth.toPx()
                    val inset = sw / 2f
                    // Inner corner radius concentric with the sheet's corner (centre stays at (r, r)),
                    // so the stroke runs parallel to the rounded top edge instead of bowing outside
                    // the clip near the corners (which made the ends thin out / fade).
                    val rr = r - inset
                    val w = size.width
                    val path = Path().apply {
                        moveTo(inset, r)
                        arcTo(Rect(inset, inset, inset + 2f * rr, inset + 2f * rr), 180f, 90f, true)
                        lineTo(w - r, inset)
                        arcTo(
                            Rect(w - inset - 2f * rr, inset, w - inset, inset + 2f * rr),
                            270f,
                            90f,
                            false,
                        )
                    }
                    // Solid across the top, fading to transparent only over the last ~2.5% at each
                    // corner so the ends blend into the screen.
                    val transparentEnd = topBorderColor.copy(alpha = 0f)
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            GradientDividerConstants.EDGE_STOP to transparentEnd,
                            GradientDividerConstants.EDGE_FADE_FRACTION to topBorderColor,
                            (GradientDividerConstants.END_STOP - GradientDividerConstants.EDGE_FADE_FRACTION) to topBorderColor,
                            GradientDividerConstants.END_STOP to transparentEnd,
                        ),
                        style = Stroke(width = sw),
                    )
                },
        ) {
            BottomSheetDragHandle()
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_large))
                    .navigationBarsPadding(),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_xl)))
                }

                content()

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_xl)))
            }
        }
    }
}

@Composable
private fun BottomSheetDragHandle() {
    val handleColor = MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacer_l))
            .height(dimensionResource(R.dimen.drag_sheet)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .height(dimensionResource(R.dimen.size_indicator))
                .fillMaxWidth(0.2f)
        ) {
            drawRoundRect(
                color = handleColor,
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}
