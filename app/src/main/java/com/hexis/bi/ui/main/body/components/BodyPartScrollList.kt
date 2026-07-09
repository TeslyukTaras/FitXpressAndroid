package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.utils.constants.BodyVisualConstants
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil

@Composable
internal fun BodyPartScrollList(
    selected: BodyMeasurementRegion,
    onSelect: (BodyMeasurementRegion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = BodyMeasurementRegion.entries.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scrollDampingConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                return Offset(
                    x = 0f,
                    y = available.y * BodyVisualConstants.BODY_PART_SELECTOR_PRE_CONSUMED_SCROLL_RATIO,
                )
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                return Offset(x = 0f, y = available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity =
                Velocity(
                    x = 0f,
                    y = available.y * BodyVisualConstants.BODY_PART_SELECTOR_PRE_CONSUMED_SCROLL_RATIO,
                )

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                available
        }
    }
    val latestSelected = rememberUpdatedState(selected)
    val latestOnSelect by rememberUpdatedState(onSelect)
    var programmaticScrollTarget by remember { mutableStateOf<Int?>(null) }
    val labelStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
    val labels = BodyMeasurementRegion.entries.map {
        stringResource(BodyVisualConstants.visualLabelRes(it))
    }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelWidth = remember(labels, labelStyle, textMeasurer, density) {
        with(density) {
            val maxLabelWidthPx = labels.maxOf { label ->
                textMeasurer.measure(
                    text = AnnotatedString(label),
                    style = labelStyle,
                ).size.width
            }
            ceil(maxLabelWidthPx.toDouble()).toFloat().toDp()
        }
    }
    val itemHeight = dimensionResource(R.dimen.body_part_selector_item_height)
    val selectorWidth = dimensionResource(R.dimen.body_part_selector_scale_width) +
            dimensionResource(R.dimen.body_part_selector_label_gap) +
            labelWidth

    BoxWithConstraints(
        modifier = modifier
            .width(selectorWidth)
    ) {
        val itemHeightPx = with(density) { itemHeight.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val maxScrollPx =
            (BodyMeasurementRegion.entries.size * itemHeightPx - viewportHeightPx).coerceAtLeast(0f)
        val hasViewport = maxHeight > 0.dp

        fun scrollOffsetFor(index: Int): Int {
            val desiredScrollPx = bodyPartSelectorScrollPxForIndex(
                index = index,
                itemHeightPx = itemHeightPx,
                viewportHeightPx = viewportHeightPx,
                maxScrollPx = maxScrollPx,
            )
            return (desiredScrollPx - index * itemHeightPx).toInt()
        }

        suspend fun animateToBodyPart(index: Int) {
            programmaticScrollTarget = index
            try {
                listState.animateScrollToItem(
                    index = index,
                    scrollOffset = scrollOffsetFor(index),
                )
            } finally {
                programmaticScrollTarget = null
            }
        }

        LaunchedEffect(selectedIndex, hasViewport) {
            if (hasViewport && !listState.isScrollInProgress) animateToBodyPart(selectedIndex)
        }

        LaunchedEffect(listState, hasViewport, itemHeightPx, maxScrollPx) {
            if (!hasViewport) return@LaunchedEffect
            snapshotFlow {
                listState.selectedBodyPartIndex(
                    itemHeightPx = itemHeightPx,
                    maxScrollPx = maxScrollPx,
                    itemCount = BodyMeasurementRegion.entries.size,
                )
            }
                .distinctUntilChanged()
                .collect { index ->
                    if (index == null) return@collect
                    if (programmaticScrollTarget != null) return@collect
                    val region = BodyMeasurementRegion.entries[index]
                    if (region != latestSelected.value) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        latestOnSelect(region)
                    }
                }
        }

        LaunchedEffect(listState, hasViewport, itemHeightPx, maxScrollPx) {
            if (!hasViewport) return@LaunchedEffect
            snapshotFlow { listState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { isScrolling ->
                    if (isScrolling) return@collect
                    val index = listState.selectedBodyPartIndex(
                        itemHeightPx = itemHeightPx,
                        maxScrollPx = maxScrollPx,
                        itemCount = BodyMeasurementRegion.entries.size,
                    ) ?: return@collect
                    val currentScrollPx = listState.bodyPartSelectorScrollPx(itemHeightPx)
                    val targetScrollPx = bodyPartSelectorScrollPxForIndex(
                        index = index,
                        itemHeightPx = itemHeightPx,
                        viewportHeightPx = viewportHeightPx,
                        maxScrollPx = maxScrollPx,
                    )
                    if (
                        abs(currentScrollPx - targetScrollPx) >
                        BodyVisualConstants.BODY_PART_SELECTOR_SNAP_EPSILON_PX
                    ) {
                        animateToBodyPart(index)
                    }
                }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .matchParentSize()
                .nestedScroll(scrollDampingConnection),
        ) {
            items(BodyMeasurementRegion.entries.size) { index ->
                val region = BodyMeasurementRegion.entries[index]
                val isSelected by remember(region) {
                    derivedStateOf { latestSelected.value == region }
                }
                BodyPartScrollItem(
                    label = labels[index],
                    isSelected = isSelected,
                    labelWidth = labelWidth,
                    selectedLabelStyle = labelStyle,
                    drawTrailingTicks = index < BodyMeasurementRegion.entries.lastIndex,
                    onClick = {
                        scope.launch {
                            if (region != latestSelected.value) latestOnSelect(region)
                            animateToBodyPart(index)
                        }
                    },
                )
            }
        }
    }
}

private fun LazyListState.selectedBodyPartIndex(
    itemHeightPx: Float,
    maxScrollPx: Float,
    itemCount: Int,
): Int? {
    val layoutInfo = layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null
    if (itemCount <= 1) return 0

    val currentScrollPx = bodyPartSelectorScrollPx(itemHeightPx)
    val viewportStart = layoutInfo.viewportStartOffset.toFloat()
    val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
    val viewportCenter = (viewportStart + viewportEnd) / 2f
    val focalY = bodyPartSelectorFocalY(
        scrollPx = currentScrollPx,
        maxScrollPx = maxScrollPx,
        itemHeightPx = itemHeightPx,
        viewportStart = viewportStart,
        viewportCenter = viewportCenter,
        viewportEnd = viewportEnd,
    )

    return visibleItems
        .minByOrNull { item ->
            val itemCenter = item.offset + item.size / 2f
            abs(itemCenter - focalY)
        }
        ?.index
        ?.coerceIn(0, itemCount - 1)
}

private fun LazyListState.bodyPartSelectorScrollPx(
    itemHeightPx: Float,
): Float = firstVisibleItemIndex * itemHeightPx + firstVisibleItemScrollOffset

private fun bodyPartSelectorFocalY(
    scrollPx: Float,
    maxScrollPx: Float,
    itemHeightPx: Float,
    viewportStart: Float,
    viewportCenter: Float,
    viewportEnd: Float,
): Float {
    // Overlapping edge zones make focalY discontinuous.
    val edgeUnlockPx = (itemHeightPx * BodyVisualConstants.BODY_PART_SELECTOR_EDGE_UNLOCK_ITEMS)
        .coerceAtMost(maxScrollPx / 2f)
    if (edgeUnlockPx <= 0f) return viewportCenter
    val topProgress = (scrollPx / edgeUnlockPx).coerceIn(0f, 1f)
    val bottomProgress = ((maxScrollPx - scrollPx) / edgeUnlockPx).coerceIn(0f, 1f)
    val topFocalY = viewportStart + itemHeightPx / 2f
    val bottomFocalY = viewportEnd - itemHeightPx / 2f

    return when {
        topProgress < 1f -> lerp(topFocalY, viewportCenter, topProgress)
        bottomProgress < 1f -> lerp(bottomFocalY, viewportCenter, bottomProgress)
        else -> viewportCenter
    }
}

private fun bodyPartSelectorScrollPxForIndex(
    index: Int,
    itemHeightPx: Float,
    viewportHeightPx: Float,
    maxScrollPx: Float,
): Float {
    if (maxScrollPx <= 0f) return 0f

    val targetContentY = index * itemHeightPx + itemHeightPx / 2f
    var low = 0f
    var high = maxScrollPx

    repeat(BodyVisualConstants.BODY_PART_SELECTOR_SNAP_SEARCH_ITERATIONS) {
        val mid = (low + high) / 2f
        val focalContentY = mid + bodyPartSelectorFocalY(
            scrollPx = mid,
            maxScrollPx = maxScrollPx,
            itemHeightPx = itemHeightPx,
            viewportStart = 0f,
            viewportCenter = viewportHeightPx / 2f,
            viewportEnd = viewportHeightPx,
        )
        if (focalContentY < targetContentY) {
            low = mid
        } else {
            high = mid
        }
    }

    return ((low + high) / 2f).coerceIn(0f, maxScrollPx)
}

@Composable
private fun BodyPartScrollItem(
    label: String,
    isSelected: Boolean,
    labelWidth: Dp,
    selectedLabelStyle: TextStyle,
    drawTrailingTicks: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemHeight = dimensionResource(R.dimen.body_part_selector_item_height)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyPartSelectorScale(
            isSelected = isSelected,
            drawTrailingTicks = drawTrailingTicks,
            modifier = Modifier
                .width(dimensionResource(R.dimen.body_part_selector_scale_width))
                .height(itemHeight),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.body_part_selector_label_gap)))
        Box(
            modifier = Modifier
                .width(labelWidth)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = label,
                style = if (isSelected) selectedLabelStyle else MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = BodyVisualConstants.BODY_PART_SELECTOR_INACTIVE_ALPHA,
                    )
                },
            )
        }
    }
}

