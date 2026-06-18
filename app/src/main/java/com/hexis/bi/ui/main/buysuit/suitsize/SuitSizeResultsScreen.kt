package com.hexis.bi.ui.main.buysuit.suitsize

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.order.SuitSize
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.AppSlider
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.ui.main.body.components.BodySegmentedToggleChip
import com.hexis.bi.ui.main.body.components.BodySegmentedToggleTrack
import com.hexis.bi.utils.cmToInches
import com.hexis.bi.utils.cmToRoundedFeetAndInches
import com.hexis.bi.utils.constants.ProfileConstants
import com.hexis.bi.utils.inchesToCm
import com.hexis.bi.utils.kgToLb
import com.hexis.bi.utils.lbToKg
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt
import com.hexis.bi.ui.theme.NocturnePulseTheme

@Composable
fun SuitSizeResultsScreen(
    onBack: () -> Unit,
    onProceedToOrder: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SuitSizeResultsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    BaseScreen(
        modifier = modifier.screenBackground(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        error = error,
        onDismissError = viewModel::clearError,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.scan_size_results_title),
                background = androidx.compose.ui.graphics.Color.Transparent,
                onBack = onBack,
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cross),
                            contentDescription = stringResource(R.string.cd_close),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                },
            )
        },
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            else -> SuitSizeResultsContent(
                state = state,
                onSelectMetric = viewModel::selectMetric,
                onSelectImperial = viewModel::selectImperial,
                onHeightChange = viewModel::updateHeight,
                onWeightChange = viewModel::updateWeight,
                onProceedToOrder = {
                    viewModel.confirmSelection()
                    onProceedToOrder()
                },
            )
        }
    }
}

@Composable
private fun SuitSizeResultsContent(
    state: SuitSizeResultsState,
    onSelectMetric: () -> Unit,
    onSelectImperial: () -> Unit,
    onHeightChange: (Float) -> Unit,
    onWeightChange: (Float) -> Unit,
    onProceedToOrder: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        BodyGlassCard(
            contentPadding = PaddingValues(
                start = dimensionResource(R.dimen.spacer_xs),
                top = dimensionResource(R.dimen.spacer_l),
                end = dimensionResource(R.dimen.spacer_xs),
                bottom = dimensionResource(R.dimen.spacer_l),
            ),
        ) {
            UnitsToggle(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
                isMetric = state.isMetric,
                onSelectMetric = onSelectMetric,
                onSelectImperial = onSelectImperial,
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_l)))
            MeasurementSliderRow(
                label = stringResource(R.string.edit_profile_height),
                valueText = formatHeight(state.heightCm, state.isMetric),
                value = if (state.isMetric) state.heightCm else state.heightCm.cmToInches(),
                valueRange = if (state.isMetric) {
                    ProfileConstants.HEIGHT_CM_MIN..ProfileConstants.HEIGHT_CM_MAX
                } else {
                    ProfileConstants.HEIGHT_IN_MIN..ProfileConstants.HEIGHT_IN_MAX
                },
                onValueChange = { value ->
                    onHeightChange(if (state.isMetric) value else value.inchesToCm())
                },
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            MeasurementSliderRow(
                label = stringResource(R.string.edit_profile_weight),
                valueText = formatWeight(state.weightKg, state.isMetric),
                value = if (state.isMetric) state.weightKg else state.weightKg.kgToLb(),
                valueRange = if (state.isMetric) {
                    ProfileConstants.WEIGHT_KG_MIN..ProfileConstants.WEIGHT_KG_MAX
                } else {
                    ProfileConstants.WEIGHT_LB_MIN..ProfileConstants.WEIGHT_LB_MAX
                },
                onValueChange = { value ->
                    onWeightChange(if (state.isMetric) value else value.lbToKg())
                },
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            Text(
                text = stringResource(R.string.suit_size_confirm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SuitImage(
                resId = R.drawable.img_man_costume,
                modifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(R.dimen.suit_size_image_height)),
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            SuitImage(
                resId = R.drawable.img_woman_costume,
                modifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(R.dimen.suit_size_image_height)),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        SuitFeatureRow(stringResource(R.string.suit_size_feature_lightweight))
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        SuitFeatureRow(stringResource(R.string.suit_size_feature_tracking))

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
        AppHorizontalGradientDivider()
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.suit_size_label))
                append(" ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append(stringResource(state.suitSize.labelRes()))
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))

        AppPrimaryButton(
            text = stringResource(R.string.suit_size_proceed_to_order),
            onClick = onProceedToOrder,
            enabled = state.canProceedToOrder,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
    }
}

@StringRes
private fun SuitSize.labelRes(): Int = when (this) {
    SuitSize.X_SMALL -> R.string.suit_size_x_small
    SuitSize.SMALL -> R.string.suit_size_small
    SuitSize.MEDIUM -> R.string.suit_size_medium
    SuitSize.LARGE -> R.string.suit_size_large
    SuitSize.X_LARGE -> R.string.suit_size_x_large
    SuitSize.XX_LARGE -> R.string.suit_size_xx_large
}

@Composable
private fun UnitsToggle(
    modifier: Modifier,
    isMetric: Boolean,
    onSelectMetric: () -> Unit,
    onSelectImperial: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
private fun MeasurementSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        AppSlider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SuitImage(
    resId: Int,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
private fun SuitFeatureRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.icon_medium))
                .clip(CircleShape)
                .border(dimensionResource(R.dimen.border_thin), NocturnePulseTheme.extendedColors.positive, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = null,
                tint = NocturnePulseTheme.extendedColors.positive,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
        }
        Spacer(Modifier.size(dimensionResource(R.dimen.spacer_s)))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun formatHeight(heightCm: Float, isMetric: Boolean): String =
    if (isMetric) {
        "${heightCm.roundToInt()} cm"
    } else {
        val (feet, inches) = heightCm.cmToRoundedFeetAndInches()
        "$feet ft $inches in"
    }

private fun formatWeight(weightKg: Float, isMetric: Boolean): String =
    if (isMetric) {
        "${weightKg.roundToInt()} kg"
    } else {
        "${weightKg.kgToLb().roundToInt()} lb"
    }
