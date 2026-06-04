package com.hexis.bi.ui.main.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.theme.dark.DarkTheme

/** Dark-themed placeholder for Body Intelligence features that aren't built yet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LightStatusBarIcons()

    DarkTheme {
        BaseScreen(
            modifier = modifier
                .fillMaxSize()
                .darkScreenBackground(),
            containerColor = Color.Transparent,
            topBar = {
                BaseTopBar(
                    title = stringResource(titleRes),
                    onBack = onBack,
                    background = Color.Transparent,
                )
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.padding_medium)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
