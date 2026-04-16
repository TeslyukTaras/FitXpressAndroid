package com.hexis.bi.ui.auth.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.hexis.bi.ui.components.AppSlider
import com.hexis.bi.ui.components.AppTextField
import com.hexis.bi.ui.components.AppTopBar
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
        MeasurementSection(state = state, viewModel = viewModel)

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
    }
}

@Composable
private fun MeasurementSection(
    state: OnboardingState,
    viewModel: OnboardingViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
    ) {
        // Units toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = dimensionResource(R.dimen.spacer_l),
                    horizontal = dimensionResource(R.dimen.spacer_xs),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.edit_profile_units),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(dimensionResource(R.dimen.spacer_xxs)),
            ) {
                val selectedBgColor = MaterialTheme.colorScheme.surfaceVariant

                Text(
                    text = stringResource(R.string.edit_profile_metric),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isMetric) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (state.isMetric) selectedBgColor else MaterialTheme.colorScheme.background)
                        .clickable { viewModel.selectMetric() }
                        .padding(
                            horizontal = dimensionResource(R.dimen.spacer_s),
                            vertical = dimensionResource(R.dimen.spacer_xxs),
                        ),
                )

                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacer_xxs)))

                Text(
                    text = stringResource(R.string.edit_profile_imperial),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (!state.isMetric) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (!state.isMetric) selectedBgColor else MaterialTheme.colorScheme.background)
                        .clickable { viewModel.selectImperial() }
                        .padding(
                            horizontal = dimensionResource(R.dimen.spacer_s),
                            vertical = dimensionResource(R.dimen.spacer_xxs),
                        ),
                )
            }
        }

        // Height slider
        MeasurementSlider(
            label = stringResource(R.string.edit_profile_height),
            valueText = if (state.isMetric) stringResource(
                R.string.unit_height_cm,
                state.heightDisplayValue
            )
            else stringResource(R.string.unit_height_ft_in, state.heightFeet, state.heightInches),
            value = state.heightSliderValue,
            valueRange = state.heightSliderRange,
            onValueChange = viewModel::updateHeight,
        )

        // Weight slider
        MeasurementSlider(
            label = stringResource(R.string.edit_profile_weight),
            valueText = if (state.isMetric) stringResource(
                R.string.unit_weight_kg,
                state.weightDisplayValue
            )
            else stringResource(R.string.unit_weight_lb, state.weightDisplayValue),
            value = state.weightSliderValue,
            valueRange = state.weightSliderRange,
            onValueChange = viewModel::updateWeight,
        )
    }
}

@Composable
private fun MeasurementSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            )
        }
        AppSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
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
