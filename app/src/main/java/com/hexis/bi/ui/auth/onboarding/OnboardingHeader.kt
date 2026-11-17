package com.hexis.bi.ui.auth.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppLogo

@Composable
internal fun OnboardingHeader(onBack: (() -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(R.dimen.spacer_2xl),
                    bottom = dimensionResource(R.dimen.spacer_xxl),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_medium))
                        .rotate(BACK_ARROW_ROTATION),
                )
            }
            AppLogo()
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_4xl)))
    }
}

private const val BACK_ARROW_ROTATION = 180f
