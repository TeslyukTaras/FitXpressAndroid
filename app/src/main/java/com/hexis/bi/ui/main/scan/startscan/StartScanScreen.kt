package com.hexis.bi.ui.main.scan.startscan

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.utils.gradientBackground
import com.look.camera.sdk.SdkActivity
import com.look.camera.sdk.data.LaunchOption
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@Composable
fun StartScanScreen(
    onBack: () -> Unit,
    onScanComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StartScanViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

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

    LaunchedEffect(state.isComplete) { if (state.isComplete) onScanComplete() }

    LaunchedEffect(state.shouldNavigateBack) { if (state.shouldNavigateBack) onBack() }

    BaseScreen(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        isLoading = isLoading,
        error = error,
        onDismissError = { viewModel.onErrorDismissed() },
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.scan_title),
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
        loadingContent = {
            ScanProgressIndicator(progress = state.scanProgress)
        },
    ) {
        Box(modifier = Modifier.fillMaxSize())
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
