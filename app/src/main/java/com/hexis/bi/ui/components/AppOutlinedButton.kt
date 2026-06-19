package com.hexis.bi.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.BackgroundConstants
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass

@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val isActive = enabled && !isLoading
    val shape = MaterialTheme.shapes.small
    val borderPx = with(LocalDensity.current) { dimensionResource(R.dimen.border_thin).toPx() }
    val borderTop = NocturnePulseTheme.extendedColors.switchActiveTrackTop
    val borderBottom = NocturnePulseTheme.extendedColors.switchActiveTrackBottom

    val decorationModifier = if (isActive) {
        Modifier.drawBehind {
            val brush = Brush.linearGradient(
                colors = listOf(borderTop, borderBottom),
                start = Offset(
                    0f,
                    size.height * BackgroundConstants.COMPONENT_VERTICAL_GRADIENT_START_FRACTION
                ),
                end = Offset(
                    0f,
                    size.height * BackgroundConstants.COMPONENT_VERTICAL_GRADIENT_END_FRACTION_WIDE
                ),
            )
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline = outline, brush = brush, style = Stroke(width = borderPx))
        }
    } else {
        Modifier
            .glass(
                tint = NocturnePulseTheme.extendedColors.glassRimHighlight,
                shape = shape,
                level = GlassConstants.LEVEL_RAISED,
                backgroundAlpha = 1f,
                backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                rimWidth = dimensionResource(R.dimen.glass_rim_width),
            )
            .border(
                dimensionResource(R.dimen.border_thin),
                MaterialTheme.colorScheme.outline,
                shape
            )
    }

    Button(
        onClick = onClick,
        enabled = isActive,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = dimensionResource(R.dimen.elevation_none),
            pressedElevation = dimensionResource(R.dimen.elevation_none),
            disabledElevation = dimensionResource(R.dimen.elevation_none),
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.height_button))
            .then(decorationModifier),
    ) {
        if (isLoading) CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onBackground,
            strokeWidth = dimensionResource(R.dimen.border_thin),
            modifier = Modifier.size(dimensionResource(R.dimen.size_loading_indicator)),
        )
        else Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
