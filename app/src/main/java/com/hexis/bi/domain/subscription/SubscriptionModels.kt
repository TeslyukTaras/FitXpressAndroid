package com.hexis.bi.domain.subscription

import com.hexis.bi.utils.constants.SubscriptionConstants
import java.time.LocalDate

enum class PlanId(val scansPerMonth: Int, val rank: Int) {
    ESSENTIAL(SubscriptionConstants.ESSENTIAL_SCANS_PER_MONTH, SubscriptionConstants.ESSENTIAL_RANK),
    PROGRESS(SubscriptionConstants.PROGRESS_SCANS_PER_MONTH, SubscriptionConstants.PROGRESS_RANK),
    TRANSFORMATION(
        SubscriptionConstants.TRANSFORMATION_SCANS_PER_MONTH,
        SubscriptionConstants.TRANSFORMATION_RANK,
    ),
}

val PlanId.isTopPlan: Boolean
    get() = rank == PlanId.entries.minOf { it.rank }

data class PlanInfo(
    val id: PlanId,
    val rank: Int,
    val scansPerMonth: Int,
    val mostPopular: Boolean,
)

data class SubscriptionPlan(
    val id: PlanId,
    val formattedPrice: String,
    val scansPerMonth: Int = id.scansPerMonth,
    val rank: Int = id.rank,
    val mostPopular: Boolean = false,
)

enum class RestoreResult { RESTORED, NOT_FOUND, FAILED }

enum class PurchaseResult { COMPLETED, CANCELLED }

enum class StorePlatform { ANDROID, IOS }

sealed interface SubscriptionStatus {

    data object Loading : SubscriptionStatus

    data class None(val freeScanUsed: Boolean = false) : SubscriptionStatus

    sealed interface Subscribed : SubscriptionStatus {
        val plan: PlanId
        val scansUsed: Int
        val scansAllowed: Int
        val renewsOn: LocalDate
        val renewsAtMillis: Long
        val platform: StorePlatform
    }

    data class Active(
        override val plan: PlanId,
        override val scansUsed: Int,
        override val renewsOn: LocalDate,
        override val scansAllowed: Int = plan.scansPerMonth,
        override val platform: StorePlatform = StorePlatform.ANDROID,
        val renews: Boolean = true,
        val pendingPlan: PlanId? = null,
        override val renewsAtMillis: Long = 0L,
    ) : Subscribed

    data class GracePeriod(
        override val plan: PlanId,
        override val scansUsed: Int,
        override val renewsOn: LocalDate,
        val graceEndsOn: LocalDate,
        override val scansAllowed: Int = plan.scansPerMonth,
        override val platform: StorePlatform = StorePlatform.ANDROID,
        override val renewsAtMillis: Long = 0L,
    ) : Subscribed

    data class Expired(
        val plan: PlanId,
        val platform: StorePlatform = StorePlatform.ANDROID,
        val paymentIssue: Boolean = false,
    ) : SubscriptionStatus
}

val SubscriptionStatus.storePlatform: StorePlatform?
    get() = when (this) {
        is SubscriptionStatus.Subscribed -> platform
        is SubscriptionStatus.Expired -> platform
        else -> null
    }

val SubscriptionStatus.Subscribed.scansRemaining: Int
    get() = (scansAllowed - scansUsed).coerceAtLeast(0)

val SubscriptionStatus.Subscribed.allScansUsed: Boolean get() = scansRemaining == 0
