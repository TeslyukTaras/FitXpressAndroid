package com.hexis.bi.ui.auth.onboarding

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppDatePicker
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.components.AppLogo
import com.hexis.bi.ui.components.my_suit.BuySuitDialogContent
import com.hexis.bi.ui.components.my_suit.SuitCareSheet
import com.hexis.bi.ui.components.my_suit.SuitConnectedBanner
import com.hexis.bi.ui.components.my_suit.SuitInfoRow
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.dark.DarkOutlinedButton
import com.hexis.bi.ui.dark.DarkOutlinedTextField
import com.hexis.bi.ui.dark.DarkPrimaryButton
import com.hexis.bi.ui.dark.DarkSlider
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.main.body.components.BodySegmentedToggleChip
import com.hexis.bi.ui.main.body.components.BodySegmentedToggleTrack
import com.hexis.bi.ui.theme.DarkSliderLabel
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.ui.theme.dark.Positive
import com.hexis.bi.utils.constants.AuthFlowConstants
import com.hexis.bi.utils.parseDob
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val PAGE_COUNT = 2
private const val PAGE_SCROLL_DURATION_MS = 350

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
    var showBuySuitDialog by remember { mutableStateOf(false) }
    val isLastPage = pagerState.currentPage == PAGE_COUNT - 1
    val goToPage: (Int) -> Unit = { page ->
        scope.launch {
            pagerState.animateScrollToPage(page, animationSpec = tween(PAGE_SCROLL_DURATION_MS))
        }
    }

    LightStatusBarIcons()

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

    DarkTheme {
        Box(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (state.showDatePicker || showBuySuitDialog)
                            Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                        else Modifier
                    )
                    .darkScreenBackground(),
            ) {
                Image(
                    painter = painterResource(R.drawable.img_auth_gradient),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    colorFilter = ColorFilter.tint(
                        MaterialTheme.colorScheme.primary.copy(alpha = AuthFlowConstants.GRADIENT_TINT_ALPHA),
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                )

                BaseScreen(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    isLoading = isLoading,
                    error = error,
                    onDismissError = viewModel::clearError,
                    topBar = {
                        if (pagerState.currentPage != 0) BaseTopBar(
                            title = stringResource(R.string.my_suit_title),
                            background = Color.Transparent,
                            onBack = { goToPage(0) },
                        )
                    },
                    bottomBar = {
                        OnboardingBottomBar(
                            currentPage = pagerState.currentPage,
                            pageCount = PAGE_COUNT,
                            isLastPage = isLastPage,
                            onSkip = if (!isLastPage) ({ viewModel.skip() }) else null,
                            onBack = if (isLastPage) ({ goToPage(0) }) else null,
                            onNext = { goToPage(pagerState.currentPage + 1) },
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
                            1 -> MySuitPage(
                                state = state,
                                viewModel = viewModel,
                                onBuyOne = { showBuySuitDialog = true },
                            )
                        }
                    }
                }
            }

            if (state.showDatePicker) AppDatePicker(
                state = datePickerState,
                onDismissRequest = viewModel::hideDatePicker,
                onSelect = viewModel::updateDateOfBirth,
            )

            if (state.showSuitCareSheet) SuitCareSheet(
                accepted = state.careInstructionsAccepted,
                onAcceptedChange = viewModel::setCareInstructionsAccepted,
                onContinue = viewModel::dismissSuitCareSheet,
                onDismiss = viewModel::dismissSuitCareSheet,
            )

            if (showBuySuitDialog) AppDialog(onDismiss = { showBuySuitDialog = false }) {
                BuySuitDialogContent(onBuySuit = { showBuySuitDialog = false })
            }
        }
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
            .navigationBarsPadding()
            .padding(
                bottom = dimensionResource(R.dimen.padding_auth_vertical),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else if (onBack != null) TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.action_back),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = stringResource(R.string.app_info_page_indicator, currentPage + 1, pageCount),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .imePadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.spacer_5xl)),
                contentAlignment = Alignment.Center,
            ) {
                AppLogo(tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_personal_info_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                Text(
                    text = stringResource(R.string.onboarding_personal_info_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.padding_auth_vertical)))

                // Date of birth
                Box(modifier = Modifier.fillMaxWidth()) {
                    DarkOutlinedTextField(
                        value = state.dateOfBirth,
                        onValueChange = {},
                        readOnly = true,
                        label = stringResource(R.string.label_date_of_birth),
                        placeholder = stringResource(R.string.placeholder_date_of_birth),
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                GenderField(
                    selected = state.gender,
                    onSelect = viewModel::selectGender,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                // Units + Height + Weight card
                PersonalInfoSection(
                    state = state,
                    onSelectMetric = viewModel::selectMetric,
                    onSelectImperial = viewModel::selectImperial,
                    onHeightChange = viewModel::updateHeight,
                    onWeightChange = viewModel::updateWeight,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))
            }
        }
    }
}

