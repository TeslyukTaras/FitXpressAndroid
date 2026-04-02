package com.hexis.bi.utils

object ProfileConstants {

    const val CM_TO_IN = 2.54f
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
