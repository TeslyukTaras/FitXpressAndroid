package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
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

/**
 * Horizontal body-part selector for the Compare tab. Mirrors the scroll / snap /
 * focal-point selection logic of the vertical [BodyPartScrollList], adapted to
 * natural (per-label) item widths instead of a uniform item size.
 */
@Composable
internal fun BodyPartHorizontalScrollList(
    selected: BodyMeasurementRegion,
    onSelect: (BodyMeasurementRegion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val regions = BodyMeasurementRegion.entries
    val selectedIndex = regions.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val selectedLabelStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
    val labels = regions.map { stringResource(BodyVisualConstants.visualLabelRes(it)) }

    val scrollDampingConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                return Offset(
                    x = available.x * BodyVisualConstants.BODY_PART_SELECTOR_HORIZONTAL_PRE_CONSUMED_SCROLL_RATIO,
                    y = 0f,
                )
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                return Offset(x = available.x, y = 0f)
            }

            override suspend fun onPreFling(available: Velocity): Velocity =
                Velocity(
                    x = available.x * BodyVisualConstants.BODY_PART_SELECTOR_HORIZONTAL_PRE_CONSUMED_SCROLL_RATIO,
                    y = 0f,
                )

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                available
        }
    }

    val latestSelected by rememberUpdatedState(selected)
    val latestOnSelect by rememberUpdatedState(onSelect)
    var programmaticScrollTarget by remember { mutableStateOf<Int?>(null) }

    // Item widths (natural label width + horizontal padding) and their cumulative
    // content-space offsets — the basis for all snap math with variable widths.
    val textMeasurer = rememberTextMeasurer()
    val itemPaddingPx = with(density) { dimensionResource(R.dimen.body_part_selector_horizontal_item_padding).toPx() }
    val itemWidthsPx = remember(labels, selectedLabelStyle, textMeasurer, itemPaddingPx) {
        labels.map { label ->
            val textWidth = textMeasurer.measure(
                text = AnnotatedString(label),
                style = selectedLabelStyle,
            ).size.width
            ceil(textWidth + itemPaddingPx * 2f)
        }
    }
    val cumulativePx = remember(itemWidthsPx) {
        var acc = 0f
        FloatArray(itemWidthsPx.size) { i -> acc.also { acc += itemWidthsPx[i] } }
    }
    val contentWidthPx = remember(itemWidthsPx) { itemWidthsPx.sum() }

    BoxWithConstraints(modifier = modifier) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val hasViewport = maxWidth > 0.dp
        val maxScrollPx = (contentWidthPx - viewportWidthPx).coerceAtLeast(0f)
        val edgeUnlockPx =
            (contentWidthPx / regions.size * BodyVisualConstants.BODY_PART_SELECTOR_EDGE_UNLOCK_ITEMS)
                .coerceAtMost(maxScrollPx / 2f)
        val leadHalf = itemWidthsPx.first() / 2f
        val trailHalf = itemWidthsPx.last() / 2f

        fun currentScrollPx(): Float =
            cumulativePx[listState.firstVisibleItemIndex] + listState.firstVisibleItemScrollOffset

        fun targetScrollPxFor(index: Int): Float = selectorScrollPxForCenter(
            targetCenter = cumulativePx[index] + itemWidthsPx[index] / 2f,
            maxScrollPx = maxScrollPx,
            viewportSizePx = viewportWidthPx,
            edgeUnlockPx = edgeUnlockPx,
            leadHalf = leadHalf,
            trailHalf = trailHalf,
        )

        suspend fun animateToBodyPart(index: Int) {
            programmaticScrollTarget = index
            try {
                val target = targetScrollPxFor(index)
                listState.animateScrollToItem(
                    index = index,
                    scrollOffset = (target - cumulativePx[index]).toInt(),
                )
            } finally {
                programmaticScrollTarget = null
            }
        }

        LaunchedEffect(selectedIndex, hasViewport) {
            if (hasViewport && !listState.isScrollInProgress) animateToBodyPart(selectedIndex)
        }

        LaunchedEffect(listState, hasViewport, maxScrollPx, edgeUnlockPx) {
            if (!hasViewport) return@LaunchedEffect
            snapshotFlow {
                listState.selectorSelectedIndex(
                    scrollPx = currentScrollPx(),
                    maxScrollPx = maxScrollPx,
                    edgeUnlockPx = edgeUnlockPx,
                    leadHalf = leadHalf,
                    trailHalf = trailHalf,
                    itemCount = regions.size,
                )
            }
                .distinctUntilChanged()
                .collect { index ->
                    if (index == null) return@collect
                    if (programmaticScrollTarget != null) return@collect
                    val region = regions[index]
                    if (region != latestSelected) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        latestOnSelect(region)
                    }
                }
        }

        LaunchedEffect(listState, hasViewport, maxScrollPx, edgeUnlockPx) {
            if (!hasViewport) return@LaunchedEffect
            snapshotFlow { listState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { isScrolling ->
                    if (isScrolling) return@collect
                    val index = listState.selectorSelectedIndex(
                        scrollPx = currentScrollPx(),
                        maxScrollPx = maxScrollPx,
                        edgeUnlockPx = edgeUnlockPx,
                        leadHalf = leadHalf,
                        trailHalf = trailHalf,
                        itemCount = regions.size,
                    ) ?: return@collect
                    val targetScrollPx = targetScrollPxFor(index)
                    if (
                        abs(currentScrollPx() - targetScrollPx) >
                        BodyVisualConstants.BODY_PART_SELECTOR_SNAP_EPSILON_PX
                    ) {
                        animateToBodyPart(index)
                    }
                }
        }

        LazyRow(
            state = listState,
            modifier = Modifier
                .matchParentSize()
                .nestedScroll(scrollDampingConnection),
        ) {
            items(regions.size) { index ->
                BodyPartHorizontalItem(
                    label = labels[index],
                    isSelected = regions[index] == selected,
                    width = with(density) { itemWidthsPx[index].toDp() },
                    selectedLabelStyle = selectedLabelStyle,
                    onClick = {
                        scope.launch {
                            val region = regions[index]
                            if (region != latestSelected) latestOnSelect(region)
                            animateToBodyPart(index)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BodyPartHorizontalItem(
    label: String,
    isSelected: Boolean,
    width: Dp,
    selectedLabelStyle: TextStyle,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (isSelected) selectedLabelStyle else MaterialTheme.typography.bodyMedium,
            color = if (isSelected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = BodyVisualConstants.BODY_PART_SELECTOR_INACTIVE_ALPHA,
                )
            },
        )
    }
}

/** Nearest item (by measured center) to the edge-unlock focal point. */
private fun LazyListState.selectorSelectedIndex(
    scrollPx: Float,
    maxScrollPx: Float,
    edgeUnlockPx: Float,
    leadHalf: Float,
    trailHalf: Float,
    itemCount: Int,
): Int? {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) return null
    if (itemCount <= 1) return 0

    val viewportStart = info.viewportStartOffset.toFloat()
    val viewportEnd = info.viewportEndOffset.toFloat()
    val viewportCenter = (viewportStart + viewportEnd) / 2f
    val focal = selectorFocalPoint(
        scrollPx = scrollPx,
        maxScrollPx = maxScrollPx,
        edgeUnlockPx = edgeUnlockPx,
        leadFocal = viewportStart + leadHalf,
        trailFocal = viewportEnd - trailHalf,
        viewportCenter = viewportCenter,
    )

    return visible
        .minByOrNull { item -> abs(item.offset + item.size / 2f - focal) }
        ?.index
        ?.coerceIn(0, itemCount - 1)
}

/**
 * Focal point along the scroll axis. Near the start/end the focal slides toward the
 * leading/trailing edge so the first/last item can be reached and rest near the edge;
 * in the middle it sits at the viewport center. Mirrors the vertical selector.
 */
private fun selectorFocalPoint(
    scrollPx: Float,
    maxScrollPx: Float,
    edgeUnlockPx: Float,
    leadFocal: Float,
    trailFocal: Float,
    viewportCenter: Float,
): Float {
    if (edgeUnlockPx <= 0f) return viewportCenter
    val leadProgress = (scrollPx / edgeUnlockPx).coerceIn(0f, 1f)
    val trailProgress = ((maxScrollPx - scrollPx) / edgeUnlockPx).coerceIn(0f, 1f)
    return when {
        leadProgress < 1f -> lerp(leadFocal, viewportCenter, leadProgress)
        trailProgress < 1f -> lerp(trailFocal, viewportCenter, trailProgress)
        else -> viewportCenter
    }
}

/** Binary-search the scroll px that places [targetCenter] (content space) at the focal point. */
private fun selectorScrollPxForCenter(
    targetCenter: Float,
    maxScrollPx: Float,
    viewportSizePx: Float,
    edgeUnlockPx: Float,
    leadHalf: Float,
    trailHalf: Float,
): Float {
    if (maxScrollPx <= 0f) return 0f
    var low = 0f
    var high = maxScrollPx
    repeat(BodyVisualConstants.BODY_PART_SELECTOR_SNAP_SEARCH_ITERATIONS) {
        val mid = (low + high) / 2f
        val focalContent = mid + selectorFocalPoint(
            scrollPx = mid,
            maxScrollPx = maxScrollPx,
            edgeUnlockPx = edgeUnlockPx,
            leadFocal = leadHalf,
            trailFocal = viewportSizePx - trailHalf,
            viewportCenter = viewportSizePx / 2f,
        )
        if (focalContent < targetCenter) low = mid else high = mid
    }
    return ((low + high) / 2f).coerceIn(0f, maxScrollPx)
}
