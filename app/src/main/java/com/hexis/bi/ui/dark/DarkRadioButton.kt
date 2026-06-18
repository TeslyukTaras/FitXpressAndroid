package com.hexis.bi.ui.dark

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
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
 * Dark design-system radio control rendered from the design icons: a grey ring when unselected
 * ([R.drawable.ic_circle]) and an accent ring with a filled centre when selected
 * ([R.drawable.ic_circle_check]).
 *
 * Pass a null [onClick] to render the ring as display-only when an enclosing row owns the click
 * (so the whole row is the touch target).
 */
@Composable
fun DarkRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val select = if (onClick != null) {
        Modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
        )
    } else {
        Modifier
    }
    Icon(
        painter = painterResource(
            if (selected) R.drawable.ic_circle_check else R.drawable.ic_circle,
        ),
        contentDescription = null,
        tint = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondary
        },
        modifier = modifier
            .size(dimensionResource(R.dimen.radio_button_size))
            .then(select),
    )
}
