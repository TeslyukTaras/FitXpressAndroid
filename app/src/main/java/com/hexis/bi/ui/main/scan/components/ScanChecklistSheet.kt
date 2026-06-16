package com.hexis.bi.ui.main.scan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseBottomSheet
import com.hexis.bi.ui.dark.DarkPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanChecklistSheet(
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = stringResource(R.string.scan_checklist_title),
        onDismiss = onDismiss,
        modifier = modifier.fillMaxHeight(0.75f),
    ) {
        Text(
            text = stringResource(R.string.scan_checklist_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Text(
            text = stringResource(R.string.scan_checklist_label),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m))) {
            ScanChecklistItem(stringResource(R.string.scan_checklist_fitted_clothing))
            ScanChecklistItem(stringResource(R.string.scan_checklist_good_lighting))
            ScanChecklistItem(stringResource(R.string.scan_checklist_clean_background))
            ScanChecklistItem(stringResource(R.string.scan_checklist_volume_on))
            ScanChecklistItem(stringResource(R.string.scan_checklist_voice_guidance))
            ScanChecklistItem(stringResource(R.string.scan_checklist_front_left_photos))
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Text(
            text = stringResource(R.string.scan_checklist_footer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        DarkPrimaryButton(
            text = stringResource(R.string.action_continue),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScanChecklistItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(dimensionResource(R.dimen.spacer_xs))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
