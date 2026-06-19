package com.hexis.bi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** The app's Material theme + [NocturnePulseTheme.extendedColors]. */
@Composable
fun NocturnePulseTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtendedColors provides DefaultExtendedColors) {
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
