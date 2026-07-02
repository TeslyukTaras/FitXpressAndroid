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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withStateAtLeast

/**
 * Standard screen scaffold: loading overlay, persistent error snackbar, short info snackbar,
 * and optional [topBar] / [bottomBar] slots.
 */
@Composable
fun BaseScreen(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    isLoading: Boolean = false,
    error: String? = null,
    onDismissError: () -> Unit = {},
    message: String? = null,
    onDismissMessage: () -> Unit = {},
    viewModel: BaseViewModel? = null,
    onInitialization: (() -> Unit)? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    loadingContent: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    if (viewModel != null) LaunchedEffect(viewModel) {
        // Wait until the destination is RESUMED (nav transition complete) so heavy
        // startup work doesn't compete with the slide/fade animation.
        lifecycleOwner.lifecycle.withStateAtLeast(Lifecycle.State.RESUMED) {
            viewModel.runOnceOnInitialize()
        }
    }
    if (onInitialization != null) LaunchedEffect(lifecycleOwner) {
        onInitialization()
    }

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
            containerColor = containerColor,
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
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
            ) {
                if (loadingContent != null) {
                    loadingContent()
                } else {
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
}
