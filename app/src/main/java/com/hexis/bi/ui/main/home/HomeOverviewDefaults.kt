package com.hexis.bi.ui.main.home

import android.content.Context
import com.hexis.bi.R
import com.hexis.bi.utils.constants.SleepConstants

internal object HomeOverviewDefaults {

    fun placeholderSleepCard(context: Context, goalHours: Int = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS): OverviewCardData =
        sleepCard(
            context = context,
            goalSubtitle = context.getString(R.string.home_sleep_goal, goalHours.toString()),
            value = context.getString(R.string.sleep_placeholder),
            valueLabel = null,
        )

    fun sleepCard(
        context: Context,
        goalSubtitle: String,
        value: String,
        valueLabel: String?,
        variant: OverviewCardVariant = OverviewCardVariant.Accent,
    ): OverviewCardData = OverviewCardData(
        title = context.getString(R.string.home_card_sleep),
        iconRes = R.drawable.ic_moon,
        value = value,
        valueLabel = valueLabel,
        subtitle = goalSubtitle,
        variant = variant,
    )
}
