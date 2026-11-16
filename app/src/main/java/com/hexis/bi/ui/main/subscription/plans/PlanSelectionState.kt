package com.hexis.bi.ui.main.subscription.plans

import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.SubscriptionPlan
import com.hexis.bi.ui.main.subscription.components.RestoreOutcome

data class PlanSelectionState(
    val plans: List<SubscriptionPlan>? = null,
    val selected: PlanId? = null,
    val currentPlan: PlanId? = null,
    val restore: RestoreOutcome? = null,
) {
    val isChangeMode: Boolean get() = currentPlan != null
    val canContinue: Boolean get() = selected != null && selected != currentPlan
}
