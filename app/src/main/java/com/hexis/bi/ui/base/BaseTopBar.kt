package com.hexis.bi.ui.base

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R

/**
 * Standard top bar used across detail/inner screens.
 *
 * Shows a centered [title] with an optional back arrow on the left
 * and an optional action slot on the right.
 *
 * Usage:
 * ```
 * BaseTopBar(
 *     title = "Sleep",
 *     onBack = { navController.popBackStack() },
 *     actions = {
 *         IconButton(onClick = { }) {
 *             Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
 *         }
 *     }
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseTopBar(
    title: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    onBack: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.icon_medium))
                            .rotate(180f),
                        painter = painterResource(R.drawable.ic_arrow),
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = background,
        ),
        modifier = modifier,
        actions = actions
    )
}
