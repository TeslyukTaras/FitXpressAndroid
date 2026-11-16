package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.data.health.access.HealthDataGate
import com.hexis.bi.domain.subscription.HealthDataAccess
import org.koin.compose.koinInject

@Composable
internal fun rememberHealthDataAccess(gate: HealthDataGate = koinInject()): HealthDataAccess =
    gate.accessFlow.collectAsStateWithLifecycle(initialValue = HealthDataAccess.FULL).value

@Composable
internal fun HealthAccessBanner(
    access: HealthDataAccess,
    freeText: String?,
    frozenText: String,
    spacerBelow: Dp,
    modifier: Modifier = Modifier,
    spacerAbove: Dp = 0.dp,
) {
    val text = when (access) {
        HealthDataAccess.FULL -> null
        HealthDataAccess.FIRST_WEEK -> freeText
        HealthDataAccess.FROZEN -> frozenText
    } ?: return
    Spacer(Modifier.height(spacerAbove))
    InfoNote(
        text = text,
        modifier = modifier,
        gap = dimensionResource(R.dimen.spacer_xs),
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(spacerBelow))
}
