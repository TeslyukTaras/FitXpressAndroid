package com.hexis.bi.ui.components.profile

interface HealthParameters {
    val isMetric: Boolean
    val heightSliderValue: Float
    val heightSliderRange: ClosedFloatingPointRange<Float>
    val heightDisplayValue: Int
    val heightFeet: Int
    val heightInches: Int
    val weightSliderValue: Float
    val weightSliderRange: ClosedFloatingPointRange<Float>
    val weightDisplayValue: Int
}
