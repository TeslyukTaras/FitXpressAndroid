package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import com.hexis.bi.R

internal data class FooterLinkItem(val text: String, val onClick: () -> Unit)

@Composable
internal fun FooterLinks(
    links: List<FooterLinkItem>,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = { links.forEach { FooterLinkText(it.text, it.onClick) } },
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val cell = maxWidth / measurables.size
        val fitsInRow = measurables.all { it.maxIntrinsicWidth(Int.MAX_VALUE) <= cell }
        if (fitsInRow) {
            val placeables = measurables.map {
                it.measure(Constraints(minWidth = cell, maxWidth = cell))
            }
            val height = placeables.maxOf { it.height }
            layout(maxWidth, height) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(index * cell, (height - placeable.height) / 2)
                }
            }
        } else {
            val placeables = measurables.map { it.measure(Constraints(maxWidth = maxWidth)) }
            layout(maxWidth, placeables.sumOf { it.height }) {
                var y = 0
                placeables.forEach {
                    it.placeRelative((maxWidth - it.width) / 2, y)
                    y += it.height
                }
            }
        }
    }
}

@Composable
private fun FooterLinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = dimensionResource(R.dimen.spacer_s)),
        textAlign = TextAlign.Center,
    )
}
