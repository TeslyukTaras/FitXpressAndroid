package com.hexis.bi.ui.main.subscription

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.BuildConfig
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.utils.constants.SubscriptionConstants
import java.time.Instant
import java.time.ZoneId
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

internal fun SubscriptionStatus.Subscribed.renewalDisplay(): String =
    if (BuildConfig.ENVIRONMENT == SubscriptionConstants.DEV_ENVIRONMENT && renewsAtMillis > 0L) {
        Instant.ofEpochMilli(renewsAtMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(SubscriptionConstants.DEV_RENEWAL_TIME_PATTERN))
    } else {
        renewsOn.formatDisplay()
    }

