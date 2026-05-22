package com.hexis.bi.ui.dark

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import com.hexis.bi.R
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass

@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search_placeholder),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = MaterialTheme.shapes.small
    val borderColor =
        if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .glass(
                shape = shape,
                level = GlassConstants.LEVEL_DEFAULT,
                fill = DarkTheme.extendedColors.surfaceTranslucent,
                backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                rimWidth = dimensionResource(R.dimen.glass_rim_width),
                backgroundAlpha = GlassConstants.TEXT_FIELD_BACKGROUND_ALPHA,
            )
            .border(
                dimensionResource(R.dimen.border_line),
                borderColor.copy(alpha = GlassConstants.TEXT_FIELD_BORDER_ALPHA),
                shape,
            ),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
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
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = if (isFocused || value.isNotEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                    )
                },
                trailingIcon = if (value.isNotEmpty()) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_cross),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(dimensionResource(R.dimen.icon_medium))
                                .clickable { onValueChange("") },
                        )
                    }
                } else null,
                shape = shape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_m)),
            )
        },
    )
}
