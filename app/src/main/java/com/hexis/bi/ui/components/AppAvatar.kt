package com.hexis.bi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.hexis.bi.R

@Composable
fun AppAvatar(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = dimensionResource(R.dimen.size_avatar),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryFixed),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.cd_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            error = painterResource(R.drawable.ic_user),
            placeholder = painterResource(R.drawable.ic_user),
        )
        else Icon(
            painter = painterResource(R.drawable.ic_user),
            contentDescription = stringResource(R.string.cd_avatar),
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_large)),
        )
    }
}
