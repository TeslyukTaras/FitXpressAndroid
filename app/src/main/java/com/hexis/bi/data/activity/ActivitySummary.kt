package com.hexis.bi.data.activity

import java.time.LocalDate

data class ActivitySummary(
    val date: LocalDate,
    val steps: Int,
    val distanceKm: Float,
    val activeCalories: Int,
    val hourlySteps: Map<Int, Int> = emptyMap(),
)
