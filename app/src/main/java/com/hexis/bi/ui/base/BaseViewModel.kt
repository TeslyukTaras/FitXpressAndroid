package com.hexis.bi.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // One-shot events (navigation, toasts, etc.) that survive recomposition
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    protected fun setError(message: String?) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }

    protected fun emitEvent(event: UiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    /**
     * Launch a coroutine with automatic loading state and error handling.
     *
     * @param showLoading whether to flip [isLoading] around the block
     * @param onError custom error handler; defaults to storing the message in [error]
     * @param block the suspending work to execute
     */
    protected fun launch(
        showLoading: Boolean = true,
        onError: (Throwable) -> Unit = { setError(it.message ?: "Unexpected error") },
        block: suspend CoroutineScope.() -> Unit,
    ) = viewModelScope.launch {
        try {
            if (showLoading) setLoading(true)
            block()
        } catch (e: Exception) {
            onError(e)
        } finally {
            if (showLoading) setLoading(false)
        }
    }
}
