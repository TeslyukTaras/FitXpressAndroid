package com.hexis.bi.ui.main.scan.results

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppSwitch
import com.hexis.bi.ui.components.AppTabSelector
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.Red100
import com.hexis.bi.utils.cmToFeetAndInches
import com.hexis.bi.utils.cmToInches
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private const val RESULTS_PREVIEW_EXIT_FADE_MS = 200
private const val RESULTS_PREVIEW_EXIT_SETTLE_MS = 24

@Composable
fun ResultsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isModelInteracting by remember { mutableStateOf(false) }
    var pendingExitAfterPreviewHidden by remember { mutableStateOf(false) }

    val previewAlpha by animateFloatAsState(
        targetValue = if (pendingExitAfterPreviewHidden) 0f else 1f,
        animationSpec = tween(durationMillis = RESULTS_PREVIEW_EXIT_FADE_MS),
        label = "resultsPreviewAlpha",
    )

    LaunchedEffect(pendingExitAfterPreviewHidden) {
        if (!pendingExitAfterPreviewHidden) return@LaunchedEffect
        delay(RESULTS_PREVIEW_EXIT_FADE_MS.toLong() + RESULTS_PREVIEW_EXIT_SETTLE_MS)
        onBack()
    }

    fun requestBack() {
        if (pendingExitAfterPreviewHidden) return
        pendingExitAfterPreviewHidden = true
    }

    BackHandler(enabled = true) { requestBack() }

    BaseScreen(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.scan_results_title),
                onBack = { requestBack() },
                background = MaterialTheme.colorScheme.surfaceVariant,
            )
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(
                state = rememberScrollState(),
                enabled = !isModelInteracting,
            )
        ) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

            AppTabSelector(
                tabs = ResultsTab.entries,
                selectedTab = state.selectedTab,
                onTabSelected = viewModel::selectTab,
                tabLabel = { stringResource(it.labelRes) },
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            ScanResultsPreviewSection(
                selectedTab = state.selectedTab,
                model3dUrl = state.model3dUrl,
                previousModel3dUrl = state.previousModel3dUrl,
                isPreviewSectionLoading = state.isPreviewSectionLoading,
                showSkinAreas = state.showSkinAreas,
                onModelInteractionChanged = { isModelInteracting = it },
                measurements = state.measurements,
                isMetric = state.isMetric,
                modifier = Modifier.graphicsLayer { alpha = previewAlpha },
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            if (state.selectedTab == ResultsTab.Visual) {
                ColorAnalysisCard(
                    modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                    enabled = state.colorAnalysisEnabled,
                    onToggle = viewModel::toggleColorAnalysis,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            }

            MeasurementsCard(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                measurements = state.measurements,
                isMetric = state.isMetric,
                todayDate = state.todayDate,
                previousDate = state.previousDate,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        }
    }
}

@Composable
private fun ScanResultsPreviewSection(
    selectedTab: ResultsTab,
    model3dUrl: String?,
    previousModel3dUrl: String?,
    isPreviewSectionLoading: Boolean,
    showSkinAreas: Boolean,
    onModelInteractionChanged: (Boolean) -> Unit,
    measurements: List<MeasurementRow>,
    isMetric: Boolean,
    modifier: Modifier = Modifier,
) {
    var visualTransform by remember { mutableStateOf<VisualAvatarTransform?>(null) }

    var avatarMeshReady by remember(model3dUrl) { mutableStateOf(false) }

    var measurementGuide by remember(model3dUrl) { mutableStateOf<MetricAvatarMeasurementGuide?>(null) }

    LaunchedEffect(model3dUrl) {
        measurementGuide = null
        visualTransform = null
        avatarMeshReady = false
    }

    LaunchedEffect(selectedTab, isPreviewSectionLoading) {
        if (selectedTab != ResultsTab.Visual || isPreviewSectionLoading) {
            visualTransform = null
        }
    }

    val previewModifier = Modifier
        .fillMaxWidth()
        .height(dimensionResource(R.dimen.scan_results_preview_height))
    Box(
        modifier = modifier
            .then(previewModifier)
            .metricAvatarPreviewGradientBackground(),
    ) {
        when {
            isPreviewSectionLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
                        Text(
                            text = stringResource(R.string.scan_results_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            else -> when (selectedTab) {
                ResultsTab.Compare -> {
                    CompareModelsPanel(
                        currentModelUrl = model3dUrl,
                        previousModelUrl = previousModel3dUrl,
                        showSkinAreas = showSkinAreas,
                        onInteractionChanged = onModelInteractionChanged,
                    )
                }
                ResultsTab.Visual,
                ResultsTab.Posture,
                -> {
                    if (!model3dUrl.isNullOrBlank()) {
                        val profileYaw = when (selectedTab) {
                            ResultsTab.Posture -> MetricAvatarSideProfileYawDegrees
                            else -> 0f
                        }
                        Box(Modifier.fillMaxSize()) {
                            MetricAvatarPreview(
                                modelUrl = model3dUrl,
                                showSkinAreas = showSkinAreas,
                                onInteractionChanged = onModelInteractionChanged,
                                modifier = Modifier.fillMaxSize(),
                                useGradientBackground = false,
                                initialYawDegrees = profileYaw,
                                leaderSegments = null,
                                onMeasurementGuideLoaded = { measurementGuide = it },
                                onAvatarReady = { avatarMeshReady = true },
                                onVisualTransformChanged =
                                    if (selectedTab == ResultsTab.Visual) {
                                        { yaw, pitch, w, h ->
                                            visualTransform =
                                                VisualAvatarTransform(yaw, pitch, w, h)
                                        }
                                    } else {
                                        null
                                    },
                            )
                            if (selectedTab == ResultsTab.Visual &&
                                !isPreviewSectionLoading &&
                                visualTransform != null &&
                                avatarMeshReady
                            ) {
                                MeasurementLeaderOverlay(
                                    measurements = measurements,
                                    isMetric = isMetric,
                                    transform = visualTransform,
                                    measurementGuide = measurementGuide,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInteropFilter(onTouchEvent = { false }),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareModelsPanel(
    currentModelUrl: String?,
    previousModelUrl: String?,
    showSkinAreas: Boolean,
    onInteractionChanged: (Boolean) -> Unit,
) {
    val compareRotationLink = remember(currentModelUrl, previousModelUrl) { CompareRotationLink() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.scan_results_compare_current),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(R.dimen.spacer_2xs)),
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            currentModelUrl.isNullOrBlank() -> Unit
                            else -> {
                                key(currentModelUrl) {
                                    MetricAvatarPreview(
                                        modelUrl = currentModelUrl,
                                        showSkinAreas = showSkinAreas,
                                        onInteractionChanged = onInteractionChanged,
                                        modifier = Modifier.fillMaxSize(),
                                        useGradientBackground = false,
                                        compareRotationLink = compareRotationLink,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.secondaryFixed,
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.scan_results_compare_previous),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(R.dimen.spacer_2xs)),
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            previousModelUrl.isNullOrBlank() -> {
                                GradientCenteredLabel(messageRes = R.string.scan_results_compare_no_previous)
                            }
                            else -> {
                                key(previousModelUrl) {
                                    MetricAvatarPreview(
                                        modelUrl = previousModelUrl,
                                        showSkinAreas = showSkinAreas,
                                        onInteractionChanged = onInteractionChanged,
                                        modifier = Modifier.fillMaxSize(),
                                        useGradientBackground = false,
                                        compareRotationLink = compareRotationLink,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradientCenteredLabel(messageRes: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
        )
    }
}

@Composable
private fun ColorAnalysisCard(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.spacer_m)),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.scan_results_color_analysis),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            Text(
                text = stringResource(R.string.scan_results_color_analysis_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        AppSwitch(
            checked = enabled,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun MeasurementsCard(
    measurements: List<MeasurementRow>,
    isMetric: Boolean,
    todayDate: String,
    previousDate: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.spacer_m),
                    top = dimensionResource(R.dimen.spacer_xxs),
                    end = dimensionResource(R.dimen.spacer_xxs),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.scan_results_measurements_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.cd_info),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = dimensionResource(R.dimen.spacer_m)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.scan_results_body_part),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primaryFixed,
                modifier = Modifier.weight(1.2f),
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.secondaryFixed,
            )
            HeaderCell(
                title = stringResource(R.string.scan_results_today),
                date = todayDate,
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.secondaryFixed,
            )
            HeaderCell(
                title = stringResource(R.string.scan_results_previous),
                date = previousDate ?: stringResource(R.string.scan_results_no_value),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_m)),
            color = MaterialTheme.colorScheme.secondaryFixed
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        measurements.forEachIndexed { index, row ->
            MeasurementTableRow(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_m)),
                row = row,
                isMetric = isMetric,
            )
            if (index < measurements.lastIndex) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_m)),
                    color = MaterialTheme.colorScheme.secondaryFixed
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
            }
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
    }
}

@Composable
private fun HeaderCell(
    title: String,
    date: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primaryFixed,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
        Text(
            text = date,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun MeasurementTableRow(
    modifier: Modifier = Modifier,
    row: MeasurementRow,
    isMetric: Boolean,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(row.bodyPartRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1.2f)
                .padding(start = dimensionResource(R.dimen.padding_medium)),
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.secondaryFixed,
        )
        ValueCell(value = row.today, isMetric = isMetric, modifier = Modifier.weight(1f))
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.secondaryFixed,
        )
        if (row.previous != null) ValueCell(
            value = row.previous,
            colorDelta = false,
            isMetric = isMetric,
            modifier = Modifier.weight(1f)
        )
        else EmptyValueCell(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EmptyValueCell(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.scan_results_no_value),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun ValueCell(
    modifier: Modifier = Modifier,
    value: MeasurementValue,
    colorDelta: Boolean = true,
    isMetric: Boolean,
) {
    val deltaColor = if (colorDelta) when (value.change) {
        MeasurementChange.Positive -> Green
        MeasurementChange.Negative -> Red100
        null -> MaterialTheme.colorScheme.primaryFixed
    } else MaterialTheme.colorScheme.primaryFixed

    val unit = stringResource(if (isMetric) R.string.unit_cm else R.string.unit_in)
    val deltaValue = if (isMetric) value.deltaCm else value.deltaCm.cmToInches()
    val deltaText = when {
        value.deltaCm > 0 -> stringResource(R.string.format_delta_up, deltaValue, unit)
        value.deltaCm < 0 -> stringResource(R.string.format_delta_down, deltaValue, unit)
        else -> stringResource(R.string.format_delta_neutral, deltaValue, unit)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isMetric) {
            val valueText = stringResource(R.string.format_value_decimal, value.cm)
            val unitText = stringResource(R.string.unit_cm)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                        append(valueText)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                        append(unitText)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val (feet, inches) = value.cm.cmToFeetAndInches()
            val feetText = stringResource(R.string.format_value_int, feet)
            val ftUnit = stringResource(R.string.unit_ft)
            val inchesText = stringResource(R.string.format_value_decimal, inches)
            val inUnit = stringResource(R.string.unit_in)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                        append(feetText)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                        append(ftUnit)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                        append(inchesText)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                        append(inUnit)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
        Text(
            text = deltaText,
            style = MaterialTheme.typography.labelMedium,
            color = deltaColor,
        )
    }
}
