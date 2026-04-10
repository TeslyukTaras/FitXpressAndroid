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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.hexis.bi.R

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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDragHandle() },
    ) {
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

@Composable
private fun BottomSheetDragHandle() {
    val handleColor = MaterialTheme.colorScheme.secondary
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_l)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.icon_normalized)),
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
