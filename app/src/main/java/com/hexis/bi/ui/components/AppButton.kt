package com.hexis.bi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.Blue100
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.utils.gradientBackground

/**
 * Primary action button.
 *
 * By default renders a blue gradient background. Pass [containerColor] to replace the gradient
 * with a solid colour (e.g. [com.hexis.bi.ui.theme.Red300] for destructive actions).
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val isActive = enabled && !isLoading

    // When a custom containerColor is provided use it as a solid fill and skip the gradient.
    // Otherwise fall back to the standard blue gradient drawn behind a transparent container.
    val backgroundModifier = if (containerColor == null && isActive) {
        Modifier.gradientBackground(
            brush = Brush.verticalGradient(listOf(Blue100, Blue200)),
            shape = MaterialTheme.shapes.small,
        )
    } else Modifier

    val resolvedContainerColor = containerColor ?: Color.Transparent
    val disabledContainerColor = containerColor?.copy(alpha = 0.5f)
        ?: MaterialTheme.colorScheme.primaryFixed

    Button(
        onClick = onClick,
        enabled = isActive,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = resolvedContainerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = contentColor,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = dimensionResource(R.dimen.elevation_none),
            pressedElevation = dimensionResource(R.dimen.elevation_none),
            disabledElevation = dimensionResource(R.dimen.elevation_none),
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.height_button))
            .then(backgroundModifier),
    ) {
        if (isLoading) CircularProgressIndicator(
            color = contentColor,
            strokeWidth = dimensionResource(R.dimen.border_thin),
            modifier = Modifier.size(dimensionResource(R.dimen.size_loading_indicator)),
        )
        else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
                trailingIcon()
            }
        }
    }
}

@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.secondary,
        ),
        border = BorderStroke(
            width = dimensionResource(R.dimen.border_thin),
            color = if (enabled && !isLoading) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.height_button)),
    ) {
        if (isLoading) CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = dimensionResource(R.dimen.border_thin),
            modifier = Modifier.size(dimensionResource(R.dimen.size_loading_indicator)),
        )
        else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
                trailingIcon()
            }
        }
    }
}
