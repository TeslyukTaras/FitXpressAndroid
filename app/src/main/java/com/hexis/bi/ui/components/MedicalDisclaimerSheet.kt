package com.hexis.bi.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalDisclaimerSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    requireAcknowledgement: Boolean = false,
) {
    BaseBottomSheet(
        title = stringResource(R.string.medical_disclaimer_title),
        onDismiss = if (requireAcknowledgement) ({}) else onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { !requireAcknowledgement || it != SheetValue.Hidden },
        ),
        properties = if (requireAcknowledgement) {
            ModalBottomSheetProperties(
                shouldDismissOnBackPress = false,
                shouldDismissOnClickOutside = false,
            )
        } else ModalBottomSheetDefaults.properties,
    ) {
        Paragraph(R.string.medical_disclaimer_body_1, isFirst = true)
        Paragraph(R.string.medical_disclaimer_body_2)
        Paragraph(R.string.medical_disclaimer_body_3)
        Paragraph(R.string.medical_disclaimer_body_4)

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(
                text = stringResource(
                    if (requireAcknowledgement) R.string.medical_disclaimer_acknowledge
                    else R.string.action_got_it
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun Paragraph(@StringRes body: Int, isFirst: Boolean = false) {
    if (!isFirst) Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    Text(
        text = stringResource(body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
