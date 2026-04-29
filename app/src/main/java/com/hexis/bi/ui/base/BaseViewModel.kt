package com.hexis.bi.ui.base

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
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
import timber.log.Timber

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

    // Channel (not SharedFlow) so events emitted before the collector subscribes are still delivered.
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    protected fun string(@StringRes resId: Int, vararg formatArgs: Any): String =
        appContext.getString(resId, *formatArgs)

    protected fun setError(message: String?) {
        if (message != null) {
            Timber.tag(this::class.java.simpleName).e("UI error: %s", message)
        }
        _error.value = message
    }

    protected fun setError(@StringRes resId: Int, vararg formatArgs: Any) {
        setError(appContext.getString(resId, *formatArgs))
    }

    fun clearError() {
        _error.value = null
    }

    protected fun setMessage(msg: String?) {
        if (msg != null) {
            Timber.tag(this::class.java.simpleName).i("UI message: %s", msg)
        }
        _message.value = msg
    }

    protected fun setMessage(@StringRes resId: Int, vararg formatArgs: Any) {
        setMessage(appContext.getString(resId, *formatArgs))
    }

    fun clearMessage() {
        _message.value = null
    }

    protected fun emitEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    /** Launches a coroutine with automatic loading state and error handling. */
    protected fun launch(
        showLoading: Boolean = true,
        onError: (Throwable) -> Unit = { setError(it.message ?: "Unexpected error") },
        block: suspend CoroutineScope.() -> Unit,
    ) = viewModelScope.launch {
        try {
            if (showLoading) setLoading(true)
            block()
        } catch (e: Exception) {
            Timber.tag(this@BaseViewModel::class.java.simpleName)
                .e(e, "Coroutine failed in launch{}")
            onError(e)
        } finally {
            if (showLoading) setLoading(false)
        }
    }
}
