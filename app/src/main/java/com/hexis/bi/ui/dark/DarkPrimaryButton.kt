package com.hexis.bi.ui.dark

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.DarkPrimaryButtonDisabledFill
import com.hexis.bi.ui.theme.DarkPrimaryButtonGradientBottom
import com.hexis.bi.ui.theme.DarkPrimaryButtonGradientTop
import com.hexis.bi.utils.constants.DarkBackgroundConstants
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass

@Composable
fun DarkPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    @DrawableRes trailingIcon: Int? = null,
) {
    val isActive = enabled && !isLoading
    val shape = MaterialTheme.shapes.small
    val fillBrush: ((Size) -> Brush)? = if (isActive) {
        { size ->
            Brush.verticalGradient(
                colors = listOf(DarkPrimaryButtonGradientTop, DarkPrimaryButtonGradientBottom),
                startY = size.height * DarkBackgroundConstants.COMPONENT_VERTICAL_GRADIENT_START_FRACTION,
                endY = size.height * DarkBackgroundConstants.COMPONENT_VERTICAL_GRADIENT_END_FRACTION,
            )
        }
    } else {
        null
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
            .glass(
                shape = shape,
                level = GlassConstants.LEVEL_RAISED,
                fill = if (isActive) Color.Transparent else DarkPrimaryButtonDisabledFill,
                fillBrush = fillBrush,
                backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                rimWidth = dimensionResource(R.dimen.glass_rim_width),
                lightingStrength = 0.65f,
            ),
    ) {
        if (isLoading) CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onBackground,
            strokeWidth = dimensionResource(R.dimen.border_thin),
            modifier = Modifier.size(dimensionResource(R.dimen.size_loading_indicator)),
        )
        else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.spacer_l)))
                Icon(
                    painter = painterResource(trailingIcon),
                    contentDescription = null,
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
        }
    }
}
