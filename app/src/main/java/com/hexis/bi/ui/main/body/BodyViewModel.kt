package com.hexis.bi.ui.main.body

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BodyViewModel(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(BodyState())
    val state: StateFlow<BodyState> = _state.asStateFlow()

    init {
        loadScanHistory()
    }

    fun retry() = loadScanHistory()

    private fun loadScanHistory() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            scanHistoryRepository.getRecentScans().fold(
                onSuccess = { records ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            scans = records.map { record ->
                                BodyScanItem(
                                    id = record.id,
                                    timestamp = record.timestamp,
                                    hasModel3d = !record.model3dUrl.isNullOrBlank(),
                                    measurementsCount = record.measurements.size,
                                )
                            },
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            scans = emptyList(),
                            errorMessage = error.message ?: "Failed to load scan history",
                        )
                    }
                },
            )
        }
    }
}
