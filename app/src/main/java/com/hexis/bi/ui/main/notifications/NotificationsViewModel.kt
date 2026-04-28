package com.hexis.bi.ui.main.notifications

import android.app.Application
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.utils.constants.NotificationUi
import androidx.lifecycle.viewModelScope
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(
    application: Application,
    private val inbox: NotificationInboxRepository,
) : BaseViewModel(application) {

    val items = inbox.items.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(NotificationUi.INBOX_STATE_FLOW_STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
    )

    fun markAllRead() = launch(showLoading = false) {
        inbox.markAllRead()
    }

    fun markRead(id: String) = launch(showLoading = false) {
        inbox.markRead(id)
    }
}
