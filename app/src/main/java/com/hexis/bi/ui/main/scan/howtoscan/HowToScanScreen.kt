package com.hexis.bi.ui.main.scan.howtoscan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.ui.theme.NocturnePulseTheme

/**
 * Single-screen "Prepare Your Scan" tutorial with [howToScanSteps] states the user pages
 * through. Reached from Settings → "How scanning works" and the Scan screen's info button.
 */
@Composable
fun HowToScanScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LightStatusBarIcons()

    BaseScreen(
        modifier = modifier
            .fillMaxSize()
            .screenBackground(),
        containerColor = Color.Transparent,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.how_to_scan_title),
                background = Color.Transparent,
                onBack = onBack,
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cross),
                            contentDescription = stringResource(R.string.cd_close),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                },
            )
        },
    ) {
        var currentStep by rememberSaveable { mutableIntStateOf(0) }
        val totalSteps = howToScanSteps.size
        HowToScanContent(
            steps = howToScanSteps,
            currentStep = currentStep,
            totalSteps = totalSteps,
            onSkip = onBack,
            onNext = { if (currentStep < totalSteps - 1) currentStep++ else onBack() },
            modifier = modifier,
        )
    }
}

@Composable
private fun HowToScanContent(
    steps: List<HowToScanStep>,
    currentStep: Int,
    totalSteps: Int,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLastStep = currentStep == totalSteps - 1
    val currentStepContent = steps[currentStep]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(currentStepContent.subtitleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

            StepIndicator(currentStep = currentStep + 1, totalSteps = totalSteps)

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            AnimatedContent(
                targetState = currentStep,
                modifier = Modifier
                    .fillMaxWidth(),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    slideInHorizontally { width -> width * direction } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width * direction } + fadeOut()
                },
                label = "HowToScanStepSlide",
            ) { targetStep ->
                val step = steps[targetStep]
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(step.headerRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

                    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs))) {
                        step.instructionsRes.forEach { textRes ->
                            InstructionRow(text = stringResource(textRes))
                        }
                    }

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                    Image(
                        painter = painterResource(step.imageRes),
                        contentDescription = stringResource(R.string.cd_how_to_scan_illustration),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1029f / 1275f)
                            .clip(MaterialTheme.shapes.large)
                    )

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
                }
            }
        }

        ScanFooter(
            currentStep = currentStep + 1,
            totalSteps = totalSteps,
            isLastStep = isLastStep,
            onSkip = onSkip,
            onNext = onNext,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
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
                append(stringResource(R.string.slash))
                append(totalSteps.toString())
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xxs)),
        ) {
            repeat(totalSteps) { index ->
                val fillProgress by animateFloatAsState(
                    targetValue = if (index <= currentStep - 1) 1f else 0f,
                    animationSpec = tween(durationMillis = 260),
                    label = "HowToScanStepIndicatorFill",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(dimensionResource(R.dimen.size_indicator_bigger))
                        .clip(CircleShape)
                        .background(NocturnePulseTheme.extendedColors.stepIndicatorTrack),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillProgress)
                            .height(dimensionResource(R.dimen.size_indicator_bigger))
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.icon_medium))
                .clip(CircleShape)
                .border(dimensionResource(R.dimen.border_thin), NocturnePulseTheme.extendedColors.positive, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = null,
                tint = NocturnePulseTheme.extendedColors.positive,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
        }

        Spacer(Modifier.size(dimensionResource(R.dimen.spacer_s)))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ScanFooter(
    currentStep: Int,
    totalSteps: Int,
    isLastStep: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val footerActionHeight = dimensionResource(R.dimen.icon_normalized)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(footerActionHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .height(footerActionHeight)
                .clickable(onClick = onSkip)
                .padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.action_skip),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Truly centered in the row, independent of the Skip / Next widths.
        Text(
            text = stringResource(R.string.how_to_scan_step_counter, currentStep, totalSteps),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(footerActionHeight)
                .clickable(onClick = onNext)
                .padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (isLastStep) R.string.action_finish else R.string.action_next
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(dimensionResource(R.dimen.spacer_xxs)))
            Icon(
                painter = painterResource(R.drawable.ic_arrow),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium_small)),
            )
        }
    }
}
