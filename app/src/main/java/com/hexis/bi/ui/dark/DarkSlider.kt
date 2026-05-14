package com.hexis.bi.ui.dark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.ui.theme.DarkSliderActiveTrack
import com.hexis.bi.ui.theme.DarkSliderInactiveTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = DarkSliderActiveTrack,
        activeTrackColor = DarkSliderActiveTrack,
        activeTickColor = DarkSliderActiveTrack,
        inactiveTrackColor = DarkSliderInactiveTrack,
        inactiveTickColor = DarkSliderInactiveTrack,
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        colors = sliderColors,
        thumb = {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.icon_small))
                    .clip(CircleShape)
                    .background(DarkSliderActiveTrack),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(dimensionResource(R.dimen.dark_slider_track_height)),
                colors = sliderColors,
                thumbTrackGapSize = 0.dp,
                drawStopIndicator = null,
            )
        },
    )
}
