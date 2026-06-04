package com.hexis.bi.ui.main.home.physiquedrift

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.ComingSoonScreen

@Composable
fun PhysiqueDriftScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ComingSoonScreen(
        titleRes = R.string.physique_drift_screen_title,
        messageRes = R.string.physique_drift_coming_soon,
        onBack = onBack,
        modifier = modifier,
    )
}
