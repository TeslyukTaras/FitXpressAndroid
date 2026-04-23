package com.hexis.bi.data.terra

import com.hexis.bi.BuildConfig

/** Sourced from local.properties (`terra.dev.id`, `terra.api.key`) via BuildConfig. */
object TerraConfig {
    val devId: String get() = BuildConfig.TERRA_DEV_ID
    val apiKey: String get() = BuildConfig.TERRA_API_KEY
    val environment: String get() = BuildConfig.ENVIRONMENT

    /** Dev builds expose Terra's DUMMY provider in the widget and prefer the API path. */
    val isSandbox: Boolean get() = BuildConfig.TERRA_INCLUDE_DUMMY_PROVIDER
}
