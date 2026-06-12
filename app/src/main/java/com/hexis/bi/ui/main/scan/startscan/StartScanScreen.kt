package com.hexis.bi.ui.main.scan.startscan

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.data.scan.ScanProgress
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppSlider
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.dark.DarkPrimaryButton
import com.hexis.bi.ui.main.scan.ScanPurpose
import com.hexis.bi.ui.main.scan.components.ScanChecklistSheet
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.dark.Positive
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.utils.gradientBackground
import com.look.camera.sdk.SdkActivity
import com.look.camera.sdk.data.LaunchOption
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@Composable
fun StartScanScreen(
    onBack: () -> Unit,
    onScanComplete: () -> Unit,
    onShowHowToScan: () -> Unit,
    modifier: Modifier = Modifier,
    scanPurpose: ScanPurpose = ScanPurpose.BodyScan,
    viewModel: StartScanViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
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
        if (state.isComplete && scanPurpose == ScanPurpose.BodyScan) onScanComplete()
    }

    LaunchedEffect(state.shouldNavigateBack) { if (state.shouldNavigateBack) onBack() }

    val isSuitSizeScan = scanPurpose == ScanPurpose.SuitSizeScan
    val showSuitSizeAnalysis = isSuitSizeScan &&
            (state.isProcessing || state.isComplete || state.scanErrorMessage != null)

    BaseScreen(
        modifier = modifier.then(if (isSuitSizeScan) Modifier.darkScreenBackground() else Modifier),
        containerColor = if (isSuitSizeScan) Color.Transparent else MaterialTheme.colorScheme.background,
        isLoading = isLoading,
        error = error,
        onDismissError = { viewModel.onErrorDismissed() },
        topBar = {
            BaseTopBar(
                title = stringResource(
                    when {
                        showSuitSizeAnalysis -> R.string.scan_size_results_title
                        isSuitSizeScan -> R.string.scan_size_title
                        else -> R.string.scan_title
                    }
                ),
                background = if (isSuitSizeScan) Color.Transparent else MaterialTheme.colorScheme.background,
                onBack = onBack,
                actions = {
                    IconButton(onClick = if (showSuitSizeAnalysis) onBack else onShowHowToScan) {
                        Icon(
                            painter = painterResource(if (showSuitSizeAnalysis) R.drawable.ic_cross else R.drawable.ic_info),
                            contentDescription = stringResource(
                                if (showSuitSizeAnalysis) R.string.cd_close
                                else R.string.cd_info,
                            ),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                },
            )
        },
        loadingContent = {
            ScanProgressIndicator(progress = state.scanProgress)
        },
    ) {
        if (showSuitSizeAnalysis) {
            SuitSizeScanAnalysisScreen(
                isProcessing = state.isProcessing,
                isComplete = state.isComplete,
                errorMessage = state.scanErrorMessage,
                onResults = onScanComplete,
                onRescan = viewModel::startCamera,
            )
        } else if (isSuitSizeScan) {
            SuitSizeScanHeaderSubtitle()
        } else {
            Box(modifier = Modifier.fillMaxSize())
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

        DarkPrimaryButton(
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
                    .background(if (hasError) MaterialTheme.colorScheme.error else Positive),
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
private fun BoxScope.ScanProgressIndicator(
    progress: ScanProgress?,
) {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        Text(
            text = when (progress) {
                is ScanProgress.Submitting -> stringResource(R.string.scan_progress_submitting)
                is ScanProgress.Processing -> stringResource(R.string.scan_progress_processing)
                else -> ""
            },
            style = MaterialTheme.typography.bodyLarge,
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
