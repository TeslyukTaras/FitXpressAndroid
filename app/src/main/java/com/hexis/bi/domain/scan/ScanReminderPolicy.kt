package com.hexis.bi.domain.scan

import com.hexis.bi.data.user.UserSettings
import com.hexis.bi.domain.enums.ReminderDay
import com.hexis.bi.domain.scan.ScanReminderPolicy.weekKeyForNudge
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.WeekFields

/**
 * Body-scan reminders:
 *  • "Scan today" push + inbox on `scheduledDay` at `reminderHour`.
 *  • "Finish your body scan today" push + inbox on `scheduledDay + 1` at `reminderHour`.
 *  • "You missed this week's scan" inbox-only on `scheduledDay + 6` at `reminderHour`.
 *
 * Each is de-duped per ISO week (see [weekKeyForNudge]).
 */
object ScanReminderPolicy {

    /** Days after the scheduled scan day for the "You missed this week's scan" inbox entry. */
    private const val MISSED_OFFSET_DAYS = 6L

    data class TodayContext(
        val todayZoned: ZonedDateTime,
        val todayDate: LocalDate,
        val startOfScanDay: ZonedDateTime,
    )

    data class NudgeContext(
        val nudgeZoned: ZonedDateTime,
        val nudgeDate: LocalDate,
        val startOfScanDay: ZonedDateTime,
    )

    data class MissedContext(
        val missedZoned: ZonedDateTime,
        val missedDate: LocalDate,
        val startOfScanDay: ZonedDateTime,
    )

    fun todayContextFor(
        now: ZonedDateTime,
        settings: UserSettings,
        zoneId: ZoneId = now.zone,
    ): TodayContext? {
        val (time, scheduledDow) = resolveReminder(settings) ?: return null
        val d = now.toLocalDate()
        if (d.dayOfWeek != scheduledDow) return null
        return TodayContext(
            todayZoned = d.atTime(time).atZone(zoneId),
            todayDate = d,
            startOfScanDay = d.atStartOfDay(zoneId),
        )
    }

    fun nudgeContextFor(
        now: ZonedDateTime,
        settings: UserSettings,
        zoneId: ZoneId = now.zone,
    ): NudgeContext? {
        val (time, scheduledDow) = resolveReminder(settings) ?: return null
        val nudgeDow = scheduledDow.plusDays(1)
        val d = now.toLocalDate()
        if (d.dayOfWeek != nudgeDow) return null
        val schedDate = d.minusDays(1)
        if (schedDate.dayOfWeek != scheduledDow) return null
        return NudgeContext(
            nudgeZoned = d.atTime(time).atZone(zoneId),
            nudgeDate = d,
            startOfScanDay = schedDate.atStartOfDay(zoneId),
        )
    }

    fun missedContextFor(
        now: ZonedDateTime,
        settings: UserSettings,
        zoneId: ZoneId = now.zone,
    ): MissedContext? {
        val (time, scheduledDow) = resolveReminder(settings) ?: return null
        val missedDow = scheduledDow.plusDays(MISSED_OFFSET_DAYS)
        val d = now.toLocalDate()
        if (d.dayOfWeek != missedDow) return null
        val schedDate = d.minusDays(MISSED_OFFSET_DAYS)
        return MissedContext(
            missedZoned = d.atTime(time).atZone(zoneId),
            missedDate = d,
            startOfScanDay = schedDate.atStartOfDay(zoneId),
        )
    }

    fun weekKeyForNudge(nudgeZoned: ZonedDateTime): String {
        val w = nudgeZoned.get(WeekFields.ISO.weekOfWeekBasedYear())
        val y = nudgeZoned.get(WeekFields.ISO.weekBasedYear())
        return y.toString() + "-W" + w.toString().padStart(2, '0')
    }

    /** Next absolute instant the day-of "Scan today" push should fire. */
    fun nextTodayAt(
        now: ZonedDateTime,
        settings: UserSettings,
        zoneId: ZoneId = now.zone,
    ): ZonedDateTime? {
        val (time, scheduledDow) = resolveReminder(settings) ?: return null
        return nextOccurrenceOf(now, scheduledDow, time, zoneId)
    }

    /** Next absolute instant the day-after "Finish your body scan today" push should fire. */
    fun nextNudgeAt(
        now: ZonedDateTime,
        settings: UserSettings,
        zoneId: ZoneId = now.zone,
    ): ZonedDateTime? {
        val (time, scheduledDow) = resolveReminder(settings) ?: return null
        return nextOccurrenceOf(now, scheduledDow.plusDays(1), time, zoneId)
    }

    /** Next absolute instant the "You missed this week's scan" inbox entry should be posted. */
    fun nextMissedAt(
        now: ZonedDateTime,
        settings: UserSettings,
        zoneId: ZoneId = now.zone,
    ): ZonedDateTime? {
        val (time, scheduledDow) = resolveReminder(settings) ?: return null
        return nextOccurrenceOf(now, scheduledDow.plusDays(MISSED_OFFSET_DAYS), time, zoneId)
    }

    private fun resolveReminder(settings: UserSettings): Pair<LocalTime, DayOfWeek>? {
        if (settings.pushNotificationsEnabled != true) return null
        if (settings.scanRemindersEnabled != true) return null
        val hour = settings.reminderHour ?: return null
        val dayName = settings.reminderDay ?: return null
        val reminderDay = runCatching { ReminderDay.valueOf(dayName) }.getOrNull() ?: return null
        return LocalTime.of(hour, 0) to reminderDay.toJavaDayOfWeek()
    }

    private fun nextOccurrenceOf(
        now: ZonedDateTime,
        targetDow: DayOfWeek,
        time: LocalTime,
        zoneId: ZoneId,
    ): ZonedDateTime {
        val todayAtTime = now.toLocalDate().atTime(time).atZone(zoneId)
        val sameDay = now.toLocalDate().dayOfWeek == targetDow && !now.isAfter(todayAtTime)
        if (sameDay) return todayAtTime
        // Advance day-by-day to next matching day of week (strict: skip today if past the time).
        var cursor = now.toLocalDate().plusDays(1)
        while (cursor.dayOfWeek != targetDow) cursor = cursor.plusDays(1)
        return cursor.atTime(time).atZone(zoneId)
    }

    private fun DayOfWeek.plusDays(n: Long): DayOfWeek {
        val v = ((value - 1 + n) % 7 + 7) % 7 + 1
        return DayOfWeek.of(v.toInt())
    }

    private fun ReminderDay.toJavaDayOfWeek(): DayOfWeek = when (this) {
        ReminderDay.Monday -> DayOfWeek.MONDAY
        ReminderDay.Tuesday -> DayOfWeek.TUESDAY
        ReminderDay.Wednesday -> DayOfWeek.WEDNESDAY
        ReminderDay.Thursday -> DayOfWeek.THURSDAY
        ReminderDay.Friday -> DayOfWeek.FRIDAY
        ReminderDay.Saturday -> DayOfWeek.SATURDAY
        ReminderDay.Sunday -> DayOfWeek.SUNDAY
    }
}
