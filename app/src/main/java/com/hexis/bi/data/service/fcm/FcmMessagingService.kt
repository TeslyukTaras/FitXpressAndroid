package com.hexis.bi.data.service.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hexis.bi.App
import com.hexis.bi.utils.SystemNotificationHelper
import com.hexis.bi.utils.constants.FcmDataKeys
import com.hexis.bi.utils.constants.FcmTrayNotificationId
import com.hexis.bi.utils.constants.SystemTrayChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.absoluteValue

/**
 * Handles incoming FCM messages. Register in AndroidManifest with
 * `com.google.firebase.MESSAGING_EVENT`.
 *
 * - **Notification payload** (title/body in console): [onMessageReceived] runs when the app is in
 *   the foreground; in background the system shows the notification automatically.
 * - **Data-only** payload: always delivered here; if `title`/`body` (or [FcmDataKeys.NOTIFICATION_TITLE] /
 *   [FcmDataKeys.NOTIFICATION_BODY]) are present, a tray notification is shown on
 *   [SystemTrayChannel.FCM_ID].
 */
class FcmMessagingService : FirebaseMessagingService() {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Timber.i(
            "FCM new token — register with your backend if you need server→device push: %s",
            token
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d(
            "FCM message id=%s from=%s collapseKey=%s data=%s",
            message.messageId,
            message.from,
            message.collapseKey,
            message.data,
        )

        val notification = message.notification
        val title: String?
        val body: String?
        if (notification != null) {
            title = notification.title
            body = notification.body
        } else {
            val data = message.data
            title = data[FcmDataKeys.TITLE] ?: data[FcmDataKeys.NOTIFICATION_TITLE]
            body = data[FcmDataKeys.BODY] ?: data[FcmDataKeys.MESSAGE]
                    ?: data[FcmDataKeys.NOTIFICATION_BODY]
        }

        if (title.isNullOrBlank() && body.isNullOrBlank()) {
            Timber.d("FCM: no display fields; nothing to show (handle data in app if needed)")
            return
        }

        val appLabel = applicationInfo.loadLabel(packageManager).toString()
        val displayTitle = title?.takeIf { it.isNotBlank() } ?: appLabel
        val displayBody = body.orEmpty()

        val notificationId = run {
            val base = message.messageId?.hashCode() ?: message.data.hashCode()
            val x = if (base != 0) {
                base.absoluteValue
            } else {
                (message.sentTime % FcmTrayNotificationId.SENT_TIME_FALLBACK_MODULUS).toInt()
                    .absoluteValue
            }
            (x % FcmTrayNotificationId.MODULUS) + FcmTrayNotificationId.NOTIFICATION_ID_OFFSET
        }

        SystemNotificationHelper.showFcmMessage(
            this,
            notificationId = notificationId,
            title = displayTitle,
            text = displayBody,
        )

        val app = applicationContext as? App
        if (app != null) {
            val inbox = app.notificationInboxRepository()
            ioScope.launch { inbox.appendRawInbox(displayTitle, displayBody) }
        }
    }
}
