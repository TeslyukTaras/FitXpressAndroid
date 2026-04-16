package com.hexis.bi.ui.main.settings.editprofile

import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.ui.components.profile.HealthParameters
import com.hexis.bi.utils.cmToFeetAndInches
import com.hexis.bi.utils.cmToInches
import com.hexis.bi.utils.constants.ProfileConstants
import com.hexis.bi.utils.kgToLb
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
    override val isMetric: Boolean = true,
    val heightCm: Float = ProfileConstants.DEFAULT_HEIGHT_CM,
    val weightKg: Float = ProfileConstants.DEFAULT_WEIGHT_KG,
    val isGenderDropdownOpen: Boolean = false,
    val showDatePicker: Boolean = false,
) : HealthParameters {
    override val heightSliderValue: Float
        get() = if (isMetric) heightCm else heightCm.cmToInches()

    override val heightSliderRange: ClosedFloatingPointRange<Float>
        get() = if (isMetric) ProfileConstants.HEIGHT_CM_MIN..ProfileConstants.HEIGHT_CM_MAX
        else ProfileConstants.HEIGHT_IN_MIN..ProfileConstants.HEIGHT_IN_MAX

    override val heightDisplayValue: Int
        get() = heightCm.roundToInt()

    override val heightFeet: Int
        get() = heightCm.cmToFeetAndInches().first

    override val heightInches: Int
        get() = heightCm.cmToFeetAndInches().second.roundToInt()

    override val weightSliderValue: Float
        get() = if (isMetric) weightKg else weightKg.kgToLb()

    override val weightSliderRange: ClosedFloatingPointRange<Float>
        get() = if (isMetric) ProfileConstants.WEIGHT_KG_MIN..ProfileConstants.WEIGHT_KG_MAX
        else ProfileConstants.WEIGHT_LB_MIN..ProfileConstants.WEIGHT_LB_MAX

    override val weightDisplayValue: Int
        get() = if (isMetric) weightKg.roundToInt() else weightKg.kgToLb().roundToInt()
}
