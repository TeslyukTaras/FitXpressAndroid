package com.hexis.bi.utils

import com.hexis.bi.utils.constants.MeasurementConstants.CM_TO_IN
import com.hexis.bi.utils.constants.MeasurementConstants.INCHES_PER_FOOT
import com.hexis.bi.utils.constants.MeasurementConstants.KG_TO_LB
import com.hexis.bi.utils.constants.MeasurementConstants.UNIT_SYSTEM_METRIC

fun Float.cmToInches(): Float = this / CM_TO_IN
fun Float.inchesToCm(): Float = this * CM_TO_IN

fun Float.kgToLb(): Float = this * KG_TO_LB
fun Float.lbToKg(): Float = this / KG_TO_LB

/** Splits centimeters into whole feet and remaining inches (as a float). */
fun Float.cmToFeetAndInches(): Pair<Int, Float> {
    val totalInches = this.cmToInches()
    val feet = (totalInches / INCHES_PER_FOOT).toInt()
    val inches = totalInches % INCHES_PER_FOOT
    return feet to inches
}

/** Splits a whole-inch value into (feet, inches). */
fun Int.inchesToFeetAndInches(): Pair<Int, Int> =
    (this / INCHES_PER_FOOT) to (this % INCHES_PER_FOOT)

fun String?.isMetricUnitSystem(fallback: Boolean = true): Boolean =
    if (this == null) fallback else this == UNIT_SYSTEM_METRIC
