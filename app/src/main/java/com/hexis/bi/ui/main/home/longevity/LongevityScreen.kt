package com.hexis.bi.ui.main.home.longevity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.main.home.longevity.components.LongevityInfoBottomSheet
import com.hexis.bi.ui.main.home.longevity.components.LongevityInsightCard
import com.hexis.bi.ui.main.home.longevity.components.LongevityScoreCard
import com.hexis.bi.ui.main.home.longevity.components.LongevitySignalsCard
import com.hexis.bi.ui.main.home.longevity.components.LongevityStatusCard
import com.hexis.bi.ui.main.home.longevity.components.LongevityTrendCard
import com.hexis.bi.ui.theme.dark.DarkTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongevityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LongevityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    DarkTheme {
        Box(modifier = modifier) {
            BaseScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (state.showInfoSheet) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                        else Modifier
                    )
                    .darkScreenBackground(),
                containerColor = Color.Transparent,
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
                val trendData = when (state.selectedTab) {
                    LongevityTab.Daily -> state.daily
                    LongevityTab.Weekly -> state.weekly
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                ) {
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

                    LongevityScoreCard(
                        score = state.score,
                        syncedDate = state.syncedDate,
                    )

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                    LongevityTrendCard(
                        selectedTab = state.selectedTab,
                        onTabSelected = viewModel::selectTab,
                        trendData = trendData,
                    )

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                    LongevitySignalsCard(signals = state.signals)

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                    LongevityStatusCard(signals = state.statusSignals)

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                    LongevityInsightCard()

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))
                }
            }

            if (state.showInfoSheet) LongevityInfoBottomSheet(onDismiss = viewModel::dismissInfoSheet)
        }
    }
}
