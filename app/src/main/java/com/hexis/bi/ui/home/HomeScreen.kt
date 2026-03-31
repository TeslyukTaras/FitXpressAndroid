package com.hexis.bi.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.components.AppButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
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
        isLoading = isLoading,
        error = error,
        onDismissError = viewModel::clearError,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            contentAlignment = Alignment.Center,
        ) {
            AppButton(
                text = stringResource(R.string.action_logout),
                onClick = viewModel::logout,
            )
        }
    }
}
