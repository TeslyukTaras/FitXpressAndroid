package com.hexis.bi.ui.main.subscription

import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.SubscriptionPlan
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.ui.main.subscription.components.RestoreOutcome

data class SubscriptionState(
    val status: SubscriptionStatus = SubscriptionStatus.None(),
    val plans: List<SubscriptionPlan>? = null,
    val showPlanDetails: Boolean = false,
    val restore: RestoreOutcome? = null,
) {
    fun priceOf(plan: PlanId): String? = plans?.firstOrNull { it.id == plan }?.formattedPrice

    fun scansOf(plan: PlanId): Int = plans?.firstOrNull { it.id == plan }?.scansPerMonth ?: plan.scansPerMonth
}
