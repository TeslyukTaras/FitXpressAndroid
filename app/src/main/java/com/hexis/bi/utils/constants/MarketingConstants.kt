package com.hexis.bi.utils.constants

internal object MarketingEvents {
    const val LOGIN = "af_login"

    const val COMPLETE_REGISTRATION = "af_complete_registration"

    const val INITIATED_CHECKOUT = "af_initiated_checkout"

    const val PLAN_SELECTED = "subscription_plan_selected"
}

internal object MarketingParams {
    const val LOGIN_METHOD = "login_method"

    const val REGISTRATION_METHOD = "registration_method"

    const val PLAN_ID = "plan_id"

    const val PRODUCT_ID = "product_id"

    const val DISPLAYED_PRICE = "displayed_price"

    const val CURRENCY = "currency"
}

internal object MarketingMethods {
    const val EMAIL = "email"

    const val GOOGLE = "Google"

    const val APPLE = "Apple"
}

internal object MarketingConfig {
    const val REMOTE_KEY_SDK_KEY = "appsflyer_android_dev_key"

    const val REMOTE_KEY_SANDBOX = "appsflyer_android_sandbox"

    const val PENDING_REGISTRATION_PREFS = "pending_registration_method"

    const val MICROS_PER_UNIT = 1_000_000.0
}
