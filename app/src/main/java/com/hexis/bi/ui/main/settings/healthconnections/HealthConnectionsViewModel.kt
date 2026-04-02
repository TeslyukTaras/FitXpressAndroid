package com.hexis.bi.ui.main.settings.healthconnections

import android.app.Application
import com.hexis.bi.domain.enums.HealthProvider
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HealthConnectionsViewModel(
    application: Application,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(HealthConnectionsState())
    val state = _state.asStateFlow()

    fun toggleConnection(provider: HealthProvider) = _state.update { current ->
        val updated = if (provider in current.connectedProviders)
            current.connectedProviders - provider
        else
            current.connectedProviders + provider
        current.copy(connectedProviders = updated)
    }
}
