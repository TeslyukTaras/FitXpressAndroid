package com.hexis.bi.utils.constants

internal object SubscriptionConstants {
    const val ESSENTIAL_SCANS_PER_MONTH = 2
    const val PROGRESS_SCANS_PER_MONTH = 4
    const val TRANSFORMATION_SCANS_PER_MONTH = 6

    const val ESSENTIAL_RANK = 3
    const val PROGRESS_RANK = 2
    const val TRANSFORMATION_RANK = 1

    const val RESTORE_STATUS_WAIT_MS = 3_000L

    const val PLAY_BASE_PLAN_ID = "monthly"
    const val PRODUCT_ESSENTIAL = "hexis_bi_essential"
    const val PRODUCT_PROGRESS = "hexis_bi_progress"
    const val PRODUCT_TRANSFORMATION = "hexis_bi_transformation"
    const val PLATFORM_ANDROID = "android"
    const val PLATFORM_IOS = "ios"
    const val APPLE_SUBSCRIPTIONS_URL = "https://apps.apple.com/account/subscriptions"
    const val FUNCTION_VERIFY_PURCHASE = "verifyPurchase"

    const val COLLECTION_SUBSCRIPTIONS = "subscriptions"
    const val FIELD_STATE = "state"
    const val FIELD_PLATFORM = "platform"
    const val FIELD_PRODUCT_ID = "productId"
    const val FIELD_PURCHASE_TOKEN = "purchaseToken"
    const val FIELD_PLAN_ID = "planId"
    const val FIELD_PERIOD_END = "periodEnd"
    const val FIELD_GRACE_PERIOD_END = "gracePeriodEnd"
    const val FIELD_SCANS_USED = "scansUsed"
    const val FIELD_SCANS_ALLOWED = "scansAllowed"

    const val STATE_ACTIVE = "active"
    const val STATE_CANCEL_SCHEDULED = "cancelScheduled"
    const val STATE_GRACE_PERIOD = "gracePeriod"
    const val STATE_ON_HOLD = "onHold"
    const val STATE_EXPIRED = "expired"

    const val REMOTE_CONFIG_PLANS_KEY = "subscription_plans"
    const val PLAN_BADGE_MOST_POPULAR = "mostPopular"
    const val PLANS_DEBUG_FETCH_INTERVAL_SECONDS = 60L
    const val PLANS_RELEASE_FETCH_INTERVAL_SECONDS = 60L * 60

    const val BILLING_UNSOLICITED_BUFFER = 8
    const val SNAPSHOT_RETRY_BASE_MS = 1_000L
    const val SNAPSHOT_RETRY_MAX_MS = 30_000L
    const val SNAPSHOT_RETRY_MAX_SHIFT = 5L

    const val PLAY_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"
    const val PLAY_SUBSCRIPTIONS_PACKAGE_PARAM = "package"

    const val FEATURE_TILE_ALPHA = 0.15f
    const val EMPTY_TILE_ALPHA = 0.1f
}
