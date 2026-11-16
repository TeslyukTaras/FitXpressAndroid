package com.hexis.bi.ui.main.subscription

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@StringRes
internal fun PlanId.nameRes(): Int = when (this) {
    PlanId.ESSENTIAL -> R.string.plan_name_essential
    PlanId.PROGRESS -> R.string.plan_name_progress
    PlanId.TRANSFORMATION -> R.string.plan_name_transformation
}

internal fun LocalDate.formatDisplay(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
