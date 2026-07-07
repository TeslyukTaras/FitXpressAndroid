package com.hexis.bi.ui.theme

import androidx.compose.foundation.text.LocalAutofillHighlightBrush
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.SolidColor

private const val AUTOFILL_HIGHLIGHT_ALPHA = 0.2f

/** The app's Material theme + [NocturnePulseTheme.extendedColors]. */
@Composable
fun NocturnePulseTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalExtendedColors provides DefaultExtendedColors,
        LocalAutofillHighlightBrush provides SolidColor(ActionTeal.copy(alpha = AUTOFILL_HIGHLIGHT_ALPHA)),
    ) {
        MaterialTheme(
            colorScheme = NocturnePulseColorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object NocturnePulseTheme {
    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
