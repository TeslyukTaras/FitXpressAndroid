package com.hexis.bi.utils

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hexis.bi.R
import com.hexis.bi.ui.MainActivity
import com.hexis.bi.utils.constants.NotificationPendingIntent
import com.hexis.bi.utils.constants.SystemTrayChannel
import com.hexis.bi.utils.constants.SystemTrayNotificationId

object SystemNotificationHelper {
    fun createChannels(application: Application) {
        val mgr = application.getSystemService(NotificationManager::class.java) ?: return
        val bodyScan = NotificationChannel(
            SystemTrayChannel.BODY_SCAN_ID,
            SystemTrayChannel.BODY_SCAN_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val fcm = NotificationChannel(
            SystemTrayChannel.FCM_ID,
            SystemTrayChannel.FCM_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        mgr.createNotificationChannel(bodyScan)
        mgr.createNotificationChannel(fcm)
    }

    fun showBodyScanReminder(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
    ) {
        val n = NotificationCompat.Builder(context, SystemTrayChannel.BODY_SCAN_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(launchAppPendingIntent(context))
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(notificationId, n)
    }

    fun showFcmMessage(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
    ) {
        val n = NotificationCompat.Builder(context, SystemTrayChannel.FCM_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(launchAppPendingIntent(context))
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(notificationId, n)
    }

    private fun launchAppPendingIntent(context: Context): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            SystemTrayNotificationId.LAUNCH_APP_INTENT_REQUEST_CODE,
            launchIntent,
            NotificationPendingIntent.FOR_LAUNCH_ACTIVITY,
        )
    }
}
