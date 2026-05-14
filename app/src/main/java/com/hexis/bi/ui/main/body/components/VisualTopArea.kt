package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.main.body.VisualState
import com.hexis.bi.ui.main.scan.results.MetricAvatarPreview
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
) {
    Box(modifier = modifier) {
        // The 3D model runs up to the tabs as the backdrop; the scan-date label and the
        // part list are drawn after it so they stay on top.
        BodyModelPreview(
            state = state,
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

@Composable
private fun BodyModelPreview(
    state: VisualState,
    modifier: Modifier = Modifier,
) {
    val modelUrl = state.latestModel3dUrl

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .modelBlur(blurEnabled = state.selectedBodyPart != BodyMeasurementRegion.FullBody),
        contentAlignment = Alignment.Center,
    ) {
        if (modelUrl.isNullOrBlank()) ModelPlaceholder(modifier = Modifier.fillMaxSize())
        else MetricAvatarPreview(
            modelUrl = modelUrl,
            onInteractionChanged = {},
            modifier = Modifier.fillMaxSize(),
            showSkinAreas = false,
            drawBackground = false,
            touchRotationEnabled = true,
            useGradientBackground = false,
            framingRegion = state.selectedBodyPart,
        )
    }
}

@Composable
private fun ModelPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.body_visual_model_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
