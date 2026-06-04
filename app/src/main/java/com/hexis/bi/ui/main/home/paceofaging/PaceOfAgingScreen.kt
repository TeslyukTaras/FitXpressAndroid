package com.hexis.bi.ui.main.home.paceofaging

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.ComingSoonScreen

@Composable
fun PaceOfAgingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ComingSoonScreen(
        titleRes = R.string.pace_of_aging_screen_title,
        messageRes = R.string.pace_of_aging_coming_soon,
        onBack = onBack,
        modifier = modifier,
    )
}
