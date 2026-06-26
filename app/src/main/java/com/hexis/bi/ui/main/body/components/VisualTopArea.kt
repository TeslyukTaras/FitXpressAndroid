package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.main.body.BodyVisualColorModel
import com.hexis.bi.ui.main.body.BodyVisualMode
import com.hexis.bi.ui.main.body.VisualState
import com.hexis.bi.ui.avatar.MetricAvatarLoading
import com.hexis.bi.ui.avatar.MetricAvatarPreview
import com.hexis.bi.ui.avatar.MetricAvatarStatusText
import java.text.SimpleDateFormat

@Composable
internal fun VisualTopArea(
    state: VisualState,
    selectedScanLabel: String,
    dateFormatter: SimpleDateFormat,
    modelAreaHeight: Dp,
    onBodyPartSelected: (BodyMeasurementRegion) -> Unit,
    onScanSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showScanSelector: Boolean = true,
    onAvatarReady: () -> Unit = {},
) {
    Box(modifier = modifier) {
        BodyModelPreview(
            state = state,
            onAvatarReady = onAvatarReady,
            modifier = Modifier
                .fillMaxWidth()
                .height(modelAreaHeight)
                .align(Alignment.TopCenter)
        )

        BodyPartScrollList(
            selected = state.selectedBodyPart,
            onSelect = onBodyPartSelected,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = dimensionResource(R.dimen.spacer_4xl),
                    bottom = dimensionResource(R.dimen.spacer_2xl)
                )
                .height(modelAreaHeight),
        )

        if (showScanSelector) {
            VisualScanDateDropdown(
                label = selectedScanLabel,
                selectedTimestamp = state.latestScanTimestamp,
                options = state.scanOptions,
                dateFormatter = dateFormatter,
                onScanSelected = onScanSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.padding_medium),
                        vertical = dimensionResource(R.dimen.spacer_3xs)
                    ),
            )
        }
    }
}

@Composable
private fun BodyModelPreview(
    state: VisualState,
    onAvatarReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coloredModel = state.colorModel as? BodyVisualColorModel.Ready
    val showBaseModel = state.mode == BodyVisualMode.Base
    val showColorModel = state.mode == BodyVisualMode.Color && coloredModel != null

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .modelBlur(blurEnabled = state.selectedBodyPart != BodyMeasurementRegion.FullBody),
        contentAlignment = Alignment.Center,
    ) {
        ModelPreview(
            modelUrl = state.latestModel3dUrl,
            selectedBodyPart = state.selectedBodyPart,
            visible = showBaseModel,
            onAvatarReady = onAvatarReady,
        )
        coloredModel?.let { model ->
            ModelPreview(
                modelUrl = model.coloredModelUrl,
                useModelVertexColors = true,
                selectedBodyPart = state.selectedBodyPart,
                loadingMessageRes = R.string.body_visual_color_loading,
                visible = showColorModel,
                onAvatarReady = onAvatarReady,
            )
        }
        if (state.mode == BodyVisualMode.Color && coloredModel == null) {
            when (state.colorModel) {
                BodyVisualColorModel.Idle,
                BodyVisualColorModel.Loading -> MetricAvatarLoading(
                    messageRes = R.string.body_visual_color_loading,
                    modifier = Modifier.fillMaxSize(),
                )
                BodyVisualColorModel.Unavailable -> MetricAvatarStatusText(
                    messageRes = R.string.body_visual_color_unavailable,
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
                )
                BodyVisualColorModel.Error -> MetricAvatarStatusText(
                    messageRes = R.string.body_visual_color_error,
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
                )
                is BodyVisualColorModel.Ready -> Unit
            }
        }
    }
}

@Composable
private fun BoxScope.ModelPreview(
    modelUrl: String?,
    selectedBodyPart: BodyMeasurementRegion,
    useModelVertexColors: Boolean = false,
    loadingMessageRes: Int = R.string.scan_results_avatar_loading,
    visible: Boolean = true,
    onAvatarReady: () -> Unit = {},
) {
    val modifier = Modifier
        .fillMaxSize()
        .zIndex(if (visible) 1f else 0f)
        .alpha(if (visible) 1f else 0f)
    if (modelUrl.isNullOrBlank()) ModelPlaceholder(modifier = modifier)
    else MetricAvatarPreview(
        modelUrl = modelUrl,
        useModelVertexColors = useModelVertexColors,
        onInteractionChanged = {},
        modifier = modifier,
        showSkinAreas = false,
        drawBackground = false,
        touchRotationEnabled = visible,
        useGradientBackground = false,
        framingRegion = selectedBodyPart,
        loadingMessageRes = loadingMessageRes,
        onAvatarReady = if (visible) onAvatarReady else null,
    )
}

@Composable
private fun ModelPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        MetricAvatarStatusText(
            messageRes = R.string.body_visual_model_placeholder,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
        )
    }
}
