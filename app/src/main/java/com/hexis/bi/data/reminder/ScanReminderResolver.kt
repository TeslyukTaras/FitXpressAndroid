package com.hexis.bi.data.reminder

import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.scan.ScanFetchProjection
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.subscription.SubscriptionRepository
import com.hexis.bi.data.user.UserSettings
import com.hexis.bi.domain.scan.ReminderBasis
import com.hexis.bi.domain.scan.ReminderForecast
import com.hexis.bi.domain.scan.ReminderInput
import com.hexis.bi.domain.scan.ReminderOverride
import com.hexis.bi.domain.scan.ScanReminderPlan
import com.hexis.bi.domain.scan.SentReminders
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.utils.constants.ScanReminderRules
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class ScanReminderResolver(
    private val subscriptionRepository: SubscriptionRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val preferences: UserPreferencesRepository,
) {

    suspend fun forecast(now: ZonedDateTime, settings: UserSettings): ReminderForecast? {
        val status = withTimeoutOrNull(ScanReminderRules.STATUS_WAIT_MS) {
            subscriptionRepository.status.first { it !is SubscriptionStatus.Loading }
        } ?: return null
        val lastScanAt = lastScanAt(now.zone).getOrElse { return null }
        return ScanReminderPlan.forecast(
            ReminderInput(
                now = now,
                basis = basisOf(status, now.zone),
                lastScanAt = lastScanAt,
                defaultHour = settings.reminderHour ?: ScanReminderRules.DEFAULT_HOUR,
                override = overrideOf(settings),
                sent = SentReminders.parse(
                    today = preferences.getScanReminderTodaySent(),
                    nudge = preferences.getScanReminderNudgeSent(),
                    missed = preferences.getScanReminderMissedSent(),
                ),
            ),
        )
    }

    private suspend fun lastScanAt(zone: ZoneId): Result<ZonedDateTime?> =
        scanHistoryRepository.getRecentScans(limit = 1, projection = ScanFetchProjection.TIMESTAMPS_ONLY)
            .map { scans ->
                scans.maxOfOrNull { it.timestamp }?.takeIf { it > 0L }
                    ?.let { Instant.ofEpochMilli(it).atZone(zone) }
            }

    private fun basisOf(status: SubscriptionStatus, zone: ZoneId): ReminderBasis? = when (status) {
        is SubscriptionStatus.Subscribed -> ReminderBasis.Plan(
            periodEnd = status.renewsOn,
            renewsAt = status.renewsAtMillis.takeIf { it > 0L }?.let { Instant.ofEpochMilli(it).atZone(zone) },
            scansUsed = status.scansUsed,
            scansAllowed = status.scansAllowed,
        )

        is SubscriptionStatus.None -> if (status.freeScanUsed) ReminderBasis.FreeScan else null
        else -> null
    }

    private fun overrideOf(settings: UserSettings): ReminderOverride? {
        val date = settings.nextReminderDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val hour = settings.nextReminderHour ?: return null
        return ReminderOverride(date, hour, settings.nextReminderScanMillis ?: 0L)
    }
}
