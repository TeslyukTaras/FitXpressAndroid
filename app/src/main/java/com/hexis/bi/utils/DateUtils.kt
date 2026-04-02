package com.hexis.bi.utils

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
