package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.AppSlider
import com.hexis.bi.ui.components.AppSwitch
import com.hexis.bi.utils.constants.ActivityConstants
import java.text.NumberFormat
import java.util.Locale
import com.hexis.bi.ui.theme.NocturnePulseTheme

@Composable
fun ActivitySettingsDialogContent(
    stepsGoal: Int,
    showActiveCalories: Boolean,
    dataSource: String,
    onStepsGoalChange: (Int) -> Unit,
    onShowActiveCaloriesChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(R.dimen.padding_large),
                horizontal = dimensionResource(R.dimen.padding_medium),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
            text = stringResource(R.string.activity_settings_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.padding_small)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.activity_settings_data_source),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = dataSource,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.padding_small)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.activity_settings_steps_goal),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onBackground,
            )
            val unitText = stringResource(R.string.activity_unit_steps_full)
            val stepsText = stringResource(R.string.activity_settings_steps_goal_value, fmt.format(stepsGoal))
            Text(
                text = buildAnnotatedString {
                    append(stepsText)
                    val unitIndex = stepsText.indexOf(unitText)
                    if (unitIndex >= 0) {
                        addStyle(
                            SpanStyle(color = NocturnePulseTheme.extendedColors.gray200),
                            unitIndex,
                            stepsText.length,
                        )
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        AppSlider(
            value = stepsGoal.toFloat(),
            onValueChange = { raw ->
                val step = ActivityConstants.STEPS_GOAL_SLIDER_STEP
                val snapped = (raw / step).toInt() * step
                onStepsGoalChange(snapped)
            },
            valueRange = ActivityConstants.STEPS_GOAL_MIN.toFloat()..
                    ActivityConstants.STEPS_GOAL_MAX.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.padding_small)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.activity_settings_show_active_calories),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onBackground,
            )
            AppSwitch(
                checked = showActiveCalories,
                onCheckedChange = onShowActiveCaloriesChange,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.padding_small)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
        ) {
            AppOutlinedButton(
                text = stringResource(R.string.action_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            AppPrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = onSave,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
