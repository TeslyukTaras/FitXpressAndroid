package com.hexis.bi.utils.constants

internal object AppUpdateRemoteConfig {

    const val MINIMUM_VERSION_KEY = "minimum_supported_version_android"

    const val DEBUG_FETCH_INTERVAL_SECONDS = 60L
    const val RELEASE_FETCH_INTERVAL_SECONDS = 60L * 60
}

internal object AppUpdateStore {

    const val PACKAGE_NAME = "com.hexis.bi"
    const val MARKET_URI = "market://details?id=$PACKAGE_NAME"
    const val PLAY_STORE_URI = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"
}
