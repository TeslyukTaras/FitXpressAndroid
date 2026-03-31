package com.hexis.bi.ui.main.home.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.OverviewCardVariant
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.Lime100
import com.hexis.bi.ui.theme.Lime200
import com.hexis.bi.utils.gradientBackground

@Composable
fun OverviewCard(
    title: String,
    @DrawableRes iconRes: Int,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    variant: OverviewCardVariant = OverviewCardVariant.Default,
    valueLabel: String? = null,
) {
    val isPrimary = variant == OverviewCardVariant.Primary
    val primaryGradient = Brush.verticalGradient(listOf(Blue300, Blue200))

    val bgColor = when (variant) {
        OverviewCardVariant.Default -> MaterialTheme.colorScheme.background
        OverviewCardVariant.Accent -> Lime100
        OverviewCardVariant.Primary -> Color.Transparent
    }
    val valueColor = when (variant) {
        OverviewCardVariant.Primary -> Lime200
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val contentColor = when (variant) {
        OverviewCardVariant.Primary -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onBackground
    }
    val subtitleColor = when (variant) {
        OverviewCardVariant.Primary -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.secondary
    }

    val gradientModifier = if (isPrimary)
        modifier.gradientBackground(brush = primaryGradient, shape = MaterialTheme.shapes.medium)
    else modifier

    Card(
        modifier = gradientModifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = bgColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_none)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacer_m)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                    color = contentColor,
                )
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

            Text(
                color = valueColor,
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                            fontWeight = MaterialTheme.typography.headlineMedium.fontWeight,
                        )
                    ) { append(value) }
                    if (valueLabel != null) {
                        append(" ")
                        withStyle(
                            SpanStyle(
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
                            )
                        ) { append(valueLabel) }
                    }
                },
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
            )
        }
    }
}
