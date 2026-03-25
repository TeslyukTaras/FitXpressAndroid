package com.hexis.bi.ui.auth.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R

@Composable
internal fun SocialAuthRow(
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.Center) {
        SocialButton(
            iconRes = R.drawable.ic_google,
            contentDescription = stringResource(R.string.cd_google_signin),
            onClick = onGoogleClick,
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_large)))
        SocialButton(
            iconRes = R.drawable.ic_apple,
            contentDescription = stringResource(R.string.cd_apple_signin),
            onClick = onAppleClick,
        )
    }
}

@Composable
private fun SocialButton(iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(dimensionResource(R.dimen.size_social_button))
            .clip(CircleShape)
            .border(dimensionResource(R.dimen.border_thin), MaterialTheme.colorScheme.secondaryFixed, CircleShape),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = androidx.compose.ui.graphics.Color.Unspecified,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
    }
}
