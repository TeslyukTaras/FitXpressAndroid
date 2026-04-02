package com.hexis.bi.ui.main.home

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.main.home.components.HomeHeader
import com.hexis.bi.ui.main.home.components.OverviewCard
import com.hexis.bi.ui.main.home.components.PromoBanner
import com.hexis.bi.ui.main.home.components.UserStatsCard
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToLogin -> onLogout()
            }
        }
    }

    BaseScreen(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        isLoading = isLoading,
        error = error,
        onDismissError = viewModel::clearError,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(
                    start = dimensionResource(R.dimen.padding_medium),
                    end = dimensionResource(R.dimen.padding_medium),
                    top = dimensionResource(R.dimen.padding_top),
                ),
        ) {
            HomeHeader(
                userName = state.userName,
                avatarUrl = state.avatarUrl,
                onNotificationClick = onNotificationClick,
                onSettingsClick = onSettingsClick,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            UserStatsCard(
                weight = state.weight,
                height = state.height,
                age = state.age,
            )

            if (state.showBanner) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
                PromoBanner(onBuyClick = { /* TODO */ })
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            Text(
                text = stringResource(R.string.home_overview_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

            OverviewGrid(cards = state.overviewCards)

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))
        }
    }
}

@Composable
private fun OverviewGrid(cards: List<OverviewCardData>) {
    val rows = cards.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m))) {
        rows.forEach { rowCards ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
            ) {
                rowCards.forEach { card ->
                    OverviewCard(
                        title = card.title,
                        iconRes = card.iconRes,
                        value = card.value,
                        subtitle = card.subtitle,
                        variant = card.variant,
                        valueLabel = card.valueLabel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                // Pad last row if odd number of cards
                if (rowCards.size < 2) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
