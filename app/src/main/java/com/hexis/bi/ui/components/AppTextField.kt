package com.hexis.bi.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.VisualTransformation
import com.hexis.bi.R

@Composable
fun AppTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isError = error != null
    val shape = MaterialTheme.shapes.small

    // Border: blue when focused, red when error, none otherwise
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> null
    }

    Column(modifier = modifier) {
        if (label != null) Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacer_s)),
            )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (borderColor != null)
                        Modifier.border(
                            dimensionResource(R.dimen.border_thin),
                            borderColor,
                            shape)
                    else Modifier
                ),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    isError = isError,
                    placeholder = placeholder?.let {
                        {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isFocused) MaterialTheme.colorScheme.primaryFixed else MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                    trailingIcon = trailingIcon,
                    shape = shape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.tertiary,
                        errorContainerColor = MaterialTheme.colorScheme.tertiary,
                        focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
                        errorIndicatorColor = MaterialTheme.colorScheme.tertiary,
                        // Trailing icon colour is controlled per state so
                        // Icon composables in the slot can omit explicit tint
                        focusedTrailingIconColor = MaterialTheme.colorScheme.secondary,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.secondary,
                        errorTrailingIconColor = MaterialTheme.colorScheme.error,
                    ),
                    contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_medium)),
                )
            },
        )

        if (error != null) Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = dimensionResource(R.dimen.spacer_s)),
            )
    }
}
