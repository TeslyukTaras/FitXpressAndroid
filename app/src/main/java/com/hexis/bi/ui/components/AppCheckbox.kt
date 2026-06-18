package com.hexis.bi.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.hexis.bi.R

/**
 * Design-system checkbox rendered from the design icons: an accent-tinted square
 * ([R.drawable.ic_square]) that gains a checkmark when [checked] ([R.drawable.ic_square_check]).
 *
 * Pass a null [onCheckedChange] to render the box as display-only when an enclosing row owns the
 * click (so the whole row is the touch target).
 */
@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val toggle = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
        )
    } else {
        Modifier
    }
    Icon(
        painter = painterResource(
            if (checked) R.drawable.ic_square_check else R.drawable.ic_square,
        ),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .size(dimensionResource(R.dimen.checkbox_size))
            .then(toggle),
    )
}
