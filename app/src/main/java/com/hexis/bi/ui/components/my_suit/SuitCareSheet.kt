package com.hexis.bi.ui.components.my_suit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseBottomSheet
import com.hexis.bi.ui.components.AppPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuitCareSheet(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val acceptedState = rememberUpdatedState(accepted)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || acceptedState.value },
    )

    BaseBottomSheet(
        onDismiss = { if (accepted) onDismiss() },
        sheetState = sheetState,
        modifier = modifier,
        title = stringResource(R.string.suit_care_title),
    ) {
        Text(
            text = stringResource(R.string.suit_care_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Text(
            text = stringResource(R.string.suit_care_instructions_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))

        stringArrayResource(R.array.suit_care_instructions).forEach { instruction ->
            CareInstructionRow(text = instruction)
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        Text(
            text = stringResource(R.string.suit_care_footer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        ConfirmRow(accepted = accepted, onToggle = { onAcceptedChange(!accepted) })

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

        AppPrimaryButton(
            text = stringResource(R.string.action_continue),
            onClick = onContinue,
            enabled = accepted,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CareInstructionRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacer_2xs)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_2xs)),
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.spacer_xs))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ConfirmRow(accepted: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        ) {
            Icon(
                painter = painterResource(
                    if (accepted) R.drawable.ic_checkbox_check else R.drawable.ic_checkbox_uncheck
                ),
                contentDescription = null,
                tint = if (accepted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.suit_care_confirm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
