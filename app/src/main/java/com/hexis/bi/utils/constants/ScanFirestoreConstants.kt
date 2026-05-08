package com.hexis.bi.utils.constants

object ScanFirestoreConstants {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_SCANS = "scans"

    const val SUB_CIRCUMFERENCE_PARAMS = "circumferenceParams"
    const val SUB_FRONT_LINEAR_PARAMS = "frontLinearParams"
    const val SUB_SIDE_LINEAR_PARAMS = "sideLinearParams"
    const val SUB_SUBSCRIPTION_INFO = "subscriptionInfo"

    const val PARAMS_DOC_ID = "data"

    const val FIELD_ID = "id"
    const val FIELD_STATUS = "status"
    const val FIELD_URL = "url"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_COMPLETED_AT = "completedAt"
    const val FIELD_SAVED_AT = "savedAt"
    const val FIELD_GENDER = "gender"
    const val FIELD_MODEL_3D_URL = "model3dUrl"
    const val FIELD_MODEL_PREVIEW_PNG_BASE64 = "modelPreviewPngBase64"
    const val FIELD_HEIGHT = "height"
    const val FIELD_WEIGHT = "weight"
    const val FIELD_AGE = "age"
    const val FIELD_BMI = "bmi"
    const val FIELD_BMR = "bmr"
    const val FIELD_FAT_PERCENTAGE = "fatPercentage"
    const val FIELD_LEAN_BODY_MASS = "leanBodyMass"
    const val FIELD_FAT_BODY_MASS = "fatBodyMass"
    const val FIELD_ESTIMATED_BMI = "estimatedBmi"
    const val FIELD_ESTIMATED_BMR = "estimatedBmr"
    const val FIELD_ESTIMATED_WEIGHT = "estimatedWeight"
    const val FIELD_ESTIMATED_LEAN_BODY_MASS = "estimatedLeanBodyMass"
    const val FIELD_ESTIMATED_FAT_BODY_MASS = "estimatedFatBodyMass"

    const val SCAN_HISTORY_MAX_SCANS = 100L
    const val SCAN_HISTORY_DEFAULT_LIMIT = 20L
    const val SCAN_HISTORY_TIMESTAMP_LOOKBACK = 50L
}
