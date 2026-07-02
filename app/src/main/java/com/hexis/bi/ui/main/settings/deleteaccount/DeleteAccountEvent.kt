package com.hexis.bi.ui.main.settings.deleteaccount

import com.hexis.bi.ui.base.UiEvent

sealed interface DeleteAccountEvent : UiEvent {
    data object DeleteSuccess : DeleteAccountEvent
}
