package com.hexis.bi.ui.main.scan.results

import android.app.Application
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ResultsViewModel(application: Application) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ResultsState())
    val state: StateFlow<ResultsState> = _state.asStateFlow()

    fun selectTab(tab: ResultsTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun toggleColorAnalysis() {
        _state.update { it.copy(colorAnalysisEnabled = !it.colorAnalysisEnabled) }
    }
}
