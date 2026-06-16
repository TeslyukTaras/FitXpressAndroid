package com.hexis.bi.ui.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.main.buysuit.orderdetails.OrderDetailsSheet
import com.hexis.bi.ui.main.home.components.ActivityOverviewCard
import com.hexis.bi.ui.main.home.components.HomeHeader
import com.hexis.bi.ui.main.home.components.IntelligenceScoresCard
import com.hexis.bi.ui.main.home.components.PromoBanner
import com.hexis.bi.ui.main.home.components.ScanOverviewCard
import com.hexis.bi.ui.main.home.components.SleepOverviewCard
import com.hexis.bi.ui.main.home.components.SuitOrderCard
import com.hexis.bi.ui.main.home.components.UserStatsCard
import com.hexis.bi.ui.theme.dark.DarkTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSleepClick: () -> Unit = {},
    onActivityClick: () -> Unit = {},
    onRecoveryClick: () -> Unit = {},
    onLongevityClick: () -> Unit = {},
    onPhysiqueDriftClick: () -> Unit = {},
    onPaceOfAgingClick: () -> Unit = {},
    onScanClick: () -> Unit = {},
    onBuySuitClick: () -> Unit = {},
    onEditOrderAddress: (orderId: String) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        // Refresh on every return to Home (not just first composition); Terra syncs also push updates.
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshOverview()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToLogin -> onLogout()
            }
        }
    }

    LightStatusBarIcons()

    DarkTheme {
        BaseScreen(
            modifier = modifier
                .fillMaxSize()
                .darkScreenBackground(),
            containerColor = Color.Transparent,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
        ) {
            val navClearance =
                dimensionResource(R.dimen.size_bottom_nav_center) +
                        dimensionResource(R.dimen.spacer_l) +
                        dimensionResource(R.dimen.spacer_2xl)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = dimensionResource(R.dimen.padding_medium),
                        end = dimensionResource(R.dimen.padding_medium),
                        top = dimensionResource(R.dimen.padding_top),
                        bottom = navClearance
                    ),
            ) {
                val scanSubtitle = state.latestScanDate?.let {
                    stringResource(R.string.home_latest_scan, it)
                }
                HomeHeader(
                    userName = state.userName,
                    imageUrl = state.imageUrl,
                    subtitle = scanSubtitle,
                    hasUnreadNotifications = state.hasUnreadNotifications,
                    onNotificationClick = onNotificationClick,
                    onSettingsClick = onSettingsClick,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

                val unknown = stringResource(R.string.stat_unknown)
                UserStatsCard(
                    weight = state.weight ?: unknown,
                    height = state.height ?: unknown,
                    age = state.age ?: unknown,
                )

                val suitOrder = state.suitOrder
                if (suitOrder != null) {
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
                    SuitOrderCard(
                        data = suitOrder,
                        onClick = viewModel::showOrderDetails,
                    )
                } else if (state.suitSectionResolved && !state.isSuitConnected) {
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
                    PromoBanner(onBuyClick = onBuySuitClick)
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

                SectionTitle(stringResource(R.string.home_overview_title))

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
                ) {
                    ActivityOverviewCard(
                        data = state.activity,
                        onClick = onActivityClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    SleepOverviewCard(
                        data = state.sleep,
                        onClick = onSleepClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

                ScanOverviewCard(
                    data = state.scan,
                    onClick = onScanClick,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

                SectionTitle(stringResource(R.string.home_intelligence_title))

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                IntelligenceScoresCard(
                    scores = state.intelligenceScores,
                    onScoreClick = { key ->
                        when (key) {
                            IntelligenceScoreKey.RECOVERY -> onRecoveryClick()
                            IntelligenceScoreKey.PHYSIQUE_DRIFT -> onPhysiqueDriftClick()
                            IntelligenceScoreKey.LONGEVITY -> onLongevityClick()
                            IntelligenceScoreKey.PACE_OF_AGING -> onPaceOfAgingClick()
                        }
                    },
                )
            }
        }

        val orderDetails = state.orderDetails
        if (state.showOrderDetails && orderDetails != null) {
            OrderDetailsSheet(
                details = orderDetails,
                onDismiss = viewModel::dismissOrderDetails,
                onEditAddress = {
                    viewModel.dismissOrderDetails()
                    onEditOrderAddress(orderDetails.orderId)
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onBackground,
    )
}
