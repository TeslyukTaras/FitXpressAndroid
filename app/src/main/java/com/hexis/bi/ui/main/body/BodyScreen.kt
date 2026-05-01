package com.hexis.bi.ui.main.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.components.AppTabSelector
import org.koin.androidx.compose.koinViewModel

@Composable
fun BodyScreen(
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BodyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BaseScreen(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        ) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.body_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_normalized))
                        .align(Alignment.Top),
                    onClick = onHistoryClick,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_history),
                        contentDescription = stringResource(R.string.cd_body_history),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                    )
                }

            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
            AppTabSelector(
                tabs = BodyTab.entries,
                selectedTab = state.selectedTab,
                onTabSelected = viewModel::selectTab,
                tabLabel = { stringResource(it.labelRes) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
