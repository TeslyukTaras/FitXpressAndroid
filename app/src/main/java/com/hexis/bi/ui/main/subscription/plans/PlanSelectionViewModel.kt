package com.hexis.bi.ui.main.subscription.plans

import com.hexis.bi.data.subscription.productId
import com.hexis.bi.data.telemetry.MarketingAnalytics
import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.subscription.SubscriptionRepository
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.RestoreResult
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.main.subscription.components.RestoreOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.hexis.bi.utils.constants.SubscriptionConstants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch

class PlanSelectionViewModel(
    application: Application,
    private val repository: SubscriptionRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(
        PlanSelectionState(currentPlan = repository.status.value.subscribedPlan()),
    )
    val state: StateFlow<PlanSelectionState> = _state.asStateFlow()

    override fun onInitialize() {
        viewModelScope.launch {
            repository.status.collect { status ->
                _state.update {
                    it.copy(currentPlan = status.subscribedPlan())
                }
            }
        }
        loadPlans()
    }

    fun dismissError() {
        clearError()
        if (_state.value.plans == null) loadPlans()
    }

    private fun loadPlans() {
        launch(showLoading = false) {
            val plans = repository.loadPlans()
            _state.update { it.copy(plans = plans) }
        }
    }

    fun selectPlan(plan: PlanId) {
        _state.update { it.copy(selected = plan) }
    }

    fun confirmPlan(plan: PlanId) {
        val offer = _state.value.plans?.firstOrNull { it.id == plan } ?: return
        MarketingAnalytics.planSelected(appContext, plan, plan.productId(), offer.priceMicros ?: return, offer.currency ?: return)
    }

    fun restorePurchases() {
        launch(
            onError = { _state.update { it.copy(restore = RestoreOutcome(RestoreResult.FAILED, null)) } },
        ) {
            val result = repository.restorePurchases()
            val plan = if (result == RestoreResult.RESTORED) {
                withTimeoutOrNull(SubscriptionConstants.RESTORE_STATUS_WAIT_MS) {
                    repository.status.first { it is SubscriptionStatus.Subscribed }.subscribedPlan()
                }
            } else {
                null
            }
            val outcome = if (result == RestoreResult.RESTORED && plan == null) {
                RestoreOutcome(RestoreResult.FAILED, null)
            } else {
                RestoreOutcome(result, plan)
            }
            _state.update { it.copy(restore = outcome) }
        }
    }

    fun dismissRestore() {
        _state.update { it.copy(restore = null) }
    }

    private fun SubscriptionStatus.subscribedPlan(): PlanId? =
        (this as? SubscriptionStatus.Subscribed)?.plan
}
