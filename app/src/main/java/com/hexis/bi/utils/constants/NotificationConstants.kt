package com.hexis.bi.utils.constants

import android.app.PendingIntent

/**
 * Notification, tray, FCM, and scan-reminder alarm values. Feature code should import from here, not
 * use magic numbers.
 */
object NotificationInboxList {
    const val MAX_PERSISTED_ITEMS: Int = 200
}

object NotificationUi {
    const val INBOX_STATE_FLOW_STOP_TIMEOUT_MILLIS: Long = 5_000L
    const val RELATIVE_TIME_EPOCH_UNSET: Long = 0L
}

object SystemTrayChannel {
    const val BODY_SCAN_ID: String = "body_scan_reminders"
    const val BODY_SCAN_NAME: String = "Body scan reminders"
    const val FCM_ID: String = "fcm_remote"
    const val FCM_NAME: String = "Push messages"
}

object SystemTrayNotificationId {
    const val BODY_SCAN_TODAY: Int = 9_001
    const val BODY_SCAN_FINISH_TODAY: Int = 9_002
    const val LAUNCH_APP_INTENT_REQUEST_CODE: Int = 0
}

object FcmDataKeys {
    const val TITLE: String = "title"
    const val NOTIFICATION_TITLE: String = "notification_title"
    const val BODY: String = "body"
    const val MESSAGE: String = "message"
    const val NOTIFICATION_BODY: String = "notification_body"
}

object FcmTrayNotificationId {
    const val MODULUS: Int = 1_000_000
    const val NOTIFICATION_ID_OFFSET: Int = 10_000
    const val SENT_TIME_FALLBACK_MODULUS: Long = 1_000_000L
}

object ScanReminderAlarms {
    const val KIND_TODAY: String = "today"
    const val KIND_NUDGE: String = "nudge"
    const val KIND_MISSED: String = "missed"
    const val BROADCAST_INTENT_EXTRA_KIND: String = "kind"

    object RequestCode {
        const val LAUNCH_APP_STATUS_BAR: Int = 0x3A0
        const val BROADCAST_TODAY: Int = 0x3A1
        const val BROADCAST_NUDGE: Int = 0x3A2
        const val BROADCAST_MISSED: Int = 0x3A3
    }
}

object NotificationPendingIntent {
    @JvmField
    val FOR_LAUNCH_ACTIVITY: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}
