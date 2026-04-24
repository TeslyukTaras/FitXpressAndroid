package com.hexis.bi.data.terra

/** Deep-link contract shared with the Terra widget redirect config and the AndroidManifest filter. */
object TerraDeepLinks {
    const val SCHEME = "fitxpress"
    const val HOST = "terra"
    const val PATH_SUCCESS = "/success"
    const val PATH_FAILURE = "/failure"

    const val SUCCESS_URL = "$SCHEME://$HOST$PATH_SUCCESS"
    const val FAILURE_URL = "$SCHEME://$HOST$PATH_FAILURE"

    const val PARAM_USER_ID = "user_id"
    const val PARAM_REFERENCE_ID = "reference_id"
    const val PARAM_RESOURCE = "resource"
    const val PARAM_REASON = "reason"
}
