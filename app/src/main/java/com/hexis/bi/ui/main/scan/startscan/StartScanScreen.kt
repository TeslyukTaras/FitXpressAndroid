package com.hexis.bi.ui.main.scan.startscan

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withResumed
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.AppSlider
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.scan.ScanPurpose
import com.hexis.bi.ui.main.scan.components.ScanChecklistSheet
import com.hexis.bi.ui.main.scan.processing.ScanAnalyzingContent
import com.hexis.bi.ui.main.scan.results.ResultsViewModel
import com.hexis.bi.ui.main.scan.results.content.PersonalizeResultsDialog
import com.hexis.bi.ui.main.scan.results.content.ScanResultsContent
import com.hexis.bi.ui.main.scan.results.isDisplayable
import com.hexis.bi.ui.main.scan.results.resultsActions
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.utils.gradientBackground
import com.look.camera.sdk.SdkActivity
import com.look.camera.sdk.data.LaunchOption
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

private const val BODY_SCAN_REVEAL_DURATION_MS = 300

private enum class ScanMode {
    Idle,
    BodyRunning,
    SuitAnalysis,
    SuitIntro;

    val isDark: Boolean get() = this != Idle
    val showsCloseIcon: Boolean get() = this == BodyRunning || this == SuitAnalysis
}

private fun scanMode(scanPurpose: ScanPurpose, state: StartScanState): ScanMode = when {
    scanPurpose == ScanPurpose.SuitSizeScan ->
        if (state.isProcessing || state.isComplete || state.scanErrorMessage != null) {
            ScanMode.SuitAnalysis
        } else {
            ScanMode.SuitIntro
        }

    state.isProcessing || state.isComplete -> ScanMode.BodyRunning
    else -> ScanMode.Idle
}

@Composable
fun StartScanScreen(
    onBack: () -> Unit,
    onScanComplete: () -> Unit,
    onShowHowToScan: () -> Unit,
    onOpenScanPreferences: () -> Unit,
    modifier: Modifier = Modifier,
    scanPurpose: ScanPurpose = ScanPurpose.BodyScan,
    viewModel: StartScanViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var bodyResultsReady by remember { mutableStateOf(false) }
    var bodyProgressComplete by remember { mutableStateOf(false) }
    var showChecklistSheet by remember(scanPurpose) {
        mutableStateOf(scanPurpose == ScanPurpose.SuitSizeScan)
    }

    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        Timber.d("camera result code=%d data=%s", result.resultCode, result.data)
        if (result.resultCode == Activity.RESULT_OK) {
            val frontUri = SdkActivity.getFrontPhotoUri(result.data)
            val sideUri = SdkActivity.getSidePhotoUri(result.data)
            Timber.d("extracted front=%s side=%s", frontUri, sideUri)
            if (frontUri != null && sideUri != null) viewModel.onPhotosReceived(frontUri, sideUri)
            else viewModel.onPhotoError()
        } else viewModel.onCameraCancelled()
    }

    LaunchedEffect(state.shouldLaunchCamera) {
        if (state.shouldLaunchCamera) {
            viewModel.onCameraLaunched()
            val intent = SdkActivity.start(context, LaunchOption.FRONT_AND_SIDE_ONLY)
            cameraLauncher.launch(intent)
        }
    }

    LaunchedEffect(scanPurpose) {
        viewModel.prepareForScan(scanPurpose)
        if (scanPurpose == ScanPurpose.BodyScan) viewModel.startCamera()
    }

    LaunchedEffect(state.isComplete, scanPurpose) {
        bodyResultsReady = false
        bodyProgressComplete = false
    }

    LaunchedEffect(state.shouldNavigateBack) {
        if (state.shouldNavigateBack) lifecycleOwner.lifecycle.withResumed { onBack() }
    }

    val mode = scanMode(scanPurpose, state)
    val showBodyResults = mode == ScanMode.BodyRunning &&
            state.isComplete && bodyProgressComplete && bodyResultsReady

    LaunchedEffect(showBodyResults) {
        if (showBodyResults) viewModel.onBodyResultsRevealed()
    }

    BaseScreen(
        modifier = modifier
            .then(if (mode.isDark) Modifier.screenBackground() else Modifier)
            .then(
                if (state.showPersonalizeResultsHint) {
                    Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                } else {
                    Modifier
                }
            ),
        containerColor = if (mode.isDark) Color.Transparent else MaterialTheme.colorScheme.background,
        error = error,
        onDismissError = { viewModel.onErrorDismissed() },
        topBar = {
            BaseTopBar(
                title = stringResource(
                    when (mode) {
                        ScanMode.BodyRunning -> R.string.scan_results_title
                        ScanMode.SuitAnalysis -> R.string.scan_size_results_title
                        ScanMode.SuitIntro -> R.string.scan_size_title
                        ScanMode.Idle -> R.string.scan_title
                    }
                ),
                background = if (mode.isDark) Color.Transparent else MaterialTheme.colorScheme.background,
                onBack = onBack,
                actions = {
                    IconButton(onClick = if (mode.showsCloseIcon) onBack else onShowHowToScan) {
                        Icon(
                            painter = painterResource(if (mode.showsCloseIcon) R.drawable.ic_cross else R.drawable.ic_info),
                            contentDescription = stringResource(
                                if (mode.showsCloseIcon) R.string.cd_close
                                else R.string.cd_info,
                            ),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                },
            )
        },
    ) {
        when (mode) {
            ScanMode.BodyRunning -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (state.isComplete) {
                        CompletedBodyScanResults(
                            onReadyChanged = { bodyResultsReady = it },
                            revealed = showBodyResults,
                        )
                    }
                    AnimatedVisibility(
                        visible = !showBodyResults,
                        enter = fadeIn(),
                        exit = slideOutHorizontally(tween(BODY_SCAN_REVEAL_DURATION_MS)) { -it } +
                                fadeOut(tween(BODY_SCAN_REVEAL_DURATION_MS)),
                    ) {
                        ScanAnalyzingContent(
                            modifier = Modifier.fillMaxSize(),
                            isComplete = state.isComplete,
                            onProgressFinished = { bodyProgressComplete = true },
                        )
                    }
                }
            }

            ScanMode.SuitAnalysis -> SuitSizeScanAnalysisScreen(
                isProcessing = state.isProcessing,
                isComplete = state.isComplete,
                errorMessage = state.scanErrorMessage,
                onResults = onScanComplete,
                onRescan = viewModel::startCamera,
            )

            ScanMode.SuitIntro -> SuitSizeScanHeaderSubtitle()

            ScanMode.Idle -> Box(modifier = Modifier.fillMaxSize())
        }
    }

    if (showChecklistSheet) {
        ScanChecklistSheet(
            onContinue = {
                showChecklistSheet = false
                viewModel.startCamera()
            },
            onDismiss = onBack,
        )
    }

    if (state.showPersonalizeResultsHint) {
        PersonalizeResultsDialog(
            onDismiss = viewModel::onPersonalizeResultsHintDismissed,
            onGoToSettings = {
                viewModel.onPersonalizeResultsHintDismissed()
                onOpenScanPreferences()
            },
        )
    }
}

