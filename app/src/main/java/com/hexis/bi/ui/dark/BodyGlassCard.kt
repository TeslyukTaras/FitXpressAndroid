package com.hexis.bi.ui.dark

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass

@Composable
fun BodyGlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(dimensionResource(R.dimen.spacer_m)),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glass(
                shape = MaterialTheme.shapes.medium,
                level = GlassConstants.LEVEL_DEFAULT,
                fillBrush = { bodyGlassCardFillBrush(it) },
                backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                rimWidth = dimensionResource(R.dimen.glass_rim_width),
            )
            .then(if (highlighted) Modifier.bodyGlassHighlight() else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

private fun Modifier.bodyGlassHighlight(): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(BodyGlassHighlightTopStart, Color.Transparent),
            center = Offset.Zero,
            radius = BodyGlassHighlightTopStartRadius.toPx(),
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(BodyGlassHighlightBottomEnd, Color.Transparent),
            center = Offset(
                x = size.width - BodyGlassHighlightBottomEndOffset.toPx(),
                y = size.height - BodyGlassHighlightBottomEndOffset.toPx(),
            ),
            radius = BodyGlassHighlightBottomEndRadius.toPx(),
        ),
    )
}

private val BodyGlassHighlightTopStart = Color(0x5E1DC4B3)
private val BodyGlassHighlightBottomEnd = Color(0x4D1DC4B3)
private val BodyGlassHighlightTopStartRadius = 80.dp
private val BodyGlassHighlightBottomEndRadius = 45.dp
private val BodyGlassHighlightBottomEndOffset = 10.dp
