package com.hexis.bi.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Standard screen scaffold used across the app.
 *
 * Handles:
 * - Loading overlay with [CircularProgressIndicator]
 * - Snackbar display when [error] is non-null (auto-cleared via [onDismissError])
 * - Optional [topBar] and [bottomBar] slots
 *
 * Usage:
 * ```
 * BaseScreen(
 *     isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value,
 *     error = viewModel.error.collectAsStateWithLifecycle().value,
 *     onDismissError = viewModel::clearError,
 *     topBar = { MyTopBar() },
 * ) {
 *     MyContent()
 * }
 * ```
 */
@Composable
fun BaseScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null,
    onDismissError: () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            onDismissError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
