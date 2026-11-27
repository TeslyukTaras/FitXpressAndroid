package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.detailedFeatures
import com.hexis.bi.domain.subscription.features
import com.hexis.bi.ui.main.subscription.label
import com.hexis.bi.ui.theme.NocturnePulseTheme

@Composable
internal fun PlanFeatureRow(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = NocturnePulseTheme.extendedColors.textEmphasis,
        )
    }
}

@Composable
internal fun PlanFeatureList(
    plan: PlanId,
    scansPerMonth: Int,
    modifier: Modifier = Modifier,
    detailed: Boolean = false,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_l)),
    ) {
        (if (detailed) plan.detailedFeatures else plan.features).forEach { feature ->
            PlanFeatureRow(feature.label(scansPerMonth, detailed))
        }
    }
}
