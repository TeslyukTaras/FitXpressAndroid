package com.hexis.bi.ui.main.subscription

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanFeature

@Composable
internal fun PlanFeature.label(scansPerMonth: Int, detailed: Boolean): String = when (this) {
    PlanFeature.FULL_ACCESS -> stringResource(R.string.plan_details_full_access)
    PlanFeature.SCANS -> stringResource(R.string.confirm_feature_scans, scansPerMonth)
    PlanFeature.FULL_SCAN -> stringResource(R.string.confirm_feature_full_scan)
    PlanFeature.MEASUREMENTS -> stringResource(R.string.confirm_feature_measurements)
    PlanFeature.INSIGHTS -> stringResource(
        if (detailed) R.string.plan_details_insights else R.string.confirm_feature_insights,
    )
}
