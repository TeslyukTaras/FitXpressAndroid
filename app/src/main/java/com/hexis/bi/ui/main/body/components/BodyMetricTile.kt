package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.Blue100

@Composable
internal fun BodyMetricTile(
    label: String,
    value: String,
    delta: Float?,
    modifier: Modifier = Modifier,
    /** True for metrics where a decrease is healthier (e.g. body fat); flips the delta colour. */
    decreaseIsPositive: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = Blue100,
                )
                if (delta != null) BodyDelta(delta = delta, decreaseIsPositive = decreaseIsPositive)
            }
        }
        if (trailingIcon != null) Box(modifier = Modifier.align(Alignment.TopEnd)) { trailingIcon() }
    }
}

@Composable
internal fun BodyInfoIconButton(
    onClick: () -> Unit,
    contentDescription: String = stringResource(R.string.cd_body_bis_info),
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primaryFixed,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
        )
    }
}
