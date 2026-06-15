package com.hexis.bi.ui.main.buysuit.orderdetails

import android.content.ClipData
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseBottomSheet
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.dark.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsSheet(
    details: OrderDetailsUi,
    onDismiss: () -> Unit,
    onEditAddress: () -> Unit = {},
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    BaseBottomSheet(
        title = stringResource(R.string.order_details_title),
        onDismiss = onDismiss,
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.reference,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
                Text(
                    text = stringResource(
                        if (details.referenceIsTracking) R.string.order_details_tracking_label
                        else R.string.order_details_number_label
                    ),
                    style = TitleDimTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText("", details.reference))
                    )
                }
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = stringResource(
                        if (details.referenceIsTracking) R.string.cd_copy_tracking_number
                        else R.string.cd_copy_order_number
                    ),
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        Row {
            Text(
                text = stringResource(R.string.home_suit_order_eta_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
            Text(
                text = details.eta ?: stringResource(R.string.home_suit_order_eta_estimating),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = dimensionResource(R.dimen.border_hairline),
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(dimensionResource(R.dimen.spacer_m)),
        ) {
            details.steps.forEachIndexed { index, step ->
                TimelineStep(
                    step = step,
                    isFirst = index == 0,
                    isLast = index == details.steps.lastIndex,
                    prevReached = details.steps.getOrNull(index - 1)?.reached == true,
                    nextReached = details.steps.getOrNull(index + 1)?.reached == true,
                )
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.order_details_address_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
                Text(
                    text = details.address,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (details.addressChangePending) {
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xs)))
                    Text(
                        text = stringResource(R.string.order_details_address_change_pending),
                        style = TitleDimTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (details.canEditAddress) {
                IconButton(onClick = onEditAddress) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.cd_edit_address),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                    )
                }
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

        Text(
            text = stringResource(R.string.order_details_got_it),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onDismiss)
                .padding(dimensionResource(R.dimen.spacer_xs)),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
    }
}

@Composable
private fun TimelineStep(
    step: OrderTimelineStepUi,
    isFirst: Boolean,
    isLast: Boolean,
    prevReached: Boolean,
    nextReached: Boolean,
) {
    val dotSize = dimensionResource(R.dimen.timeline_dot_size)
    val connectorWidth = dimensionResource(R.dimen.timeline_connector_width)
    val dash = dimensionResource(R.dimen.timeline_dash)
    val mutedLine = MaterialTheme.colorScheme.outline
    val activeLine = MaterialTheme.colorScheme.primary
    val dotColor = if (step.reached) activeLine else TextSecondary
    // A connector segment is active (blue) only when both endpoints it joins are reached.
    val lineAbove = if (prevReached && step.reached) activeLine else mutedLine
    val lineBelow = if (step.reached && nextReached) activeLine else mutedLine

    fun DrawScope.dashedSegment(color: Color, startY: Float, endY: Float) {
        if (endY <= startY) return
        val centerX = size.width / 2f
        drawLine(
            color = color,
            start = Offset(centerX, startY),
            end = Offset(centerX, endY),
            strokeWidth = connectorWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx()), 0f),
        )
    }

    Column {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(dotSize)
                    .fillMaxHeight(),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val dotCenterY = if (isFirst) dotSize.toPx() / 2f else size.height / 2f
                    // Split at the dot so each half can take the colour of the connector it belongs to.
                    if (!isFirst) dashedSegment(lineAbove, 0f, dotCenterY)
                    if (!isLast) dashedSegment(lineBelow, dotCenterY, size.height)
                }
                Spacer(
                    Modifier
                        .size(dotSize)
                        .align(if (isFirst) Alignment.TopCenter else Alignment.Center)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

            Column {
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (step.timestamp != null) {
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
                    Text(
                        text = step.timestamp,
                        style = TitleDimTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!isLast) Box(
            modifier = Modifier
                .width(dotSize)
                .height(dimensionResource(R.dimen.spacer_l)),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                dashedSegment(lineBelow, 0f, size.height)
            }
        }
    }
}
