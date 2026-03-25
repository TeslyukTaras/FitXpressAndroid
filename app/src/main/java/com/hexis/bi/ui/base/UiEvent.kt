package com.hexis.bi.ui.base

/**
 * One-shot UI events emitted from a ViewModel via [BaseViewModel.events].
 *
 * Extend this interface to add screen-specific events:
 *
 * ```
 * sealed interface MyEvent : UiEvent {
 *     data object NavigateToHome : MyEvent
 *     data class ShowToast(val text: String) : MyEvent
 * }
 * ```
 */
interface UiEvent {
    /** Navigate back to the previous destination. */
    data object NavigateBack : UiEvent

    /** Show a short-lived snackbar message. */
    data class ShowSnackbar(val message: String) : UiEvent
}
