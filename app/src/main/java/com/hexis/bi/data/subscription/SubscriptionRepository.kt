package com.hexis.bi.data.subscription

import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.PurchaseResult
import com.hexis.bi.domain.subscription.RestoreResult
import com.hexis.bi.domain.subscription.SubscriptionPlan
import com.hexis.bi.domain.subscription.SubscriptionStatus
import kotlinx.coroutines.flow.StateFlow

interface SubscriptionRepository {
    val status: StateFlow<SubscriptionStatus>
    suspend fun loadPlans(): List<SubscriptionPlan>
    suspend fun subscribe(plan: PlanId): PurchaseResult
    suspend fun changePlan(plan: PlanId): PurchaseResult
    suspend fun restorePurchases(): RestoreResult
}
