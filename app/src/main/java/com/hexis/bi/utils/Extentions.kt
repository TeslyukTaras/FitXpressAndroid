package com.hexis.bi.utils

import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Date

fun Date.calculateAge(): Int {
    val birthDate = this.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    val currentDate = LocalDate.now(ZoneId.systemDefault())

    return Period.between(birthDate, currentDate).years
}