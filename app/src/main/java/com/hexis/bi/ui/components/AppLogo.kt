package com.hexis.bi.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    val painter = painterResource(R.drawable.ic_logo_wordmark)
    Image(
        painter = painter,
        contentDescription = stringResource(R.string.app_name),
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(dimensionResource(R.dimen.logo_width))
            .aspectRatio(painter.intrinsicSize.width / painter.intrinsicSize.height),
    )
}
