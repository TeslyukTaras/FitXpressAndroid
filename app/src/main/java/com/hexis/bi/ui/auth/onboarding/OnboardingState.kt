package com.hexis.bi.ui.auth.onboarding

import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.ui.base.HealthParameters
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.utils.calculateAge
import com.hexis.bi.utils.cmToInches
import com.hexis.bi.utils.cmToRoundedFeetAndInches
import com.hexis.bi.utils.constants.ProfileConstants
import com.hexis.bi.utils.kgToLb
import com.hexis.bi.utils.parseDob
import kotlin.math.roundToInt

sealed interface OnboardingEvent : UiEvent {
    data object Finished : OnboardingEvent

    data object BuySuitScan : OnboardingEvent
}

data class OnboardingState(
    // Personal info (step 1)
    val dateOfBirth: String = "",
    val gender: GenderOption = GenderOption.Male,
    override val isMetric: Boolean = true,
    val heightCm: Float = ProfileConstants.DEFAULT_HEIGHT_CM,
    val weightKg: Float = ProfileConstants.DEFAULT_WEIGHT_KG,
    val showDatePicker: Boolean = false,

    // Suit (step 2)
    val suitIdInput: String = "",
    val isSuitConnected: Boolean = false,
    val connectedSuitId: String = "",
    val connectedStatus: String = "",
    val showSuitCareSheet: Boolean = false,
    val careInstructionsAccepted: Boolean = false,
) : HealthParameters {
    private val age: Int?
        get() = dateOfBirth.parseDob()?.calculateAge()

    val isDobUnderage: Boolean
        get() = age?.let { it < ProfileConstants.MIN_AGE_YEARS } == true

    val isPersonalInfoValid: Boolean
        get() = age?.let { it >= ProfileConstants.MIN_AGE_YEARS } == true

    override val heightSliderValue: Float
        get() = if (isMetric) heightCm else heightCm.cmToInches()

    override val heightSliderRange: ClosedFloatingPointRange<Float>
        get() = if (isMetric) ProfileConstants.HEIGHT_CM_MIN..ProfileConstants.HEIGHT_CM_MAX
        else ProfileConstants.HEIGHT_IN_MIN..ProfileConstants.HEIGHT_IN_MAX

    override val heightDisplayValue: Int
        get() = heightCm.roundToInt()

    override val heightFeet: Int
        get() = heightCm.cmToRoundedFeetAndInches().first

    override val heightInches: Int
        get() = heightCm.cmToRoundedFeetAndInches().second

    override val weightSliderValue: Float
        get() = if (isMetric) weightKg else weightKg.kgToLb()

    override val weightSliderRange: ClosedFloatingPointRange<Float>
        get() = if (isMetric) ProfileConstants.WEIGHT_KG_MIN..ProfileConstants.WEIGHT_KG_MAX
        else ProfileConstants.WEIGHT_LB_MIN..ProfileConstants.WEIGHT_LB_MAX

    override val weightDisplayValue: Int
        get() = if (isMetric) weightKg.roundToInt() else weightKg.kgToLb().roundToInt()
}
