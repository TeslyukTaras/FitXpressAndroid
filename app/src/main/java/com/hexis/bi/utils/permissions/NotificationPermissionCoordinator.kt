package com.hexis.bi.utils.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.reminder.ScanReminderScheduler
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.UserRepository
import timber.log.Timber

/**
 * Coordinates the post-sign-in notification flow:
 *  • prompts for POST_NOTIFICATIONS once per uid (API 33+),
 *  • reconciles Firestore pushNotificationsEnabled against the actual OS permission so a user
 *    who revoked the OS permission since the last session no longer has reminders flagged "on".
 */
class NotificationPermissionCoordinator(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val preferences: UserPreferencesRepository,
    private val scanReminderScheduler: ScanReminderScheduler,
) {

    /** True if we should show the OS permission prompt for this uid right now. */
    suspend fun shouldPromptOnSignIn(uid: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (hasOsPermission()) return false
        return !preferences.hasAskedNotifPermission(uid)
    }

    /**
     * If OS notifications are off, force pushNotificationsEnabled to false in Firestore
     * (and re-run the scheduler so any pending alarms get canceled).
     */
    suspend fun reconcilePushSetting() {
        if (auth.currentUser == null) return
        if (hasOsPermission()) return
        val settings = userRepository.getUserSettings().getOrNull() ?: return
        if (settings.pushNotificationsEnabled != true) return
        val result = runCatching {
            userRepository.updateUserSettings(
                mapOf(FirestoreSchema.UserSettingsFields.PUSH_NOTIFICATIONS_ENABLED to false),
            )
        }
        if (result.isFailure) {
            Timber.Forest.w(
                result.exceptionOrNull(),
                "reconcilePushSetting: failed to disable push in Firestore"
            )
            return
        }
        scanReminderScheduler.onNotificationSettingsOrScanChanged()
    }

    suspend fun onPermissionResult(uid: String, granted: Boolean) {
        preferences.markAskedNotifPermission(uid)
        if (!granted) reconcilePushSetting()
    }

    private fun hasOsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}