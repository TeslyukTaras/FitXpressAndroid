package com.hexis.bi.ui.main.scan.startscan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppSlider
import com.hexis.bi.ui.main.scan.components.ScanViewfinder
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.utils.gradientBackground
import org.koin.androidx.compose.koinViewModel

@Composable
fun StartScanScreen(
    onBack: () -> Unit,
    onScanComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StartScanViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onScanComplete()
    }

    BaseScreen(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.how_to_scan_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = stringResource(R.string.cd_info),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        ) {
            ScanViewfinder(
                modifier = Modifier.weight(1f),
                onClick = viewModel::advance,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

            VoiceGuidanceCard(
                volume = state.voiceVolume,
                onVolumeChange = viewModel::updateVolume,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            StepIndicator(
                currentStep = state.currentStep,
                totalSteps = state.steps.size,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

            state.currentInstructions.forEach { instruction ->
                InstructionRow(
                    text = stringResource(instruction.textRes),
                    completed = instruction.completed,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
        }
    }
}

@Composable
private fun VoiceGuidanceCard(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(
                start = dimensionResource(R.dimen.spacer_m),
                end = dimensionResource(R.dimen.spacer_m),
                top = dimensionResource(R.dimen.spacer_m),
                bottom = dimensionResource(R.dimen.spacer_xxs),
            ),
    ) {
        Text(
            text = stringResource(R.string.scan_voice_guidance_auto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_voice),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium_small)),
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_3xs)))
            AppSlider(
                value = volume,
                onValueChange = onVolumeChange,
                background = MaterialTheme.colorScheme.background,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.scan_step_label))
                append(stringResource(R.string.space))
                append(currentStep.toString())
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primaryFixed)) {
                    append(stringResource(R.string.slash))
                    append(totalSteps.toString())
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xxs)),
        ) {
            repeat(totalSteps) { index ->
                val isFilled = index <= currentStep - 1
                val backgroundModifier = if (isFilled) Modifier.gradientBackground(
                    brush = Brush.verticalGradient(listOf(Blue300, Blue200)),
                    shape = CircleShape,
                )
                else Modifier.background(
                    color = MaterialTheme.colorScheme.secondaryFixed,
                    shape = CircleShape,
                )
                Box(
                    modifier = backgroundModifier
                        .weight(1f)
                        .height(dimensionResource(R.dimen.size_indicator_bigger))
                )
            }
        }
    }
}

@Composable
private fun InstructionRow(
    text: String,
    completed: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val circleSize = dimensionResource(R.dimen.icon_medium_small)
        if (completed) Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(Green),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium_small)),
            )
        } else Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .border(
                    width = dimensionResource(R.dimen.border_thin),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onBackground,
                )
        )

        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
