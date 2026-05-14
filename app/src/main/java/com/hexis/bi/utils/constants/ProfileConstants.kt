package com.hexis.bi.utils.constants

import com.hexis.bi.utils.constants.MeasurementConstants.CM_TO_IN
import com.hexis.bi.utils.constants.MeasurementConstants.KG_TO_LB

object ProfileConstants {

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

    // Firebase Storage directory for avatar images
    const val AVATAR_STORAGE_DIR = "avatars"
    const val PROFILE_IMAGE_FILE_NAME = "profile.jpg"
    const val PROFILE_IMAGE_CONTENT_TYPE = "image/jpeg"
    const val PROFILE_IMAGE_MAX_DIMENSION_PX = 1024
    const val PROFILE_IMAGE_JPEG_QUALITY = 88
    const val PROFILE_IMAGE_MIN_JPEG_QUALITY = 60
    const val PROFILE_IMAGE_QUALITY_STEP = 8
    const val PROFILE_IMAGE_MAX_BYTES = 4 * 1024 * 1024
}
