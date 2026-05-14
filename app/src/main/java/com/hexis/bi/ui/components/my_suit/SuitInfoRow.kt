package com.hexis.bi.ui.components.my_suit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R

@Composable
fun SuitInfoRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val resolvedValueColor = valueColor.takeOrElse { MaterialTheme.colorScheme.onBackground }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
        )
        Text(
            text = stringResource(R.string.colon),
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacer_xl)))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = resolvedValueColor,
        )
    }
}