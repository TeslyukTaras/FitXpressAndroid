package com.hexis.bi.ui.main.home.paceofaging.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.dark.DarkTheme

@Composable
internal fun PaceOfAgingMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val ext = DarkTheme.extendedColors
    val trackColor = ext.gaugeTrack
    val fillColors = remember(ext.gaugeLow, ext.gaugeMid, ext.gaugeHigh) {
        listOf(ext.gaugeLow, ext.gaugeMid, ext.gaugeHigh)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.pace_meter_bar_height)),
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)

        val fillWidth = fraction.coerceIn(0f, 1f) * size.width
        if (fillWidth > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(fillColors, startX = 0f, endX = size.width),
                size = Size(fillWidth, size.height),
                cornerRadius = radius,
            )
        }
    }
}
