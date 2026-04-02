package com.hexis.bi.ui.base

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel(
    application: Application,
    initialLoading: Boolean = false,
) : AndroidViewModel(application) {

    protected val appContext: Context get() = getApplication()

    private val _isLoading = MutableStateFlow(initialLoading)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // One-shot events (navigation, toasts, etc.) — Channel guarantees delivery
    // even if emitted before the collector is active (unlike SharedFlow with replay=0)
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    protected fun setError(message: String?) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }

    protected fun setMessage(msg: String?) {
        _message.value = msg
    }

    fun clearMessage() {
        _message.value = null
    }

    protected fun emitEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
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
