package com.hexis.bi.data.subscription

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.StorePlatform
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.utils.constants.SubscriptionConstants
import java.time.LocalDate
import java.time.ZoneId

internal val ACCESS_STATES = setOf(
    SubscriptionConstants.STATE_ACTIVE,
    SubscriptionConstants.STATE_CANCEL_SCHEDULED,
    SubscriptionConstants.STATE_GRACE_PERIOD,
)

internal fun PlanId.productId(): String = when (this) {
    PlanId.ESSENTIAL -> SubscriptionConstants.PRODUCT_ESSENTIAL
    PlanId.PROGRESS -> SubscriptionConstants.PRODUCT_PROGRESS
    PlanId.TRANSFORMATION -> SubscriptionConstants.PRODUCT_TRANSFORMATION
}

internal val PLAN_PRODUCT_IDS: Set<String> = PlanId.entries.map { it.productId() }.toSet()

internal fun DocumentSnapshot.toSubscriptionStatus(): SubscriptionStatus {
    if (!exists()) return SubscriptionStatus.None
    val plan = getString(SubscriptionConstants.FIELD_PLAN_ID)
        ?.let { id -> PlanId.entries.firstOrNull { it.name.equals(id, ignoreCase = true) } }
        ?: return SubscriptionStatus.None
    val scansUsed = (getLong(SubscriptionConstants.FIELD_SCANS_USED) ?: 0L).toInt()
    val scansAllowed = getLong(SubscriptionConstants.FIELD_SCANS_ALLOWED)?.toInt() ?: plan.scansPerMonth
    val state = getString(SubscriptionConstants.FIELD_STATE)
    val platform = if (getString(SubscriptionConstants.FIELD_PLATFORM) == SubscriptionConstants.PLATFORM_IOS) {
        StorePlatform.IOS
    } else {
        StorePlatform.ANDROID
    }
    if (state in ACCESS_STATES && getTimestamp(SubscriptionConstants.FIELD_PERIOD_END) == null) {
        return SubscriptionStatus.None
    }
    val renewsOn = getTimestamp(SubscriptionConstants.FIELD_PERIOD_END)?.toLocalDate() ?: LocalDate.now()
    return when (state) {
        SubscriptionConstants.STATE_ACTIVE -> SubscriptionStatus.Active(plan, scansUsed, renewsOn, scansAllowed, platform)

        SubscriptionConstants.STATE_CANCEL_SCHEDULED ->
            SubscriptionStatus.Active(plan, scansUsed, renewsOn, scansAllowed, platform, renews = false)

        SubscriptionConstants.STATE_GRACE_PERIOD -> SubscriptionStatus.GracePeriod(
            plan = plan,
            scansUsed = scansUsed,
            renewsOn = renewsOn,
            graceEndsOn = getTimestamp(SubscriptionConstants.FIELD_GRACE_PERIOD_END)?.toLocalDate() ?: renewsOn,
            scansAllowed = scansAllowed,
            platform = platform,
        )

        SubscriptionConstants.STATE_ON_HOLD,
        SubscriptionConstants.STATE_EXPIRED -> SubscriptionStatus.Expired(plan, platform)

        else -> SubscriptionStatus.None
    }
}

private fun Timestamp.toLocalDate(): LocalDate =
    toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
