package com.hexis.bi.intelligence.engine

import java.time.LocalDate

internal object EngineDates {

    const val ORDINAL_EPOCH_OFFSET = 719_163L

    fun ordinal(date: String): Double = (LocalDate.parse(date).toEpochDay() + ORDINAL_EPOCH_OFFSET).toDouble()

    fun minusDays(date: String, days: Long): String = LocalDate.parse(date).minusDays(days).toString()

    fun daysBetween(from: String, to: String): Int =
        (LocalDate.parse(to).toEpochDay() - LocalDate.parse(from).toEpochDay()).toInt()
}
