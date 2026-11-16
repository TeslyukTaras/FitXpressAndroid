package com.hexis.bi.ui.main.scan.access

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.theme.screenBackground

@Composable
internal fun ScanGateBackdrop(
    onBack: () -> Unit,
    blurred: Boolean,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            .then(if (blurred) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop)) else Modifier),
        containerColor = Color.Transparent,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.scan_title),
                background = Color.Transparent,
                onBack = onBack,
            )
        },
    ) {}
}
