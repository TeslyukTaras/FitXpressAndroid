package com.hexis.bi.domain.subscription

enum class HealthDataAccess { FULL, FIRST_WEEK, FROZEN }

fun SubscriptionStatus.toHealthDataAccess(): HealthDataAccess? = when (this) {
    SubscriptionStatus.Loading -> null
    is SubscriptionStatus.None -> HealthDataAccess.FIRST_WEEK
    is SubscriptionStatus.Expired -> HealthDataAccess.FROZEN
    is SubscriptionStatus.Subscribed -> HealthDataAccess.FULL
}
