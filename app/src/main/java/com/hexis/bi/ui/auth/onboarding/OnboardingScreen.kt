package com.hexis.bi.ui.auth.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppButton
import com.hexis.bi.ui.components.AppDatePicker
import com.hexis.bi.ui.components.AppDropdown
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.components.AppTextField
import com.hexis.bi.ui.components.AppTopBar
import com.hexis.bi.ui.components.profile.HeathParametersSection
import com.hexis.bi.ui.components.my_suit.SuitConnectedBanner
import com.hexis.bi.ui.components.my_suit.SuitInfoRow
import com.hexis.bi.utils.parseDob
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val PAGE_COUNT = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGE_COUNT - 1

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingEvent.Finished -> onFinish()
            }
        }
    }

    val datePickerState = rememberDatePickerState()
    LaunchedEffect(state.dateOfBirth) {
        if (state.dateOfBirth.isNotEmpty()) {
            datePickerState.selectedDateMillis = state.dateOfBirth.parseDob()?.time
        }
    }

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.showDatePicker)
                        Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                ),
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            topBar = {
                if (pagerState.currentPage == 0) AppTopBar()
                else BaseTopBar(
                    title = stringResource(R.string.my_suit_title),
                    onBack = { scope.launch { pagerState.animateScrollToPage(0) } },
                )
            },
            bottomBar = {
                OnboardingBottomBar(
                    currentPage = pagerState.currentPage,
                    pageCount = PAGE_COUNT,
                    isLastPage = isLastPage,
                    onSkip = if (!isLastPage) ({ viewModel.skip() }) else null,
                    onBack = if (isLastPage) ({
                        scope.launch { pagerState.animateScrollToPage(0) }
                    }) else null,
                    onNext = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    onFinish = viewModel::finish,
                )
            },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
            ) { page ->
                when (page) {
                    0 -> PersonalInfoPage(state = state, viewModel = viewModel)
                    1 -> MySuitPage(state = state, viewModel = viewModel)
                }
            }
        }

        if (state.showDatePicker) AppDatePicker(
            state = datePickerState,
            onDismissRequest = viewModel::hideDatePicker,
            onSelect = viewModel::updateDateOfBirth,
        )
    }
}

@Composable
private fun OnboardingBottomBar(
    currentPage: Int,
    pageCount: Int,
    isLastPage: Boolean,
    onSkip: (() -> Unit)?,
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = dimensionResource(R.dimen.spacer_xl),
                start = dimensionResource(R.dimen.padding_medium),
                end = dimensionResource(R.dimen.padding_small),
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onSkip != null) TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.action_skip),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        else if (onBack != null) TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.action_back),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Text(
            text = stringResource(R.string.app_info_page_indicator, currentPage + 1, pageCount),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primaryFixed,
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

// ── Step 1: Personal Information ──

@Composable
private fun PersonalInfoPage(
    state: OnboardingState,
    viewModel: OnboardingViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Text(
            text = stringResource(R.string.onboarding_personal_info_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        Text(
            text = stringResource(R.string.onboarding_personal_info_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_6xl)))

        // Date of birth
        Box(modifier = Modifier.fillMaxWidth()) {
            AppTextField(
                value = state.dateOfBirth,
                onValueChange = {},
                readOnly = true,
                label = stringResource(R.string.label_date_of_birth),
                placeholder = stringResource(R.string.placeholder_date_of_birth),
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = viewModel::showDatePicker),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        // Gender
        AppDropdown(
            options = GenderOption.entries,
            selectedOption = state.gender,
            onOptionSelected = { viewModel.selectGender(it) },
            label = stringResource(R.string.label_gender),
            optionLabel = { option ->
                stringResource(
                    when (option) {
                        GenderOption.Male -> R.string.gender_male
                        GenderOption.Female -> R.string.gender_female
                        GenderOption.Other -> R.string.gender_other
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        // Units + Height + Weight card
        HeathParametersSection(
            params = state,
            onSelectMetric = viewModel::selectMetric,
            onSelectImperial = viewModel::selectImperial,
            onHeightChange = viewModel::updateHeight,
            onWeightChange = viewModel::updateWeight,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
    }
}

// ── Step 2: My Suit ──

/**
 * Measures the full available height once (before the IME opens), then uses a
 * scrollable column with a fixed-height image so the image doesn't shrink when
 * the keyboard appears.
 */
@Composable
private fun MySuitPage(
    state: OnboardingState,
    viewModel: OnboardingViewModel,
) {
    var imageHeight by remember { mutableStateOf(Dp.Unspecified) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Capture the full height once; ignore subsequent shrinks from IME.
        if (imageHeight == Dp.Unspecified) {
            imageHeight = maxHeight * 0.55f
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            if (state.isSuitConnected) SuitConnectedContent(
                state = state,
                imageHeight = imageHeight,
                onReconnect = viewModel::reconnectSuit,
            )
            else SuitDisconnectedContent(
                suitIdInput = state.suitIdInput,
                imageHeight = imageHeight,
                onSuitIdChange = viewModel::updateSuitIdInput,
                onConnect = viewModel::connectSuit,
            )
        }
    }
}

@Composable
private fun ColumnScope.SuitConnectedContent(
    state: OnboardingState,
    imageHeight: Dp,
    onReconnect: () -> Unit,
) {
    SuitConnectedBanner()

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

    Image(
        painter = painterResource(R.drawable.img_my_suit),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight),
        contentScale = ContentScale.Fit,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    SuitInfoRow(
        label = stringResource(R.string.label_suit_id),
        value = state.connectedSuitId,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    SuitInfoRow(
        label = stringResource(R.string.my_suit_status),
        value = state.connectedStatus,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    Spacer(Modifier.weight(1f))

    AppOutlinedButton(
        text = stringResource(R.string.action_reconnect_suit),
        onClick = onReconnect,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.SuitDisconnectedContent(
    suitIdInput: String,
    imageHeight: Dp,
    onSuitIdChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    Text(
        text = stringResource(R.string.onboarding_suit_connect_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

    Text(
        text = stringResource(R.string.onboarding_suit_connect_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    Image(
        painter = painterResource(R.drawable.img_my_suit),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight),
        contentScale = ContentScale.Fit,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

    AppTextField(
        value = suitIdInput,
        onValueChange = onSuitIdChange,
        label = stringResource(R.string.label_suit_id),
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

    Text(
        text = stringResource(R.string.onboarding_suit_id_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    Spacer(Modifier.weight(1f))

    AppButton(
        text = stringResource(R.string.action_connect),
        onClick = onConnect,
        enabled = suitIdInput.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

    val noSuitText = buildAnnotatedString {
        append(stringResource(R.string.onboarding_no_suit))
        append(" ")
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        ) { append(stringResource(R.string.onboarding_buy_one)) }
    }
    Text(
        text = noSuitText,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.secondary,
    )
}
