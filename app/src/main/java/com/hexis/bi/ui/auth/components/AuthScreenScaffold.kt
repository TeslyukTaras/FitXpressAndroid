package com.hexis.bi.ui.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.AuthFlowConstants

/**
 * Shared shell for the auth form screens (login, sign up): dark theme + mesh background, the
 * brand-tinted teal gradient at the top, a pinned [AuthLogoTopBar], and a bottom-anchored,
 * scrollable content column (so short screens / the IME keyboard never clip the form).
 *
 * [content] is laid out in a [ColumnScope] anchored to the bottom of the available height.
 */
@Composable
internal fun AuthScreenScaffold(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null,
    onDismissError: () -> Unit = {},
    message: String? = null,
    onDismissMessage: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    DarkTheme {
        LightStatusBarIcons()

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

            BaseScreen(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                isLoading = isLoading,
                error = error,
                onDismissError = onDismissError,
                message = message,
                onDismissMessage = onDismissMessage,
                topBar = { AuthLogoTopBar() },
            ) {
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
                            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                            .padding(bottom = dimensionResource(R.dimen.padding_auth_vertical)),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content,
                    )
                }
            }
        }
    }
}
