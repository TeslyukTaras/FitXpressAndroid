package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.RestoreResult
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.main.subscription.nameRes

data class RestoreOutcome(val result: RestoreResult, val plan: PlanId?)

@Composable
internal fun RestoreResultDialog(
    outcome: RestoreOutcome,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val planName = outcome.plan?.let { stringResource(it.nameRes()) }.orEmpty()
    val title = stringResource(
        when (outcome.result) {
            RestoreResult.RESTORED -> R.string.restore_success_title
            RestoreResult.NOT_FOUND -> R.string.restore_none_title
            RestoreResult.FAILED -> R.string.restore_failed_title
        },
    )
    val message = when (outcome.result) {
        RestoreResult.RESTORED -> stringResource(R.string.restore_success_message, planName)
        RestoreResult.NOT_FOUND -> stringResource(R.string.restore_none_message)
        RestoreResult.FAILED -> stringResource(R.string.restore_failed_message)
    }
    val action = stringResource(
        when (outcome.result) {
            RestoreResult.RESTORED -> R.string.action_done
            RestoreResult.NOT_FOUND -> R.string.action_ok
            RestoreResult.FAILED -> R.string.action_try_again
        },
    )

    AppDialog(modifier = modifier, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacer_2xl)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
            AppPrimaryButton(
                text = action,
                onClick = if (outcome.result == RestoreResult.FAILED) onRetry else onDismiss,
            )
        }
    }
}
