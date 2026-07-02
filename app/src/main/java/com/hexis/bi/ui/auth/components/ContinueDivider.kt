package com.hexis.bi.ui.auth.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.GradientDividerDirection

/**
 * "Or Continue with" separator: a centered label flanked by gradient rules that are solid next to
 * the text and fade out toward the screen edges.
 */
@Composable
internal fun ContinueDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AppHorizontalGradientDivider(
            modifier = Modifier.weight(1f),
            direction = GradientDividerDirection.END,
        )
        Text(
            text = stringResource(R.string.or_continue_with),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        )
        AppHorizontalGradientDivider(
            modifier = Modifier.weight(1f),
            direction = GradientDividerDirection.START,
        )
    }
}
