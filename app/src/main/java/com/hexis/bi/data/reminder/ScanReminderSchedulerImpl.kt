package com.hexis.bi.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.notification.ScanReminderBroadcastReceiver
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.data.subscription.SubscriptionRepository
import com.hexis.bi.domain.scan.ReminderKind
import com.hexis.bi.domain.subscription.SubscriptionStatus
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.hexis.bi.ui.MainActivity
import com.hexis.bi.utils.constants.NotificationPendingIntent
import com.hexis.bi.utils.constants.ScanReminderAlarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneId
import java.time.ZonedDateTime

class ScanReminderSchedulerImpl(
    private val context: Context,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val resolver: ScanReminderResolver,
    private val subscriptionRepository: SubscriptionRepository,
) : ScanReminderScheduler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rescheduleLock = Mutex()

    init {
        subscriptionRepository.status
            .filter { it !is SubscriptionStatus.Loading }
            .map { it.reminderFingerprint() }
            .distinctUntilChanged()
            .drop(1)
            .onEach { onNotificationSettingsOrScanChanged() }
            .launchIn(scope)
    }

    override fun onNotificationSettingsOrScanChanged() {
        scope.launch {
            rescheduleLock.withLock { reschedule() }
        }
    }

    private suspend fun reschedule() {
        val am = context.getSystemService<AlarmManager>() ?: return
        if (auth.currentUser == null) {
            cancelAllScanReminders(am)
            return
        }
        val settings = userRepository.getUserSettings().getOrElse {
            Timber.w(it, "ScanReminderScheduler: getUserSettings failed; leaving alarms as-is")
            return
        }
        if (settings.pushNotificationsEnabled != true || settings.scanRemindersEnabled != true) {
            cancelAllScanReminders(am)
            return
        }
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val forecast = resolver.forecast(now, settings) ?: run {
            Timber.w("ScanReminderScheduler: forecast unavailable; leaving alarms as-is")
            return
        }
        for (kind in ReminderKind.entries) {
            setNextAlarm(
                am,
                kind.alarmKind(),
                forecast.events.firstOrNull { it.kind == kind }?.at,
            )
        }
    }

    private fun cancelAllScanReminders(am: AlarmManager) {
        for (k in listOf(
            ScanReminderAlarms.KIND_TODAY,
            ScanReminderAlarms.KIND_NUDGE,
            ScanReminderAlarms.KIND_MISSED,
        )) {
            am.cancel(broadcastForKind(k))
        }
    }

    private fun SubscriptionStatus.reminderFingerprint(): List<Any?> = when (this) {
        is SubscriptionStatus.Subscribed -> listOf(plan, scansUsed, scansAllowed, renewsOn)
        is SubscriptionStatus.None -> listOf(freeScanUsed)
        else -> listOf(this::class)
    }

    private fun setNextAlarm(
        am: AlarmManager,
        kind: String,
        target: ZonedDateTime?,
    ) {
        val op = broadcastForKind(kind)
        if (target == null) {
            am.cancel(op)
            return
        }
        val whenMillis = target.toInstant().toEpochMilli()
        val show = showPendingIntent()
        val info = AlarmManager.AlarmClockInfo(whenMillis, show)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) am.setAlarmClock(info, op)
            else {
                Timber.w(
                    "ScanReminderScheduler: exact alarms not available; " +
                            "reminders may be delayed. Grant \"Alarms & reminders\" in app settings if needed.",
                )
                scheduleInexactWakeupAlarm(am, whenMillis, op)
            }
        } else am.setAlarmClock(info, op)
    }

    /**
     * Inexact wake-up when SCHEDULE_EXACT_ALARM is not granted (user revoked or device policy).
     * Delivery time is not guaranteed; prefer "Alarms & reminders" for scan reminders in settings.
     * Uses [AlarmManager.setAndAllowWhileIdle] so the reminder can still fire during Doze
     * (unlike [AlarmManager.setWindow], which Doze defers for the whole maintenance window).
     */
    private fun scheduleInexactWakeupAlarm(
        am: AlarmManager,
        whenMillis: Long,
        op: PendingIntent,
    ) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, op)
    }

    private fun showPendingIntent(): PendingIntent {
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            ScanReminderAlarms.RequestCode.LAUNCH_APP_STATUS_BAR,
            launch,
            NotificationPendingIntent.FOR_LAUNCH_ACTIVITY,
        )
    }

    private fun broadcastForKind(kind: String): PendingIntent {
        val intent = Intent(context, ScanReminderBroadcastReceiver::class.java).apply {
            setPackage(context.packageName)
            putExtra(ScanReminderAlarms.BROADCAST_INTENT_EXTRA_KIND, kind)
        }
        val requestCode = requestCodeForKind(kind) ?: error("unknown kind=$kind")
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            NotificationPendingIntent.FOR_LAUNCH_ACTIVITY,
        )
    }

    private fun requestCodeForKind(kind: String): Int? = when (kind) {
        ScanReminderAlarms.KIND_TODAY -> ScanReminderAlarms.RequestCode.BROADCAST_TODAY
        ScanReminderAlarms.KIND_NUDGE -> ScanReminderAlarms.RequestCode.BROADCAST_NUDGE
        ScanReminderAlarms.KIND_MISSED -> ScanReminderAlarms.RequestCode.BROADCAST_MISSED
        else -> null
    }

    private fun ReminderKind.alarmKind(): String = when (this) {
        ReminderKind.TODAY -> ScanReminderAlarms.KIND_TODAY
        ReminderKind.NUDGE -> ScanReminderAlarms.KIND_NUDGE
        ReminderKind.MISSED -> ScanReminderAlarms.KIND_MISSED
    }
}
