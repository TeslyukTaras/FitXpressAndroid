package com.hexis.bi.ui.main.home.recovery.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
fun RecoveryInfoCard(
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(
        modifier = modifier,
        onClick = onInfoClick,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.recovery_info_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
                Text(
                    text = stringResource(R.string.recovery_info_body),
                    style = TitleDimTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = stringResource(R.string.cd_info),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
        }
    }
}
