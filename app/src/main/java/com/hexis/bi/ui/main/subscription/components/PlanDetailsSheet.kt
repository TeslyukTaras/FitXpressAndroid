package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.ui.base.BaseBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanDetailsSheet(
    plan: PlanId,
    scansPerMonth: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = stringResource(R.string.plan_details_title),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            PlanFeatureList(
                plan = plan,
                scansPerMonth = scansPerMonth,
                detailed = true,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            InfoNote(
                text = stringResource(R.string.confirm_scans_note),
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            ) {
                Text(
                    text = stringResource(R.string.action_got_it),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
