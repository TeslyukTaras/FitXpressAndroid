package com.hexis.bi.ui.auth.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppLogo

@Composable
internal fun AuthTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    var backClickConsumed by remember(onBack) { mutableStateOf(false) }
    val backEnabled = onBack != null && !backClickConsumed

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                top = dimensionResource(R.dimen.spacer_2xl),
                bottom = dimensionResource(R.dimen.spacer_xxl),
            ),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = {
                if (!backEnabled) return@IconButton
                backClickConsumed = true
                onBack()
            },
            enabled = backEnabled,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            if (onBack != null) Icon(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.icon_medium))
                    .rotate(180f),
                painter = painterResource(R.drawable.ic_arrow),
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }

        AppLogo()
    }
}
