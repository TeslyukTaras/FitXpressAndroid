package com.hexis.bi.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.domain.enums.WeekDay
import com.hexis.bi.utils.constants.DateFormatConstants
import com.hexis.bi.utils.constants.ProfileConstants
import com.hexis.bi.utils.constants.SleepConstants
import com.hexis.bi.utils.constants.TimeConstants
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

fun Date.calculateAge(): Int {
    val birthDate = toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    return Period.between(birthDate, LocalDate.now(ZoneId.systemDefault())).years
}

fun Date.formatDob(): String =
    SimpleDateFormat(ProfileConstants.DOB_DATE_FORMAT, Locale.US).format(this)

fun Long.millisToDobString(): String =
    SimpleDateFormat(ProfileConstants.DOB_DATE_FORMAT, Locale.US).format(Date(this))

fun String.parseDob(): Date? =
    SimpleDateFormat(ProfileConstants.DOB_DATE_FORMAT, Locale.US)
        .runCatching { parse(this@parseDob) }
        .getOrNull()

/** Short "Apr 14" label used on the Results screen header. Locale-aware. */
fun Date.formatShortMonthDay(): String =
    SimpleDateFormat(DateFormatConstants.SHORT_MONTH_DAY, Locale.getDefault()).format(this)

fun Long.millisToShortMonthDay(): String = Date(this).formatShortMonthDay()

fun shortMonthDayFormatter(locale: Locale = Locale.getDefault()): SimpleDateFormat =
    SimpleDateFormat(DateFormatConstants.SHORT_MONTH_DAY, locale)

fun shortMonthDayYearFormatter(locale: Locale = Locale.getDefault()): SimpleDateFormat =
    SimpleDateFormat(DateFormatConstants.SHORT_MONTH_DAY_YEAR, locale)

/** "Apr 14, 2026" label from an epoch-millis timestamp. Locale-aware. */
fun Long.millisToShortMonthDayYear(): String =
    shortMonthDayYearFormatter().format(Date(this))

/** Lowercase hour with am/pm from an epoch-millis timestamp, e.g. "9 am". Locale-aware. */
fun Long.millisToHourAmPm(): String =
    SimpleDateFormat(DateFormatConstants.HOUR_AM_PM, Locale.getDefault())
        .format(Date(this))
        .lowercase(Locale.getDefault())

/** "12.06.2026 09:41 am" timestamp for the order details timeline. Locale-aware. */
fun Long.millisToOrderTimelineTimestamp(): String =
    SimpleDateFormat(DateFormatConstants.ORDER_TIMELINE_TIMESTAMP, Locale.getDefault())
        .format(Date(this))
        .lowercase(Locale.getDefault())

/** Full "April 14" label used on day-based detail screens. Locale-aware. */
fun LocalDate.formatFullMonthDay(): String =
    format(DateTimeFormatter.ofPattern(DateFormatConstants.FULL_MONTH_DAY, Locale.getDefault()))

/** Short "Apr 14" label used for compact date ranges. Locale-aware. */
fun LocalDate.formatShortMonthDay(): String =
    format(DateTimeFormatter.ofPattern(DateFormatConstants.SHORT_MONTH_DAY, Locale.getDefault()))

/** Day-of-month number, e.g. "14". Locale-aware. */
fun LocalDate.formatDayOfMonth(): String =
    format(DateTimeFormatter.ofPattern(DateFormatConstants.DAY_OF_MONTH, Locale.getDefault()))

/** Short month label, e.g. "Jan". Locale-aware. */
fun LocalDate.formatMonthShort(): String =
    format(DateTimeFormatter.ofPattern(DateFormatConstants.MONTH_SHORT, Locale.getDefault()))

/** "April 2026" label used on month-based detail screens. Locale-aware. */
fun LocalDate.formatFullMonthYear(): String =
    format(DateTimeFormatter.ofPattern(DateFormatConstants.FULL_MONTH_YEAR, Locale.getDefault()))

/** Four-digit year label, e.g. "2026". */
fun LocalDate.formatYear(): String =
    format(DateTimeFormatter.ofPattern(DateFormatConstants.YEAR, Locale.getDefault()))

/** "Apr 14, 2026" label used for last-synced timestamps. Locale-aware. */
fun LocalDate.formatShortMonthDayYear(): String =
    format(DateTimeFormatter.ofPattern(DateFormatConstants.SHORT_MONTH_DAY_YEAR, Locale.getDefault()))

/** Compact "Apr 14 - Apr 20" range using short month-day labels. */
fun formatShortDateRange(start: LocalDate, end: LocalDate): String =
    "${start.formatShortMonthDay()} - ${end.formatShortMonthDay()}"

/** Two-letter weekday initials for [this] date, e.g. "Mo", "Tu". */
fun LocalDate.weekDayAbbreviation(): String =
    WeekDay.entries[dayOfWeek.value - 1].abbreviation

/** Sortable timestamp used as the Firestore document ID for a saved scan. */
fun Date.formatAsScanDocId(): String =
    SimpleDateFormat(DateFormatConstants.SCAN_DOC_ID_TIMESTAMP, Locale.US).format(this)

fun Int.formatHour(): String {
    val h = if (this % TimeConstants.HOURS_IN_HALF_DAY == 0) TimeConstants.HOURS_IN_HALF_DAY
    else this % TimeConstants.HOURS_IN_HALF_DAY
    val amPm = if (this < TimeConstants.HOURS_IN_HALF_DAY) TimeConstants.AM else TimeConstants.PM
    return "$h $amPm"
}

fun Int.hour24ToHour12(): Int = when {
    this == 0 -> TimeConstants.HOURS_IN_HALF_DAY
    this > TimeConstants.HOURS_IN_HALF_DAY -> this - TimeConstants.HOURS_IN_HALF_DAY
    else -> this
}

fun Int.isHour24Pm(): Boolean = this >= TimeConstants.HOURS_IN_HALF_DAY

fun hour12ToHour24(hour12: Int, isPm: Boolean): Int = when {
    isPm && hour12 < TimeConstants.HOURS_IN_HALF_DAY -> hour12 + TimeConstants.HOURS_IN_HALF_DAY
    !isPm && hour12 == TimeConstants.HOURS_IN_HALF_DAY -> 0
    else -> hour12
}

@Composable
fun formatSleepDuration(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    return if (safe >= SleepConstants.MINUTES_PER_HOUR) stringResource(
        R.string.sleep_duration_hours_minutes,
        safe / SleepConstants.MINUTES_PER_HOUR,
        safe % SleepConstants.MINUTES_PER_HOUR,
    )
    else stringResource(R.string.sleep_duration_minutes, safe)
}

@Composable
fun formatHour(hour: Int): String =
    if (hour < TimeConstants.HOURS_IN_HALF_DAY) stringResource(
        R.string.sleep_time_am,
        if (hour == 0) TimeConstants.HOURS_IN_HALF_DAY else hour
    )
    else stringResource(
        R.string.sleep_time_pm,
        if (hour == TimeConstants.HOURS_IN_HALF_DAY) TimeConstants.HOURS_IN_HALF_DAY else hour - TimeConstants.HOURS_IN_HALF_DAY
    )
