package com.hexis.bi.ui.main.scan

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.domain.suit.SuitRepository
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class ScanViewModel(
    application: Application,
    private val suitRepository: SuitRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    init {
        suitRepository.connectionState
            .onEach { info ->
                _state.update { it.copy(suitConnected = info != null) }
            }
            .launchIn(viewModelScope)
    }
}
