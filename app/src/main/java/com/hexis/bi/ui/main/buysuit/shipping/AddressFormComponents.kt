package com.hexis.bi.ui.main.buysuit.shipping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseBottomSheet
import com.hexis.bi.ui.components.AppOutlinedTextField
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.constants.ShippingConstants

/** Appends an "(optional)" suffix unless the field is required. */
@Composable
internal fun requiredLabel(label: String, isRequired: Boolean): String =
    if (isRequired) label else stringResource(R.string.shipping_optional_label, label)

/** A single full-width address text field with trailing spacing. */
@Composable
internal fun ShippingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    error: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AppOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            error = error,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
    }
}

/** Read-only field that opens the country picker on tap. */
@Composable
internal fun CountryPickerField(
    country: ShippingCountry,
    label: String,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box {
            AppOutlinedTextField(
                value = country.name,
                onValueChange = {},
                label = label,
                readOnly = true,
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.icon_medium))
                            .rotate(90f),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onClick),
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun CountryPickerSheet(
    countries: List<ShippingCountry>,
    selectedCountry: ShippingCountry,
    showDialCode: Boolean,
    onCountrySelected: (ShippingCountry) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, countries) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            countries
        } else {
            countries.filter { country ->
                country.name.contains(trimmed, ignoreCase = true) ||
                        country.dialCode.contains(trimmed) ||
                        country.isoCode.contains(trimmed, ignoreCase = true)
            }
        }
    }

    BaseBottomSheet(
        title = stringResource(R.string.shipping_select_country),
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(ShippingConstants.COUNTRY_SHEET_HEIGHT_FRACTION),
    ) {
        AppOutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.shipping_search_country),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(filtered, key = { it.isoCode }) { country ->
                CountryRow(
                    country = country,
                    selected = country.isoCode == selectedCountry.isoCode,
                    showDialCode = showDialCode,
                    onClick = { onCountrySelected(country) },
                )
            }
        }
    }
}

@Composable
private fun CountryRow(
    country: ShippingCountry,
    selected: Boolean,
    showDialCode: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = GlassConstants.SELECTION_HIGHLIGHT_ALPHA)
                else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.spacer_m),
                vertical = dimensionResource(R.dimen.spacer_s),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountryFlagCircle(country = country, size = dimensionResource(R.dimen.icon_large))
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = country.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = country.isoCode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showDialCode) {
            Text(
                text = country.dialCode,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun CountryFlagCircle(
    country: ShippingCountry,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = country.flag,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
