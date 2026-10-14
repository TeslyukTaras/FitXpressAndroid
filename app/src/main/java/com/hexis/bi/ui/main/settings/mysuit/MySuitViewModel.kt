package com.hexis.bi.ui.main.settings.mysuit

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.config.AppConfig
import com.hexis.bi.domain.suit.SuitRepository
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class MySuitViewModel(
    application: Application,
    private val suitRepository: SuitRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(MySuitState())
    val state: StateFlow<MySuitState> = _state.asStateFlow()

    init {
        if (AppConfig.suitVisible) suitRepository.connectionState
            .onEach { info ->
                _state.update {
                    if (info != null) {
                        it.copy(
                            isConnected = true,
                            connectedSuitId = info.suitId,
                            connectedStatus = info.status,
                        )
                    } else {
                        it.copy(
                            isConnected = false,
                            connectedSuitId = "",
                            connectedStatus = "",
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateSuitIdInput(value: String) {
        _state.update { it.copy(suitIdInput = value) }
    }

    fun connect() {
        if (!AppConfig.suitVisible) return
        val suitId = _state.value.suitIdInput.trim()
        if (suitId.isBlank()) return
        launch { suitRepository.connect(suitId) }
    }

    fun showReconnectDialog() {
        if (!AppConfig.suitVisible) return
        _state.update { it.copy(showReconnectDialog = true) }
    }

    fun dismissReconnectDialog() {
        _state.update { it.copy(showReconnectDialog = false) }
    }

    fun reconnect() {
        if (!AppConfig.suitVisible) return
        _state.update {
            it.copy(
                showReconnectDialog = false,
                suitIdInput = it.connectedSuitId,
            )
        }
        launch { suitRepository.disconnect() }
    }
}
