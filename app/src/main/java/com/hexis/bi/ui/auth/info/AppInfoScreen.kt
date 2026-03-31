package com.hexis.bi.ui.auth.info

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.components.AppTopBar
import kotlinx.coroutines.launch

data class AppInfoPage(
    val title: String,
    val subtitle: String,
    val emphasis: String,
    val imageRes: Int,
)

@Composable
fun AppInfoScreen(
    modifier: Modifier = Modifier,
    onFinish: () -> Unit = {},
) {
    val pages = rememberAppInfoPages()
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    BaseScreen(
        modifier = modifier,
        topBar = {
            AppTopBar(
                onBack = if (pagerState.currentPage > 0) ({
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) else null,
            )
        },
        bottomBar = {
            AppInfoBottomBar(
                currentPage = pagerState.currentPage,
                pageCount = pages.size,
                isLastPage = isLastPage,
                onSkip = onFinish,
                onNext = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                onFinish = onFinish,
            )
        },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            AppInfoPageContent(page = pages[index], pageIndex = index)
        }
    }
}

@Composable
private fun AppInfoBottomBar(
    currentPage: Int,
    pageCount: Int,
    isLastPage: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = dimensionResource(R.dimen.spacer_3xl),
                start = dimensionResource(R.dimen.padding_medium),
                end = dimensionResource(R.dimen.padding_medium),
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.action_skip),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Text(
            text = stringResource(R.string.app_info_page_indicator, currentPage + 1, pageCount),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        TextButton(onClick = if (isLastPage) onFinish else onNext) {
            Text(
                text = if (isLastPage) stringResource(R.string.action_start) else stringResource(R.string.action_next),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = dimensionResource(R.dimen.spacer_xs))
                    .size(dimensionResource(R.dimen.icon_medium)),
            )
        }
    }
}


@Composable
private fun AppInfoPageContent(page: AppInfoPage, pageIndex: Int) {
    // Page 1: subtitle=gray/normal, emphasis=black/medium
    // Page 2: subtitle=black/medium, emphasis=gray/normal
    val subtitleColor =
        if (pageIndex == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground
    val subtitleStyle =
        if (pageIndex == 0) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelLarge
    val emphasisColor =
        if (pageIndex == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary
    val emphasisStyle =
        if (pageIndex == 0) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = page.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = page.subtitle,
            style = subtitleStyle,
            color = subtitleColor,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = page.emphasis,
            style = emphasisStyle,
            color = emphasisColor,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        Image(
            painter = painterResource(page.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_3xl)))
    }
}

@Composable
private fun rememberAppInfoPages(): List<AppInfoPage> = listOf(
    AppInfoPage(
        title = stringResource(R.string.app_info_page1_title),
        subtitle = stringResource(R.string.app_info_page1_subtitle),
        emphasis = stringResource(R.string.app_info_page1_emphasis),
        imageRes = R.drawable.img_app_info1,
    ),
    AppInfoPage(
        title = stringResource(R.string.app_info_page2_title),
        subtitle = stringResource(R.string.app_info_page2_subtitle),
        emphasis = stringResource(R.string.app_info_page2_emphasis),
        imageRes = R.drawable.img_app_info2,
    ),
)