package com.hexis.bi.utils

import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.MeasurementConstants.CM_TO_IN
import com.hexis.bi.utils.constants.MeasurementConstants.INCHES_PER_FOOT
import com.hexis.bi.utils.constants.MeasurementConstants.KG_TO_LB
import com.hexis.bi.utils.constants.MeasurementConstants.KM_TO_MI
import com.hexis.bi.utils.constants.MeasurementConstants.UNIT_SYSTEM_METRIC
import kotlin.math.roundToInt

fun Float.cmToInches(): Float = this / CM_TO_IN
fun Float.inchesToCm(): Float = this * CM_TO_IN

fun Float.kgToLb(): Float = this * KG_TO_LB
fun Float.lbToKg(): Float = this / KG_TO_LB

fun Float.kmToMiles(): Float = this * KM_TO_MI
fun Float.milesToKm(): Float = this / KM_TO_MI

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

/** Rounds total height first so display never produces values like 5 ft 12 in. */
fun Float.cmToRoundedFeetAndInches(): Pair<Int, Int> =
    this.cmToInches().roundToInt().inchesToFeetAndInches()

data class PersistedUserMeasurements(
    val heightCm: Int,
    val weightKg: Int,
    val heightIn: Int,
    val weightLb: Int,
)

fun persistedUserMeasurements(heightCm: Float, weightKg: Float): PersistedUserMeasurements =
    PersistedUserMeasurements(
        heightCm = heightCm.roundToInt(),
        weightKg = weightKg.roundToInt(),
        heightIn = heightCm.cmToInches().roundToInt(),
        weightLb = weightKg.kgToLb().roundToInt(),
    )

fun String?.isMetricUnitSystem(fallback: Boolean = true): Boolean =
    if (this == null) fallback else this == UNIT_SYSTEM_METRIC

/** Stride length in cm based on height and gender. */
fun strideLengthCm(heightCm: Float, isFemale: Boolean): Float =
    heightCm * if (isFemale) ActivityConstants.STRIDE_FACTOR_FEMALE else ActivityConstants.STRIDE_FACTOR_MALE

/** Distance goal in km derived from step goal, height, and gender. */
fun distanceGoalKm(stepGoal: Int, heightCm: Float, isFemale: Boolean): Float =
    (stepGoal * strideLengthCm(heightCm, isFemale)) / ActivityConstants.CM_PER_KM

/** Active calories goal derived from distance goal and body weight. */
fun caloriesGoal(distanceGoalKm: Float, weightKg: Float): Int =
    (distanceGoalKm * weightKg * ActivityConstants.CALORIES_PER_KM_PER_KG)
        .roundToInt()
        .coerceAtLeast(0)
