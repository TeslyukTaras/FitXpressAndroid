package com.hexis.bi.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import kotlin.math.absoluteValue

data class PickerColumnData<T>(
    val state: PagerState,
    val items: List<T>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppScrollPicker(
    pickerColumns: List<PickerColumnData<*>>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onConfirm: () -> Unit
) {
    AppDialog(onDismiss = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
        ) {
            Text(
                text = title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = dimensionResource(R.dimen.icon_normalized)),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.picker_height)),
                contentAlignment = Alignment.Center
            ) {
                // Highlighting bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(dimensionResource(R.dimen.picker_highlight_height))
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.medium
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pickerColumns.forEach { column ->
                        WheelColumn(
                            state = column.state,
                            items = column.items.map { it.toString() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            AppButton(
                text = stringResource(R.string.action_save),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelColumn(
    state: PagerState,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    VerticalPager(
        state = state,
        modifier = modifier.height(dimensionResource(R.dimen.picker_height)),
        contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.picker_item_padding)),
        beyondViewportPageCount = 1
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val pageOffset = (
                            (state.currentPage - page) + state.currentPageOffsetFraction
                            ).absoluteValue

                    val alphaValue = 1f - (pageOffset * 0.5f).coerceIn(0f, 0.7f)
                    val scaleValue = 1f - (pageOffset * 0.15f).coerceIn(0f, 0.2f)

                    alpha = alphaValue
                    scaleX = scaleValue
                    scaleY = scaleValue
                    rotationX =
                        (state.currentPageOffsetFraction + (state.currentPage - page)) * -20f
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = items[page % items.size],
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}