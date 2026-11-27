package com.hexis.bi.ui.main.scan.access

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun FirstFreeScanDialog(
    onContinue: () -> Unit,
    onMaybeLater: () -> Unit,
    onViewGuide: () -> Unit,
) {
    AppDialog(onDismiss = onMaybeLater) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = dimensionResource(R.dimen.padding_large),
                    end = dimensionResource(R.dimen.padding_large),
                    start = dimensionResource(R.dimen.padding_large),
                    bottom = dimensionResource(R.dimen.spacer_s),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.scan_first_free_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = stringResource(R.string.scan_first_free_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = stringResource(R.string.scan_first_free_note),
                style = TitleDimTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
            GateButtons(
                secondary = stringResource(R.string.action_maybe_later),
                onSecondary = onMaybeLater,
                primary = stringResource(R.string.action_continue),
                onPrimary = onContinue,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xs)))
            TextButton(
                onClick = onViewGuide,
                modifier = Modifier.height(dimensionResource(R.dimen.scan_gate_button_height))
            ) {
                Text(
                    text = stringResource(R.string.scan_view_guide),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun SubscriptionRequiredDialog(
    onChoosePlan: () -> Unit,
    onMaybeLater: () -> Unit,
) {
    AppDialog(onDismiss = onMaybeLater) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.padding_large)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.scan_subscription_required_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = stringResource(R.string.scan_subscription_required_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
            GateButtons(
                secondary = stringResource(R.string.action_maybe_later),
                onSecondary = onMaybeLater,
                primary = stringResource(R.string.subscription_choose_plan),
                onPrimary = onChoosePlan,
            )
        }
    }
}

@Composable
internal fun PaymentIssueScanDialog(
    onResolve: () -> Unit,
    onMaybeLater: () -> Unit,
) {
    AppDialog(onDismiss = onMaybeLater) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.padding_large)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.scan_payment_issue_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = stringResource(R.string.scan_payment_issue_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
            GateButtons(
                secondary = stringResource(R.string.action_maybe_later),
                onSecondary = onMaybeLater,
                primary = stringResource(R.string.scan_payment_issue_resolve),
                onPrimary = onResolve,
            )
        }
    }
}

@Composable
private fun GateButtons(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    onPrimary: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
    ) {
        AppOutlinedButton(
            text = secondary,
            onClick = onSecondary,
            modifier = Modifier.weight(1f),
            height = dimensionResource(R.dimen.height_button_compact),
        )
        AppPrimaryButton(
            text = primary,
            onClick = onPrimary,
            modifier = Modifier.weight(1f),
            height = dimensionResource(R.dimen.height_button_compact),
        )
    }
}
