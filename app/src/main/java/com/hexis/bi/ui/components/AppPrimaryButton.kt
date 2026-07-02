package com.hexis.bi.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.BackgroundConstants
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass

/**
 * Built on a clickable [Box] rather than Material [androidx.compose.material3.Button] so [width] and
 * [height] set the exact button (and touch) size — Material's button/surface impose a hidden
 * minimum interactive size that ignores small explicit heights.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    @DrawableRes trailingIcon: Int? = null,
    width: Dp? = null,
    height: Dp? = null,
) {
    val isActive = enabled && !isLoading
    val shape = MaterialTheme.shapes.small
    val resolvedHeight = height ?: dimensionResource(R.dimen.height_button)
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val gradientTop = NocturnePulseTheme.extendedColors.primaryButtonGradientTop
    val gradientBottom = NocturnePulseTheme.extendedColors.primaryButtonGradientBottom
    val fillBrush: ((Size) -> Brush)? = if (isActive) {
        { size ->
            Brush.verticalGradient(
                colors = listOf(gradientTop, gradientBottom),
                startY = size.height * BackgroundConstants.COMPONENT_VERTICAL_GRADIENT_START_FRACTION,
                endY = size.height * BackgroundConstants.COMPONENT_VERTICAL_GRADIENT_END_FRACTION,
            )
        }
    } else {
        null
    }

    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(resolvedHeight)
            .glass(
                tint = NocturnePulseTheme.extendedColors.glassRimHighlight,
                shape = shape,
                level = GlassConstants.LEVEL_RAISED,
                fill = if (isActive) Color.Transparent else NocturnePulseTheme.extendedColors.primaryButtonDisabledFill,
                fillBrush = fillBrush,
                backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                rimWidth = dimensionResource(R.dimen.glass_rim_width),
                lightingStrength = 0.65f,
            )
            .clickable(
                enabled = isActive,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onBackground,
                strokeWidth = dimensionResource(R.dimen.border_thin),
                modifier = Modifier.size(dimensionResource(R.dimen.size_loading_indicator)),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.size(dimensionResource(R.dimen.spacer_l)))
                    Icon(
                        painter = painterResource(trailingIcon),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                    )
                }
            }
        }
    }
}
