package com.hexis.bi.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Standard screen scaffold used across the app.
 *
 * Handles:
 * - Loading overlay with [CircularProgressIndicator]
 * - Persistent error snackbar (stays until user dismisses) when [error] is non-null
 * - Short-lived info snackbar when [message] is non-null
 * - Optional [topBar] and [bottomBar] slots
 *
 * Usage:
 * ```
 * BaseScreen(
 *     isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value,
 *     error = viewModel.error.collectAsStateWithLifecycle().value,
 *     onDismissError = viewModel::clearError,
 *     message = viewModel.message.collectAsStateWithLifecycle().value,
 *     onDismissMessage = viewModel::clearMessage,
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
    message: String? = null,
    onDismissMessage: () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Indefinite,
                withDismissAction = true,
            )
            onDismissError()
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
            onDismissMessage()
        }
    }

    Box(modifier = modifier) {
        Scaffold(
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.imePadding()) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                content()
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .imePadding(),
                )
            }
        }
    }
}
