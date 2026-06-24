package com.hexis.bi.data.terra

import com.hexis.bi.BuildConfig

object TerraConfig {
    val devId: String get() = BuildConfig.TERRA_DEV_ID
    val environment: String get() = BuildConfig.ENVIRONMENT
    val functionPrefix: String get() = BuildConfig.TERRA_FUNCTION_PREFIX

    /** Dev builds expose Terra's DUMMY provider in the widget and prefer the API path. */
    val isSandbox: Boolean get() = BuildConfig.TERRA_INCLUDE_DUMMY_PROVIDER
}
