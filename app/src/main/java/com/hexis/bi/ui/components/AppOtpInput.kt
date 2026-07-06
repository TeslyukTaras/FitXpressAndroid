package com.hexis.bi.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.KeyboardType
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass

/**
 * A segmented one-time-code input: [length] equally sized boxes backed by a single hidden text
 * field, so the system keyboard drives every box. Non-digits are rejected and input is capped at
 * [length]. Set [isError] to render every box with the error border (the "Invalid code" state).
 */
@Composable
fun AppOtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 5,
    isError: Boolean = false,
    autoFocus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }

    if (autoFocus) LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = value,
        onValueChange = { input ->
            val sanitized = input.filter(Char::isDigit).take(length)
            if (sanitized != value) onValueChange(sanitized)
        },
        modifier = modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        // The raw text is never drawn; the boxes below are the only visible representation.
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.otp_cell_spacing),
                    Alignment.CenterHorizontally,
                ),
            ) {
                repeat(length) { index ->
                    OtpCell(
                        char = value.getOrNull(index),
                        isFocusedCell = index == value.length,
                        isError = isError,
                        modifier = Modifier.size(dimensionResource(R.dimen.otp_cell_size)),
                    )
                }
            }
        },
    )
}

@Composable
private fun OtpCell(
    char: Char?,
    isFocusedCell: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        char != null || isFocusedCell -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .glass(
                tint = NocturnePulseTheme.extendedColors.glassRimHighlight,
                shape = shape,
                level = GlassConstants.LEVEL_DEFAULT,
                fill = NocturnePulseTheme.extendedColors.surfaceTranslucent,
                backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                rimWidth = dimensionResource(R.dimen.glass_rim_width),
                backgroundAlpha = GlassConstants.TEXT_FIELD_BACKGROUND_ALPHA,
            )
            .border(dimensionResource(R.dimen.border_thin), borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (char != null) Text(
            text = char.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.wrapContentSize(),
        )
    }
}
