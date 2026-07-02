package com.hexis.bi.ui.main.scan.processing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.ScanAnalyzingPercentStyle
import kotlin.math.exp
import kotlin.math.roundToInt

private const val ANALYZING_BODY_ALPHA = 0.3f
private const val ANALYZING_LOADER_VERTICAL_BIAS = -0.25f
private const val ANALYZING_CREEP_CEILING = 0.9f
private const val ANALYZING_CREEP_TIME_CONSTANT_MS = 16_000f
private const val ANALYZING_FINISH_DURATION_MS = 3_000

/**
 * "Analyzing" state shown while a scan is being processed (awaiting 3DLOOK results): a dim body
 * wireframe with a circular fake-progress loader and a title/subtitle. Rendered as part of the host
 * screen (below its top bar); the host provides the dark background.
 */
@Composable
internal fun ScanAnalyzingContent(
    isComplete: Boolean,
    modifier: Modifier = Modifier,
    onProgressFinished: () -> Unit = {},
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(isComplete) {
        if (isComplete) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ANALYZING_FINISH_DURATION_MS,
                    easing = LinearOutSlowInEasing,
                ),
            )
            onProgressFinished()
        } else {
            val startNanos = withFrameNanos { it }
            while (true) {
                val elapsedMs = (withFrameNanos { it } - startNanos) / 1_000_000.0
                val decay = exp(-elapsedMs / ANALYZING_CREEP_TIME_CONSTANT_MS).toFloat()
                progress.snapTo(ANALYZING_CREEP_CEILING * (1f - decay))
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_4xl)))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Image(
                painter = painterResource(R.drawable.img_body_scan),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(ANALYZING_BODY_ALPHA),
            )
            AnalyzingLoader(
                progress = progress.value,
                modifier = Modifier.align(BiasAlignment(0f, ANALYZING_LOADER_VERTICAL_BIAS)),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        Text(
            text = stringResource(R.string.scan_analyzing_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.padding_large)),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Text(
            text = stringResource(R.string.scan_analyzing_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.padding_large)),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.scan_analyzing_bottom_spacer)))
    }
}

@Composable
private fun AnalyzingLoader(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val trackColor = NocturnePulseTheme.extendedColors.scanLoaderTrack
    val fillColor = MaterialTheme.colorScheme.primary
    val strokeWidth = dimensionResource(R.dimen.scan_analyzing_loader_stroke)

    Box(
        modifier = modifier.size(dimensionResource(R.dimen.scan_analyzing_loader_size)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcTopLeft = Offset(inset, inset)
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = fillColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = stroke,
            )
        }
        Text(
            text = "${(progress * 100).roundToInt()}%",
            style = ScanAnalyzingPercentStyle,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
