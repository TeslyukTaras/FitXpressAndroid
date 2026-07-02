package com.hexis.bi.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * App shape scale mapped to Material3 slots:
 *
 * extraSmall  4dp  — badges, tooltips
 * small       8dp  — buttons, promo actions
 * medium      10dp — cards, banners, dialogs
 * large       16dp — navigation drawers, side sheets
 * extraLarge  28dp — bottom sheets, large surfaces
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
