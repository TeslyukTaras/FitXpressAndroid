package com.hexis.bi.ui.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppLogo

/** Pinned, transparent auth header: the centered app logo with its surrounding spacing. */
@Composable
internal fun AuthLogoTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
        AppLogo(tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))
    }
}
