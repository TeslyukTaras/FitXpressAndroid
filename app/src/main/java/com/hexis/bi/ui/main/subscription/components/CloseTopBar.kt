package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R

@Composable
internal fun CloseTopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val touchInset =
        (dimensionResource(R.dimen.touch_target_size) - dimensionResource(R.dimen.icon_medium)) / 2
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(
                start = dimensionResource(R.dimen.padding_medium) - touchInset,
                top = dimensionResource(R.dimen.subscription_close_top_padding) - touchInset,
                bottom = dimensionResource(R.dimen.subscription_close_bottom_padding) - touchInset,
            ),
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.ic_cross),
                contentDescription = stringResource(R.string.cd_close),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
        }
    }
}
