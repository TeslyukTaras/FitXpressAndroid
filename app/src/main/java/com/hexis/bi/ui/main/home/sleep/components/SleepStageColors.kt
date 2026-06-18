package com.hexis.bi.ui.main.home.sleep.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hexis.bi.data.sleep.SleepStage
import com.hexis.bi.ui.theme.NocturnePulseTheme

/**
 * Resolves the theme colour for each [SleepStage]. Returns a plain lambda (the colours are read
 * from [NocturnePulseTheme.extendedColors] in composition) so it can also be used inside
 * non-composable draw scopes.
 */
@Composable
internal fun rememberSleepStageColors(): (SleepStage) -> Color {
    val colors = NocturnePulseTheme.extendedColors
    return { stage ->
        when (stage) {
            SleepStage.Deep -> colors.sleepStageDeep
            SleepStage.REM -> colors.sleepStageRem
            SleepStage.Light -> colors.sleepStageLight
            SleepStage.Awake -> colors.sleepStageAwake
        }
    }
}
