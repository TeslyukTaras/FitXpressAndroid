package com.hexis.bi.ui.main.settings.mysuit

import android.app.Application
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MySuitViewModel(application: Application) : BaseViewModel(application) {

    private val _state = MutableStateFlow(MySuitState())
    val state: StateFlow<MySuitState> = _state.asStateFlow()

    fun updateSuitIdInput(value: String) {
        _state.update { it.copy(suitIdInput = value) }
    }

    fun connect() {
        val suitId = _state.value.suitIdInput.trim()
        if (suitId.isBlank()) return
        _state.update { it.copy(isConnected = true, connectedSuitId = suitId) }
    }

    fun showReconnectDialog() {
        _state.update { it.copy(showReconnectDialog = true) }
    }

    fun dismissReconnectDialog() {
        _state.update { it.copy(showReconnectDialog = false) }
    }

    fun reconnect() {
        _state.update {
            it.copy(
                isConnected = false,
                showReconnectDialog = false,
                suitIdInput = it.connectedSuitId,
                connectedSuitId = "",
            )
        }
    }
}
