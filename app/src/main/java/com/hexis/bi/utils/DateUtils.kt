package com.hexis.bi.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.utils.constants.ProfileConstants
import com.hexis.bi.utils.constants.SleepConstants
import com.hexis.bi.utils.constants.TimeConstants
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
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
fun formatSleepDuration(minutes: Int): String =
    if (minutes >= SleepConstants.MINUTES_PER_HOUR)
        stringResource(
            R.string.sleep_duration_hours_minutes,
            minutes / SleepConstants.MINUTES_PER_HOUR,
            minutes % SleepConstants.MINUTES_PER_HOUR,
        )
    else
        stringResource(R.string.sleep_duration_minutes, minutes)

@Composable
fun formatHour(hour: Int): String =
    if (hour < TimeConstants.HOURS_IN_HALF_DAY)
        stringResource(
            R.string.sleep_time_am,
            if (hour == 0) TimeConstants.HOURS_IN_HALF_DAY else hour
        )
    else
        stringResource(
            R.string.sleep_time_pm,
            if (hour == TimeConstants.HOURS_IN_HALF_DAY) TimeConstants.HOURS_IN_HALF_DAY else hour - TimeConstants.HOURS_IN_HALF_DAY
        )
