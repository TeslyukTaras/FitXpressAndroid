package com.hexis.bi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R

/**
 * Standard full-screen dialog overlay used across the app.
 *
 * Renders a dimmed backdrop covering the full screen and a centered [Card] with
 * the app's background color. Supports keyboard insets via [imePadding].
 *
 * The blur effect on the underlying screen content is intentionally NOT handled here —
 * each screen is responsible for blurring its own content when a dialog is visible,
 * since some screens may have composables rendered outside [BaseScreen].
 *
 * Usage:
 * ```kotlin
 * Box(modifier = modifier) {
 *     BaseScreen(
 *         modifier = Modifier.then(
 *             if (showDialog) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
 *             else Modifier
 *         ),
 *     ) { screenContent() }
 *
 *     if (showDialog) {
 *         AppDialog {
 *             // dialog content
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun AppDialog(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.25f))
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        ) {
            content()
        }
    }
}
