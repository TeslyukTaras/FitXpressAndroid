package com.hexis.bi.utils.constants

object ProfileConstants {

    const val CM_TO_IN = 2.54f
    private const val INCHES_PER_FOOT = 12

    /** Converts centimeters to a (feet, inches) pair. */
    fun cmToFeetAndInches(cm: Float): Pair<Int, Float> {
        val totalInches = cm / CM_TO_IN
        val feet = (totalInches / INCHES_PER_FOOT).toInt()
        val inches = totalInches % INCHES_PER_FOOT
        return feet to inches
    }

    /** Converts total inches to a (feet, inches) pair. */
    fun inchesToFeetAndInches(inches: Int): Pair<Int, Int> {
        return (inches / INCHES_PER_FOOT) to (inches % INCHES_PER_FOOT)
    }

    const val KG_TO_LB = 2.20462f
    const val HEIGHT_CM_MIN = 130f
    const val HEIGHT_CM_MAX = 230f
    const val HEIGHT_IN_MIN = HEIGHT_CM_MIN / CM_TO_IN
    const val HEIGHT_IN_MAX = HEIGHT_CM_MAX / CM_TO_IN
    const val WEIGHT_KG_MIN = 40f
    const val WEIGHT_KG_MAX = 180f
    const val WEIGHT_LB_MIN = WEIGHT_KG_MIN * KG_TO_LB
    const val WEIGHT_LB_MAX = WEIGHT_KG_MAX * KG_TO_LB

    // Default values for new / empty profiles
    const val DEFAULT_HEIGHT_CM = 180f
    const val DEFAULT_WEIGHT_KG = 80f

    // Date formatting
    const val DOB_DATE_FORMAT = "dd/MM/yyyy"

    const val UNIT_SYSTEM_METRIC = "Metric"
    const val UNIT_SYSTEM_IMPERIAL = "Imperial"

    // Firebase Storage directory for avatar images
    const val AVATAR_STORAGE_DIR = "avatars"
}
