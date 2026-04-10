package com.hexis.bi.ui.main.scan.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.ui.theme.ShadowColor

private const val SCAN_ANIMATION_DURATION_MS = 5000

@Composable
fun ScanViewfinder(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScanTransition")
    val scanPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SCAN_ANIMATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanLineAnimation"
    )

    val cornerWidth = dimensionResource(R.dimen.scan_corner_stroke_width)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawScannerOverlay(
                color = MaterialTheme.colorScheme.onBackground,
                shadowColor = ShadowColor,
                cornerLength = dimensionResource(R.dimen.scan_corner_length),
                cornerStrokeWidth = cornerWidth,
                scannerStrokeWidth = dimensionResource(R.dimen.scanner_line_stroke_width),
                cornerRadius = dimensionResource(R.dimen.scan_corner_radius),
                scanPositionProvider = { scanPosition }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.img_scan_preview),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(cornerWidth / 2)
        )

        IconButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(dimensionResource(R.dimen.spacer_m)),
            onClick = { onClick?.invoke() }) {
            Icon(
                painter = painterResource(R.drawable.ic_change_camera),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
        }
    }
}

private fun Modifier.drawScannerOverlay(
    color: Color,
    shadowColor: Color,
    cornerLength: Dp,
    cornerStrokeWidth: Dp,
    scannerStrokeWidth: Dp,
    cornerRadius: Dp,
    scanPositionProvider: () -> Float
) = this.drawWithCache {
    val lengthPx = cornerLength.toPx()
    val cornerStrokePx = cornerStrokeWidth.toPx()
    val scannerStrokePx = scannerStrokeWidth.toPx()
    val radiusPx = cornerRadius.toPx()

    val inset = cornerStrokePx / 2f
    val width = size.width
    val height = size.height

    val path = Path().apply {
        // top-left
        moveTo(inset, lengthPx)
        lineTo(inset, inset + radiusPx)
        quadraticTo(x1 = inset, y1 = inset, x2 = inset + radiusPx, y2 = inset)
        lineTo(lengthPx, inset)

        // top-right
        moveTo(width - lengthPx, inset)
        lineTo(width - inset - radiusPx, inset)
        quadraticTo(x1 = width - inset, y1 = inset, x2 = width - inset, y2 = inset + radiusPx)
        lineTo(width - inset, lengthPx)

        // bottom-right
        moveTo(width - inset, height - lengthPx)
        lineTo(width - inset, height - inset - radiusPx)
        quadraticTo(
            x1 = width - inset, y1 = height - inset,
            x2 = width - inset - radiusPx, y2 = height - inset
        )
        lineTo(width - lengthPx, height - inset)

        // bottom-left
        moveTo(lengthPx, height - inset)
        lineTo(inset + radiusPx, height - inset)
        quadraticTo(x1 = inset, y1 = height - inset, x2 = inset, y2 = height - inset - radiusPx)
        lineTo(inset, height - lengthPx)
    }

    onDrawWithContent {
        drawContent()

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = cornerStrokePx, cap = StrokeCap.Round)
        )

        val currentPositionFraction = scanPositionProvider()
        val lineY = height * currentPositionFraction
        val shadowTopY = lineY + scannerStrokePx

        drawRect(
            color = shadowColor,
            topLeft = Offset(x = 0f, y = shadowTopY),
            size = Size(width = width, height = maxOf(0f, height - shadowTopY))
        )

        drawRect(
            color = color,
            topLeft = Offset(x = 0f, y = lineY),
            size = Size(width = width, height = scannerStrokePx)
        )
    }
}
