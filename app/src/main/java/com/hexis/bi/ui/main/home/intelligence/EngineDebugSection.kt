package com.hexis.bi.ui.main.home.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.BuildConfig
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard

@Composable
fun EngineDebugSection(
    info: EngineDebugInfo?,
    modifier: Modifier = Modifier,
    presentation: EngineDebugPresentation = EngineDebugPresentation.DIAGNOSTIC,
) {
    if (!BuildConfig.DEBUG || info == null) return

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(dimensionResource(R.dimen.insight_card_padding)),
    ) {
        if (presentation == EngineDebugPresentation.DIAGNOSTIC) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Intelligence Engine · DEBUG",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        }

        if (info.findings.isEmpty()) {
            Text(
                text = "No published findings for ${info.areas.sorted().joinToString(" / ")}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        } else {
            if (presentation != EngineDebugPresentation.COMPACT_WITHOUT_WINDOW) {
                Text(
                    text = "Window: ${info.windowDays} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            }
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s))) {
                info.findings.forEach { finding -> DebugFindingRow(finding) }
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        }

        if (info.findings.isEmpty() || info.suppressed.isNotEmpty()) {
            Text(
                text = "findings ${info.findings.size} · trends ${info.trendCount} · " +
                    "suppressed ${info.suppressed.size} · quality failures ${info.qualityFailureCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        info.suppressed.forEach { suppressed ->
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            Text(
                text = suppressed.insightId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "suppressed: ${suppressed.reason}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            suppressed.message?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

enum class EngineDebugPresentation {
    DIAGNOSTIC,
    COMPACT,
    COMPACT_WITHOUT_WINDOW,
}

@Composable
private fun DebugFindingRow(finding: DebugFinding) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DebugFindingHeadline(finding)
        if (finding.factors.isNotEmpty()) {
            Text(
                text = finding.factors.entries.joinToString(FACTOR_SEPARATOR) { (key, value) ->
                    "${key.take(FACTOR_LABEL_CHARS)} ${value.iosDebugScore()}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DebugFindingHeadline(finding: DebugFinding) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = finding.insightId,
            modifier = Modifier.weight(0.55f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = finding.priorityRank.toString(),
            modifier = Modifier.weight(0.12f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${finding.confidence} ${finding.confidenceScore.iosDebugScore()}",
            modifier = Modifier.weight(0.33f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Double.iosDebugScore(): String = "%.3f".format(this).replace('.', ',')

private const val FACTOR_SEPARATOR = " · "
private const val FACTOR_LABEL_CHARS = 3