@Composable
private fun CompletedBodyScanResults(
    onReadyChanged: (Boolean) -> Unit,
    revealed: Boolean,
    viewModel: ResultsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var avatarReady by remember(state.visual.latestModel3dUrl) { mutableStateOf(false) }
    val dataReady = state.isDisplayable
    val needsAvatarFrame = !state.visual.latestModel3dUrl.isNullOrBlank()
    val ready = dataReady && (!needsAvatarFrame || avatarReady)
    LaunchedEffect(ready) {
        onReadyChanged(ready)
    }
    // Offset (not alpha) keeps the avatar's GL surface rendering its first frame while hidden.
    val slideDistancePx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val reveal by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(BODY_SCAN_REVEAL_DURATION_MS),
        label = "bodyResultsReveal",
    )
    if (dataReady) {
        ScanResultsContent(
            state = state,
            actions = viewModel.resultsActions(),
            onAvatarReady = { avatarReady = true },
            modifier = Modifier.graphicsLayer { translationX = (1f - reveal) * slideDistancePx },
        )
    }
}

@Composable
private fun SuitSizeScanAnalysisScreen(
    isProcessing: Boolean,
    isComplete: Boolean,
    errorMessage: String?,
    onResults: () -> Unit,
    onRescan: () -> Unit,
) {
    val hasError = errorMessage != null
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        SuitSizeScanStatusCard(
            hasError = hasError,
            body = errorMessage ?: stringResource(R.string.scan_size_complete_body),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Image(
            painter = painterResource(R.drawable.img_buy_suit_complete),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        AppPrimaryButton(
            text = stringResource(
                when {
                    hasError -> R.string.action_rescan
                    else -> R.string.action_results
                },
            ),
            onClick = if (hasError) onRescan else onResults,
            enabled = hasError || isComplete,
            isLoading = isProcessing,
            trailingIcon = if (!hasError && isComplete) R.drawable.ic_arrow else null,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    }
}

@Composable
private fun SuitSizeScanStatusCard(
    hasError: Boolean,
    body: String,
) {
    BodyGlassCard {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m)),
        ) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.icon_medium))
                    .clip(CircleShape)
                    .background(if (hasError) MaterialTheme.colorScheme.error else NocturnePulseTheme.extendedColors.positive),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (hasError) R.drawable.ic_cross else R.drawable.ic_tick),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (hasError) R.string.scan_size_error_title
                        else R.string.scan_size_complete_title,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SuitSizeScanHeaderSubtitle() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.padding_large)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        Text(
            text = stringResource(R.string.scan_size_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
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
                    brush = Brush.verticalGradient(
                        listOf(
                            NocturnePulseTheme.extendedColors.blue300,
                            NocturnePulseTheme.extendedColors.blue200
                        )
                    ),
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
                .background(NocturnePulseTheme.extendedColors.green),
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
