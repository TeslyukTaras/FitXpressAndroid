package com.hexis.bi.ui.main.settings.editprofile

import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.utils.constants.ProfileConstants
import kotlin.math.roundToInt

sealed interface EditProfileEvent : UiEvent {
    data object SaveSuccess : EditProfileEvent
}

data class EditProfileState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val dateOfBirth: String = "",
    val gender: GenderOption = GenderOption.Male,
    val isMetric: Boolean = true,
    val heightCm: Float = ProfileConstants.DEFAULT_HEIGHT_CM,
    val weightKg: Float = ProfileConstants.DEFAULT_WEIGHT_KG,
    val isGenderDropdownOpen: Boolean = false,
    val showDatePicker: Boolean = false,
) {
    val heightSliderValue: Float
        get() = if (isMetric) heightCm else heightCm / ProfileConstants.CM_TO_IN

    val heightSliderRange: ClosedFloatingPointRange<Float>
        get() = if (isMetric) ProfileConstants.HEIGHT_CM_MIN..ProfileConstants.HEIGHT_CM_MAX
        else ProfileConstants.HEIGHT_IN_MIN..ProfileConstants.HEIGHT_IN_MAX

    val heightDisplayValue: Int
        get() = heightCm.roundToInt()

    val heightFeet: Int
        get() = ProfileConstants.cmToFeetAndInches(heightCm).first

    val heightInches: Int
        get() = ProfileConstants.cmToFeetAndInches(heightCm).second.roundToInt()

    val weightSliderValue: Float
        get() = if (isMetric) weightKg else weightKg * ProfileConstants.KG_TO_LB

    val weightSliderRange: ClosedFloatingPointRange<Float>
        get() = if (isMetric) ProfileConstants.WEIGHT_KG_MIN..ProfileConstants.WEIGHT_KG_MAX
        else ProfileConstants.WEIGHT_LB_MIN..ProfileConstants.WEIGHT_LB_MAX

    val weightDisplayValue: Int
        get() = if (isMetric) weightKg.roundToInt()
        else (weightKg * ProfileConstants.KG_TO_LB).roundToInt()
}
