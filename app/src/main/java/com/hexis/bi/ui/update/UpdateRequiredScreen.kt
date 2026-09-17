package com.hexis.bi.ui.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppLogo
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.theme.screenBackground

@Composable
internal fun UpdateRequiredScreen(
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LightStatusBarIcons()
    BackHandler {}

    Box(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            .swallowPointerInput(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppLogo()

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

            Text(
                text = stringResource(R.string.update_required_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

            Text(
                text = stringResource(
                    R.string.update_required_body,
                    stringResource(R.string.app_name),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

            AppPrimaryButton(
                text = stringResource(R.string.action_update_now),
                onClick = onUpdate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun Modifier.swallowPointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}
