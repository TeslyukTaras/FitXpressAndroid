package com.hexis.bi.ui.base

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
}

val <T> UiState<T>.isLoading: Boolean get() = this is UiState.Loading
val <T> UiState<T>.isSuccess: Boolean get() = this is UiState.Success
val <T> UiState<T>.isError: Boolean get() = this is UiState.Error
val <T> UiState<T>.dataOrNull: T? get() = (this as? UiState.Success)?.data
val <T> UiState<T>.errorMessage: String? get() = (this as? UiState.Error)?.message
