package com.hexis.bi.ui.main.notifications

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.data.notification.InboxItem
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.dark.AppHorizontalGradientDivider
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.theme.TitleHighlightTextStyle
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.NotificationUi
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    DarkTheme {
        BaseScreen(
            modifier = modifier
                .fillMaxSize()
                .darkScreenBackground(),
            containerColor = Color.Transparent,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            topBar = {
                Column(Modifier.fillMaxWidth()) {
                    BaseTopBar(
                        title = stringResource(R.string.screen_notifications),
                        onBack = onBack,
                        background = Color.Transparent,
                    )
                    if (items.isNotEmpty()) Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(R.dimen.spacer_xxs)),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { viewModel.markAllRead() }) {
                            Text(
                                text = stringResource(R.string.notifications_mark_all_read),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            },
        ) {
            if (items.isEmpty()) Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_large)),
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                Text(
                    text = stringResource(R.string.notifications_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            else LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, row -> row.id },
                ) { index, item ->
                    if (index > 0) AppHorizontalGradientDivider()
                    NotificationListRow(
                        item = item,
                        onClick = { if (!item.isRead) viewModel.markRead(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationListRow(
    item: InboxItem,
    onClick: () -> Unit,
) {
    val timeLabel = shortRelativeTimeLabel(item.createdAtEpochMillis)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = dimensionResource(R.dimen.spacer_m)),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xl)))
        Box(Modifier.fillMaxHeight()) {
            Text(
                modifier = Modifier.align(Alignment.TopEnd),
                text = timeLabel,
                style = TitleHighlightTextStyle,
                color = DarkTheme.extendedColors.timestamp,
                textAlign = TextAlign.End,
            )
            if (!item.isRead) UnreadIndicator(modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun UnreadIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(dimensionResource(R.dimen.size_indicator_bigger))
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}

/** Compact relative time, e.g. "7m ago", "2h ago", "3d ago"; "Just now" under a minute. */
@Composable
private fun shortRelativeTimeLabel(epochMillis: Long): String {
    val justNow = stringResource(R.string.notifications_time_just_now)
    val diff = System.currentTimeMillis() - epochMillis
    if (epochMillis <= NotificationUi.RELATIVE_TIME_EPOCH_UNSET ||
        diff < DateUtils.MINUTE_IN_MILLIS
    ) return justNow
    val short = when {
        diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS}m"
        diff < DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.HOUR_IN_MILLIS}h"
        diff < DateUtils.WEEK_IN_MILLIS -> "${diff / DateUtils.DAY_IN_MILLIS}d"
        diff < DateUtils.DAY_IN_MILLIS * 365 -> "${diff / DateUtils.WEEK_IN_MILLIS}w"
        else -> "${diff / (DateUtils.DAY_IN_MILLIS * 365)}y"
    }
    return stringResource(R.string.notifications_time_ago, short)
}
