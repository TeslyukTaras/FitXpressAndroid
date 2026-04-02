package com.hexis.bi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.hexis.bi.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppDropdown(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        AppTextField(
            value = optionLabel(selectedOption),
            onValueChange = {},
            readOnly = true,
            label = label,
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_medium))
                        .rotate(if (expanded) 270f else 90f),
                )
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        DropdownMenu(
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.background,
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = dimensionResource(R.dimen.spacer_xxs)),
            modifier = Modifier.exposedDropdownSize()
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                DropdownMenuItem(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .padding(
                            horizontal = dimensionResource(R.dimen.spacer_s),
                            vertical = dimensionResource(R.dimen.spacer_3xs)
                        )
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                            shape = MaterialTheme.shapes.small
                        ),
                    text = {
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    contentPadding = PaddingValues(dimensionResource(R.dimen.padding_small))
                )
            }
        }
    }
}