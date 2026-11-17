package com.hexis.bi.ui.main.scan.access

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.subscription.ScanGateConfig
import com.hexis.bi.data.subscription.SubscriptionRepository
import com.hexis.bi.domain.subscription.ScanAccess
import com.hexis.bi.domain.subscription.ScanBlockReason
import com.hexis.bi.domain.subscription.StorePlatform
import com.hexis.bi.domain.subscription.storePlatform
import com.hexis.bi.domain.subscription.toScanAccess
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.SubscriptionConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ScanAccessViewModel(
    application: Application,
    private val repository: SubscriptionRepository,
    private val gateConfig: ScanGateConfig,
) : BaseViewModel(application) {

    private val _access = MutableStateFlow<ScanAccess?>(null)
    val access: StateFlow<ScanAccess?> = _access.asStateFlow()

    init {
        viewModelScope.launch {
            gateConfig.refresh()
            if (!gateConfig.enabled) {
                _access.value = ScanAccess.Allowed
                return@launch
            }
            _access.value = resolveAccess()
            restoreWhenAllowed()
        }
    }

    val storePlatform: StorePlatform
        get() = repository.status.value.storePlatform ?: StorePlatform.ANDROID

    fun onFirstFreeScanAccepted() {
        _access.value = ScanAccess.Allowed
    }

    fun onScanBlocked(reason: ScanBlockReason) {
        _access.value = reason.toScanAccess()
    }

    private suspend fun restoreWhenAllowed() {
        repository.status.collect { status ->
            if (status.toScanAccess() == ScanAccess.Allowed && _access.value != ScanAccess.Allowed) {
                _access.value = ScanAccess.Allowed
            }
        }
    }

    private suspend fun resolveAccess(): ScanAccess =
        withTimeoutOrNull(SubscriptionConstants.SCAN_ACCESS_WAIT_MS) {
            repository.status.mapNotNull { it.toScanAccess() }.first()
        } ?: ScanAccess.Allowed
}
