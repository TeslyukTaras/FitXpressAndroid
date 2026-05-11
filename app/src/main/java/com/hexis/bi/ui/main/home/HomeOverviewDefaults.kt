package com.hexis.bi.ui.main.home

import android.content.Context
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.recovery.RecoveryStatus
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.SleepConstants
import java.util.Locale

internal object HomeOverviewDefaults {

    fun placeholderSleepCard(context: Context, goalHours: Int = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS): OverviewCardData =
        sleepCard(
            context = context,
            goalSubtitle = context.getString(R.string.home_sleep_goal, goalHours.toString()),
            value = formatSleepHours(0f),
            valueLabel = context.getString(R.string.unit_hours_short),
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

    fun placeholderActivityCard(context: Context, stepsGoal: Int = ActivityConstants.DEFAULT_STEP_GOAL): OverviewCardData =
        activityCard(
            context = context,
            goalSubtitle = context.getString(R.string.home_activity_goal, formatSteps(stepsGoal)),
            value = formatSteps(0),
            valueLabel = context.getString(R.string.home_activity_value_label),
        )

    fun activityCard(
        context: Context,
        goalSubtitle: String,
        value: String,
        valueLabel: String?,
        variant: OverviewCardVariant = OverviewCardVariant.Default,
    ): OverviewCardData = OverviewCardData(
        title = context.getString(R.string.home_card_activity_score),
        iconRes = R.drawable.ic_steps,
        value = value,
        valueLabel = valueLabel,
        subtitle = goalSubtitle,
        variant = variant,
    )

    fun placeholderRecoveryCard(context: Context): OverviewCardData =
        recoveryCard(
            context = context,
            value = context.getString(R.string.home_recovery_score_value, 0),
            statusSubtitle = context.getString(RecoveryStatus.fromScore(0).labelRes),
        )

    fun recoveryCard(
        context: Context,
        value: String,
        statusSubtitle: String,
        variant: OverviewCardVariant = OverviewCardVariant.Default,
    ): OverviewCardData = OverviewCardData(
        title = context.getString(R.string.home_card_recovery),
        iconRes = R.drawable.ic_refresh,
        value = value,
        valueLabel = null,
        subtitle = statusSubtitle,
        variant = variant,
    )

    fun placeholderScanCard(context: Context): OverviewCardData =
        scanCard(
            context = context,
            value = context.getString(R.string.stat_unknown),
            valueLabel = null,
            subtitle = context.getString(R.string.home_scan_no_data),
        )

    fun scanCard(
        context: Context,
        value: String,
        valueLabel: String?,
        subtitle: String,
    ): OverviewCardData = OverviewCardData(
        title = context.getString(R.string.home_card_scan),
        iconRes = R.drawable.ic_body,
        value = value,
        valueLabel = valueLabel,
        subtitle = subtitle,
        variant = OverviewCardVariant.Primary,
    )

    internal fun formatSteps(steps: Int): String = "%,d".format(steps.coerceAtLeast(0))

    internal fun formatSleepHours(hours: Float): String =
        "%.1f".format(Locale.US, hours.coerceAtLeast(0f))
}
