package com.hexis.bi.ui.main.body

import androidx.annotation.StringRes
import com.hexis.bi.R

enum class BodyTab {
    Stats,
    Visual,
    Posture,
    Compare;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            Stats -> R.string.body_tab_stats
            Visual -> R.string.body_tab_visual
            Posture -> R.string.body_tab_posture
            Compare -> R.string.body_tab_compare
        }
}

data class BodyState(
    val selectedTab: BodyTab = BodyTab.Stats,
)
