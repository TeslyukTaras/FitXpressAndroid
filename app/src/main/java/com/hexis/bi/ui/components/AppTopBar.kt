package com.hexis.bi.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    containerColor: Color? = null,
    logoTint: Color? = null,
    navigationIconTint: Color? = null,
) {
    val resolvedLogoTint = logoTint ?: MaterialTheme.colorScheme.onBackground
    val resolvedNavigationIconTint = navigationIconTint ?: MaterialTheme.colorScheme.onBackground
    val resolvedContainerColor = containerColor ?: MaterialTheme.colorScheme.background

    CenterAlignedTopAppBar(
        title = { AppLogo(tint = resolvedLogoTint) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.icon_medium))
                            .rotate(180f),
                        painter = painterResource(R.drawable.ic_arrow),
                        contentDescription = stringResource(R.string.cd_back),
                        tint = resolvedNavigationIconTint,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = resolvedContainerColor,
        ),
        modifier = modifier,
    )
}
