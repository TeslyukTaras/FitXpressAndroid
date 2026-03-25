package com.hexis.bi.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * App shape scale mapped to Material3 slots:
 *
 * extraSmall  4dp  — badges, tooltips
 * small       8dp  — text fields, chips, snackbars
 * medium      12dp — buttons, cards, dialogs
 * large       16dp — navigation drawers, side sheets
 * extraLarge  28dp — bottom sheets, large surfaces
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
