package com.hexis.bi.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.utils.gradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = Blue300,
        activeTrackColor = Blue300,
        activeTickColor = Blue300,
        inactiveTrackColor = MaterialTheme.colorScheme.background,
        inactiveTickColor = Blue300,
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
                    .shadow(
                        elevation = dimensionResource(R.dimen.elevation_box),
                        shape = CircleShape
                    )
                    .gradientBackground(
                        brush = Brush.verticalGradient(listOf(Blue300, Blue200)),
                        shape = MaterialTheme.shapes.small,
                    )
                    .border(
                        width = dimensionResource(R.dimen.border_thumb),
                        color = MaterialTheme.colorScheme.background,
                        shape = CircleShape
                    )
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(dimensionResource(R.dimen.slider_track_height)),
                colors = sliderColors,
                thumbTrackGapSize = 0.dp,
                drawStopIndicator = null
            )
        }
    )
}