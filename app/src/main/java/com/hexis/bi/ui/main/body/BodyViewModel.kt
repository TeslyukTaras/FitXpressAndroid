package com.hexis.bi.ui.main.body

import android.app.Application
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BodyViewModel(
    application: Application,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(BodyState())
    val state: StateFlow<BodyState> = _state.asStateFlow()

    fun selectTab(tab: BodyTab) {
        _state.update { it.copy(selectedTab = tab) }
    }
}
