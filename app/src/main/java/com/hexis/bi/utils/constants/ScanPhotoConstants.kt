package com.hexis.bi.utils.constants

import com.google.firebase.functions.FirebaseFunctionsException

object ScanPhotoConstants {

    const val SCAN_PHOTO_COMPRESSION_THRESHOLD_BYTES = 1L * 1024 * 1024

    const val SCAN_PHOTO_MAX_BYTES = 2L * 1024 * 1024

    const val SCAN_PHOTO_JPEG_QUALITY = 88
    const val SCAN_PHOTO_MIN_JPEG_QUALITY = 60
    const val SCAN_PHOTO_QUALITY_STEP = 8

    const val MAX_SAME_CAPTURE_RETRIES = 3

    val TRANSIENT_FUNCTION_CODES = setOf(
        FirebaseFunctionsException.Code.UNAVAILABLE,
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
        FirebaseFunctionsException.Code.ALREADY_EXISTS,
    )
}