@Composable
private fun GenderField(
    selected: GenderOption,
    onSelect: (GenderOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        DarkOutlinedTextField(
            value = stringResource(selected.labelRes()),
            onValueChange = {},
            readOnly = true,
            label = stringResource(R.string.label_gender),
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_medium))
                        .rotate(if (expanded) 270f else 90f),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )

        DropdownMenu(
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            GenderOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PersonalInfoSection(
    state: OnboardingState,
    onSelectMetric: () -> Unit,
    onSelectImperial: () -> Unit,
    onHeightChange: (Float) -> Unit,
    onWeightChange: (Float) -> Unit,
) {
    Text(
        text = stringResource(R.string.edit_profile_personal_info),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.spacer_m)),
    )

    BodyGlassCard(
        contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_l)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_l)),
    ) {
        UnitsToggle(
            isMetric = state.isMetric,
            onSelectMetric = onSelectMetric,
            onSelectImperial = onSelectImperial,
        )
        MeasurementSlider(
            label = stringResource(R.string.edit_profile_height),
            valueText = if (state.isMetric) stringResource(
                R.string.unit_height_cm,
                state.heightDisplayValue
            )
            else stringResource(R.string.unit_height_ft_in, state.heightFeet, state.heightInches),
            value = state.heightSliderValue,
            valueRange = state.heightSliderRange,
            onValueChange = onHeightChange,
        )
        MeasurementSlider(
            label = stringResource(R.string.edit_profile_weight),
            valueText = if (state.isMetric) stringResource(
                R.string.unit_weight_kg,
                state.weightDisplayValue
            )
            else stringResource(R.string.unit_weight_lb, state.weightDisplayValue),
            value = state.weightSliderValue,
            valueRange = state.weightSliderRange,
            onValueChange = onWeightChange,
        )
    }
}

@Composable
private fun UnitsToggle(
    isMetric: Boolean,
    onSelectMetric: () -> Unit,
    onSelectImperial: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.edit_profile_units),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        BodySegmentedToggleTrack {
            BodySegmentedToggleChip(
                label = stringResource(R.string.edit_profile_metric),
                isSelected = isMetric,
                onClick = onSelectMetric,
                width = dimensionResource(R.dimen.edit_profile_units_toggle_chip_width),
            )
            Spacer(Modifier.size(dimensionResource(R.dimen.spacer_s)))
            BodySegmentedToggleChip(
                label = stringResource(R.string.edit_profile_imperial),
                isSelected = !isMetric,
                onClick = onSelectImperial,
                width = dimensionResource(R.dimen.edit_profile_units_toggle_chip_width),
            )
        }
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
                style = MaterialTheme.typography.bodyMedium,
                color = DarkSliderLabel,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = DarkSliderLabel,
            )
        }
        DarkSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Step 2: My Suit ──
@Composable
private fun MySuitPage(
    state: OnboardingState,
    viewModel: OnboardingViewModel,
    onBuyOne: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val frameHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(frameHeight)
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                if (state.isSuitConnected) SuitConnectedContent(
                    state = state,
                    onReconnect = viewModel::reconnectSuit,
                )
                else SuitDisconnectedContent(
                    suitIdInput = state.suitIdInput,
                    onSuitIdChange = viewModel::updateSuitIdInput,
                    onConnect = viewModel::connectSuit,
                    onBuyOne = onBuyOne,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SuitConnectedContent(
    state: OnboardingState,
    onReconnect: () -> Unit,
) {
    SuitConnectedBanner()

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

    Image(
        painter = painterResource(R.drawable.img_my_suit),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentScale = ContentScale.Fit,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))

    SuitInfoRow(
        label = stringResource(R.string.label_suit_id),
        value = state.connectedSuitId,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    SuitInfoRow(
        label = stringResource(R.string.my_suit_status),
        value = state.connectedStatus,
        valueColor = Positive,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

    DarkOutlinedButton(
        text = stringResource(R.string.action_reconnect_suit),
        onClick = onReconnect,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
}

@Composable
private fun ColumnScope.SuitDisconnectedContent(
    suitIdInput: String,
    onSuitIdChange: (String) -> Unit,
    onConnect: () -> Unit,
    onBuyOne: () -> Unit,
) {
    Text(
        text = stringResource(R.string.onboarding_suit_connect_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    Text(
        text = stringResource(R.string.onboarding_suit_connect_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    Image(
        painter = painterResource(R.drawable.img_my_suit),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentScale = ContentScale.Fit,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

    DarkOutlinedTextField(
        value = suitIdInput,
        onValueChange = onSuitIdChange,
        label = stringResource(R.string.label_suit_id),
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))

    Text(
        text = stringResource(R.string.onboarding_suit_id_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

    DarkPrimaryButton(
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onBuyOne),
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
}

private fun GenderOption.labelRes(): Int = when (this) {
    GenderOption.Male -> R.string.gender_male
    GenderOption.Female -> R.string.gender_female
    GenderOption.Other -> R.string.gender_other
}
