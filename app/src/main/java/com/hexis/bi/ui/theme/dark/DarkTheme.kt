package com.hexis.bi.ui.theme.dark

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.hexis.bi.ui.theme.AppShapes
import com.hexis.bi.ui.theme.Typography

/** Dark Material theme + [DarkTheme.extendedColors]. */
@Composable
fun DarkTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkExtendedColors provides DefaultDarkExtendedColors) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object DarkTheme {
    val extendedColors: DarkExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDarkExtendedColors.current
}
