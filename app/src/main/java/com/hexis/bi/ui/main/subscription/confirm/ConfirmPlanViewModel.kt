package com.hexis.bi.ui.main.subscription.confirm

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.subscription.SubscriptionRepository
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.PurchaseResult
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.utils.constants.SubscriptionConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ConfirmPlanViewModel(
    application: Application,
    private val repository: SubscriptionRepository,
    planId: PlanId,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(
        ConfirmPlanState(planId = planId, status = repository.status.value),
    )
    val state: StateFlow<ConfirmPlanState> = _state.asStateFlow()

    override fun onInitialize() {
        viewModelScope.launch {
            repository.status.collect { status ->
                _state.update { if (it.completed) it else it.copy(status = status) }
                val state = _state.value
                if (!confirming && !state.completed && state.isCurrentPlan) leave()
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

    private var confirming = false
    private var leaving = false

    fun confirm() {
        if (confirming) return
        confirming = true
        val planId = _state.value.planId
        launch {
            try {
                val status = withTimeoutOrNull(SubscriptionConstants.STATUS_WAIT_MS) {
                    repository.status.first { it !is SubscriptionStatus.Loading }
                } ?: throw IllegalStateException(appContext.getString(R.string.subscription_error_unavailable))
                if (status is SubscriptionStatus.Subscribed && status.plan == planId) {
                    leave()
                    return@launch
                }
                val kind = _state.value.kind
                val result = if (status !is SubscriptionStatus.Subscribed) {
                    repository.subscribe(planId)
                } else {
                    repository.changePlan(planId)
                }
                if (result != PurchaseResult.COMPLETED) return@launch
                _state.update { it.copy(completed = true) }
                if (kind == PlanChangeKind.DOWNGRADE || awaitPlan(planId)) {
                    emitEvent(ConfirmPlanEvent.Completed)
                } else {
                    _state.update { it.copy(awaitingConfirmation = true) }
                    viewModelScope.launch {
                        repository.status.first { it.isPlan(planId) }
                        _state.update { it.copy(awaitingConfirmation = false) }
                        emitEvent(ConfirmPlanEvent.Completed)
                    }
                }
            } finally {
                confirming = false
            }
        }
    }

    private fun leave() {
        if (leaving) return
        leaving = true
        emitEvent(UiEvent.NavigateBack)
    }

    private suspend fun awaitPlan(planId: PlanId): Boolean =
        withTimeoutOrNull(SubscriptionConstants.PURCHASE_CONFIRM_WAIT_MS) {
            repository.status.first { it.isPlan(planId) }
        } != null

    private fun SubscriptionStatus.isPlan(planId: PlanId): Boolean =
        this is SubscriptionStatus.Subscribed && plan == planId
}
