package com.hexis.bi.ui.main.home.longevity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppTabSelector
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.main.home.longevity.components.LongevityDirectionCard
import com.hexis.bi.ui.main.home.longevity.components.LongevityFoundationCard
import com.hexis.bi.ui.main.home.longevity.components.LongevityInfoBottomSheet
import com.hexis.bi.ui.theme.screenBackground
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongevityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LongevityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.showInfoSheet) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                )
                .screenBackground(),
            containerColor = Color.Transparent,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.longevity_screen_title),
                    onBack = onBack,
                    background = Color.Transparent,
                    actions = {
                        IconButton(onClick = viewModel::showInfoSheet) {
                            Icon(
                                painter = painterResource(R.drawable.ic_info),
                                contentDescription = stringResource(R.string.cd_info),
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                            )
                        }
                    },
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            ) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

                AppTabSelector(
                    tabs = LongevityWindow.entries,
                    selectedTab = state.selectedWindow,
                    onTabSelected = viewModel::selectWindow,
                    modifier = Modifier.fillMaxWidth(),
                    tabLabel = { stringResource(it.labelRes) },
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

                LongevityDirectionCard(direction = state.direction)

                state.foundations.forEach { foundation ->
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
                    LongevityFoundationCard(foundation = foundation)
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))
            }
        }

        if (state.showInfoSheet) LongevityInfoBottomSheet(onDismiss = viewModel::dismissInfoSheet)
    }
}
