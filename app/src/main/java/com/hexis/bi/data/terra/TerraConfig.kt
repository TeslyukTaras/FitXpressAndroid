package com.hexis.bi.data.terra

import com.hexis.bi.BuildConfig
import com.hexis.bi.utils.constants.TerraEnvironments

object TerraConfig {
    val devId: String get() = BuildConfig.TERRA_DEV_ID
    val environment: String get() = BuildConfig.ENVIRONMENT
    val functionPrefix: String get() = BuildConfig.TERRA_FUNCTION_PREFIX

    val terraEnvironment: String
        get() = if (functionPrefix == TerraEnvironments.PROD_FUNCTION_PREFIX) {
            TerraEnvironments.PROD
        } else {
            TerraEnvironments.DEV
        }

    /** Dev builds expose Terra's DUMMY provider in the widget and prefer the API path. */
    val isSandbox: Boolean get() = BuildConfig.TERRA_INCLUDE_DUMMY_PROVIDER
}
