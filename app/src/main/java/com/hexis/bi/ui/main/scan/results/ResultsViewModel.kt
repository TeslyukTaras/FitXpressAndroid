package com.hexis.bi.ui.main.scan.results

import android.app.Application
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.ProfileConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ResultsViewModel(
    application: Application,
    private val userRepository: UserRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ResultsState())
    val state: StateFlow<ResultsState> = _state.asStateFlow()

    init {
        loadUnitSystem()
    }

    private fun loadUnitSystem() = launch {
        userRepository.getUser().onSuccess { profile ->
            _state.update {
                it.copy(isMetric = profile.unitSystem != ProfileConstants.UNIT_SYSTEM_IMPERIAL)
            }
        }
    }

    fun selectTab(tab: ResultsTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun toggleColorAnalysis() {
        _state.update { it.copy(colorAnalysisEnabled = !it.colorAnalysisEnabled) }
    }
}
