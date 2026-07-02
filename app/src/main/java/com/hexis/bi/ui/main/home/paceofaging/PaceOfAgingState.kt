package com.hexis.bi.ui.main.home.paceofaging

import com.hexis.bi.domain.longevity.AgingLevel
import com.hexis.bi.ui.main.home.longevity.LongevityTrend

data class PaceOfAgingState(
    val hasData: Boolean = false,
    val level: AgingLevel = AgingLevel.Normal,
    val paceText: String = "",
    val meterFraction: Float = 0.5f,
    val percentText: String = "",
    val description: String = "",
    val syncedDate: String = "",
    val waistTrend: LongevityTrend? = null,
    val bodyFat: String? = null,
    val insight: String = "",
    val showInfoSheet: Boolean = false,
)
