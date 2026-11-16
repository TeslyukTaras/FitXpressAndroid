package com.hexis.bi.ui.main.subscription.confirm

import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.SubscriptionPlan
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.ui.base.UiEvent

enum class PlanChangeKind { SUBSCRIBE, UPGRADE, DOWNGRADE }

data class ConfirmPlanState(
    val planId: PlanId,
    val plans: List<SubscriptionPlan>? = null,
    val status: SubscriptionStatus = SubscriptionStatus.None,
    val completed: Boolean = false,
) {
    val plan: SubscriptionPlan? get() = plans?.firstOrNull { it.id == planId }

    val scansPerMonth: Int get() = plan?.scansPerMonth ?: planId.scansPerMonth

    private fun rankOf(id: PlanId): Int = plans?.firstOrNull { it.id == id }?.rank ?: id.rank

    val currentPrice: String?
        get() = plans?.firstOrNull { it.id == current?.plan }?.formattedPrice

    val current: SubscriptionStatus.Subscribed? get() = status as? SubscriptionStatus.Subscribed

    val kind: PlanChangeKind
        get() {
            val currentPlan = current?.plan ?: return PlanChangeKind.SUBSCRIBE
            return if (rankOf(planId) < rankOf(currentPlan)) PlanChangeKind.UPGRADE else PlanChangeKind.DOWNGRADE
        }
}

sealed interface ConfirmPlanEvent : UiEvent {
    data object Completed : ConfirmPlanEvent
}
