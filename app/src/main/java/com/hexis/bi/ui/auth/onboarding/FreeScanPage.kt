package com.hexis.bi.ui.auth.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.main.subscription.components.InfoNote
import com.hexis.bi.ui.main.subscription.components.PlanFeatureRow

@Composable
internal fun FreeScanPage(onBack: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingHeader(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(R.string.scan_first_free_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                Text(
                    text = stringResource(R.string.onboarding_free_scan_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.padding_auth_vertical)))
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_l))) {
                    PlanFeatureRow(stringResource(R.string.onboarding_free_scan_item_phone))
                    PlanFeatureRow(stringResource(R.string.onboarding_free_scan_item_clothing))
                    PlanFeatureRow(stringResource(R.string.onboarding_free_scan_item_shoes))
                    PlanFeatureRow(stringResource(R.string.onboarding_free_scan_item_frame))
                }
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
                AppHorizontalGradientDivider()
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
                InfoNote(text = stringResource(R.string.onboarding_free_scan_note))
            }
        }
    }
}
