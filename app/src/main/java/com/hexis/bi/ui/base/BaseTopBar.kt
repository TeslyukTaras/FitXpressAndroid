package com.hexis.bi.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
@Composable
fun BaseTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            actions()
        }
    }
}
