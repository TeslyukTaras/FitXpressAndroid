package com.hexis.bi.ui.main.settings.editprofile

import com.hexis.bi.ui.base.UiEvent

enum class GenderOption { Male, Female, Other }

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
    val heightCm: Float = 179f,
    val weightKg: Float = 80f,
    val isGenderDropdownOpen: Boolean = false,
    val showDatePicker: Boolean = false,
)
