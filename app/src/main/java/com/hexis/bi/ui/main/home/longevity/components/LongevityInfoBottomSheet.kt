package com.hexis.bi.ui.main.home.longevity.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import com.hexis.bi.ui.base.BaseBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongevityInfoBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = stringResource(R.string.longevity_screen_title),
        onDismiss = onDismiss,
        modifier = modifier.fillMaxHeight(0.8f),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            InfoParagraph(R.string.longevity_info_sheet_what)
            InfoParagraph(R.string.longevity_info_sheet_inputs)
            InfoParagraph(R.string.longevity_info_sheet_windows)
            InfoParagraph(R.string.longevity_info_sheet_limits)
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(
                text = stringResource(R.string.action_got_it),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun InfoParagraph(@StringRes body: Int) {
    Text(
        text = stringResource(body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
}
