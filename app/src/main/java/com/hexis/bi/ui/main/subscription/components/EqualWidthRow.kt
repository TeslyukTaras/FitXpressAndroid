package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

@Composable
internal fun EqualWidthRow(
    gap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val count = measurables.size
        val natural = measurables.maxOf { it.maxIntrinsicWidth(Int.MAX_VALUE) }
        val cell = minOf(natural, (constraints.maxWidth - gapPx * (count - 1)) / count)
        val placeables = measurables.map { it.measure(Constraints(minWidth = cell, maxWidth = cell)) }
        val height = placeables.maxOf { it.height }
        layout(constraints.maxWidth, height) {
            val total = cell * count + gapPx * (count - 1)
            var x = (constraints.maxWidth - total) / 2
            placeables.forEach {
                it.placeRelative(x, 0)
                x += cell + gapPx
            }
        }
    }
}
