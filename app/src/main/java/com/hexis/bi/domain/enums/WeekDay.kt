package com.hexis.bi.domain.enums

import java.time.DayOfWeek

enum class WeekDay(val abbreviation: String) {
    Monday("Mo"),
    Tuesday("Tu"),
    Wednesday("We"),
    Thursday("Th"),
    Friday("Fr"),
    Saturday("Sa"),
    Sunday("Su");

    fun toDayOfWeek(): DayOfWeek = DayOfWeek.of(ordinal + 1)

    companion object {
        fun fromDayOfWeek(dayOfWeek: DayOfWeek): WeekDay = entries[dayOfWeek.ordinal]
    }
}
