package com.hexis.bi.ui.main.subscription

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.subscription.SubscriptionRepository
import com.hexis.bi.domain.subscription.RestoreResult
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.main.subscription.components.RestoreOutcome
import com.hexis.bi.utils.constants.SubscriptionConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SubscriptionViewModel(
    application: Application,
    private val repository: SubscriptionRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(SubscriptionState(status = repository.status.value))
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.status.collect { status ->
                _state.update {
                    it.copy(
                        status = status,
                        showPlanDetails = it.showPlanDetails &&
                            (status is SubscriptionStatus.Subscribed || status is SubscriptionStatus.Expired),
                    )
                }
            }
        }
    }

    override fun onInitialize() {
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

    fun showPlanDetails() {
        _state.update { it.copy(showPlanDetails = true) }
    }

    fun dismissPlanDetails() {
        _state.update { it.copy(showPlanDetails = false) }
    }

    fun restorePurchases() {
        launch(
            onError = { _state.update { it.copy(restore = RestoreOutcome(RestoreResult.FAILED, null)) } },
        ) {
            val result = repository.restorePurchases()
            val plan = if (result == RestoreResult.RESTORED) {
                withTimeoutOrNull(SubscriptionConstants.RESTORE_STATUS_WAIT_MS) {
                    (repository.status.first { it is SubscriptionStatus.Subscribed } as SubscriptionStatus.Subscribed).plan
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
}