@Composable
private fun BodyPartSelectorScale(
    isSelected: Boolean,
    drawTrailingTicks: Boolean,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.onBackground
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
        .copy(alpha = BodyVisualConstants.BODY_PART_SELECTOR_INACTIVE_ALPHA)
    val tickFull = dimensionResource(R.dimen.body_part_selector_tick_full)
    val tickInner = dimensionResource(R.dimen.body_part_selector_tick_inner)
    val tickSpacingDp = dimensionResource(R.dimen.body_part_selector_tick_spacing)
    val tickStrokeDp = dimensionResource(R.dimen.body_part_selector_tick_stroke)
    Canvas(modifier = modifier) {
        val bodyPartTickLength = tickFull.toPx()
        val innerTickLength = tickInner.toPx()
        val tickSpacing = tickSpacingDp.toPx()
        val tickStroke = tickStrokeDp.toPx()
        val centerY = size.height / 2f

        drawLine(
            color = if (isSelected) activeColor else inactiveColor,
            start = Offset(0f, centerY),
            end = Offset(bodyPartTickLength, centerY),
            strokeWidth = tickStroke,
        )

        if (!drawTrailingTicks) return@Canvas

        repeat(BodyVisualConstants.BODY_PART_SELECTOR_INNER_TICK_COUNT) { index ->
            val y = centerY + tickSpacing * (index + 1)
            drawLine(
                color = inactiveColor,
                start = Offset(0f, y),
                end = Offset(innerTickLength, y),
                strokeWidth = tickStroke,
            )
        }
    }
}
