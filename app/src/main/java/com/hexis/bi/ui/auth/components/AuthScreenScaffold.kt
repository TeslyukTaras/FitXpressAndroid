package com.hexis.bi.ui.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.AuthFlowConstants

/**
 * Shared shell for the auth form screens: dark theme + mesh background, the brand-tinted
 * teal gradient at the top, and a bottom-anchored, scrollable content column (so short screens /
 * the IME keyboard never clip the form).
 *
 * The auth header is part of the scrollable content rather than a scaffold top bar.
 */
@Composable
internal fun AuthScreenScaffold(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null,
    onDismissError: () -> Unit = {},
    message: String? = null,
    onDismissMessage: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    bottomPadding: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    DarkTheme {
        LightStatusBarIcons()
        val resolvedBottomPadding = bottomPadding ?: dimensionResource(R.dimen.padding_auth_vertical)
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

        Box(
            modifier = modifier
                .fillMaxSize()
                .darkScreenBackground(),
        ) {
            Image(
                painter = painterResource(R.drawable.img_auth_gradient),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(
                    MaterialTheme.colorScheme.primary.copy(alpha = AuthFlowConstants.GRADIENT_TINT_ALPHA),
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            )

            val scrollState = rememberScrollState()
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .heightIn(min = maxHeight)
                        .navigationBarsPadding()
                        .padding(bottom = resolvedBottomPadding),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AuthTopBar(onBack = onBack)
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content,
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding(),
            )

            if (isLoading) LoadingOverlay()
        }
    }
}

@Composable
private fun BoxScope.LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Center)
                .imePadding(),
        )
    }
}
