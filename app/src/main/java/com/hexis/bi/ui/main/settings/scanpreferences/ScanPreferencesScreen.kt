package com.hexis.bi.ui.main.settings.scanpreferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppSwitch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScanPreferencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanPreferencesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BaseScreen(
        modifier = modifier,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.scan_preferences_title),
                onBack = onBack,
            )
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                .padding(top = dimensionResource(R.dimen.spacer_l)),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.scan_preferences_voice_guidance),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
                Text(
                    text = stringResource(R.string.scan_preferences_voice_guidance_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            AppSwitch(
                checked = state.voiceGuidanceEnabled,
                onCheckedChange = viewModel::toggleVoiceGuidance,
            )
        }
    }
}
