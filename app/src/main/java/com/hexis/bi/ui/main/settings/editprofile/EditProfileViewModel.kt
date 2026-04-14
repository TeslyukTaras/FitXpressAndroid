package com.hexis.bi.ui.main.settings.editprofile

import android.app.Application
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.hexis.bi.data.user.UserProfile
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.cmToInches
import com.hexis.bi.utils.constants.MeasurementConstants
import com.hexis.bi.utils.constants.ProfileConstants
import com.hexis.bi.utils.formatDob
import com.hexis.bi.utils.inchesToCm
import com.hexis.bi.utils.isMetricUnitSystem
import com.hexis.bi.utils.kgToLb
import com.hexis.bi.utils.lbToKg
import com.hexis.bi.utils.parseDob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class EditProfileViewModel(
    application: Application,
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val storage: FirebaseStorage,
) : BaseViewModel(application, initialLoading = true) {

    private val _state = MutableStateFlow(EditProfileState())
    val state = _state.asStateFlow()

    fun loadUser() = launch {
        userRepository.getUser().onSuccess { profile ->
            val dobString = profile.dateOfBirth?.formatDob().orEmpty()
            val gender = GenderOption.entries.firstOrNull { it.name == profile.gender }
            _state.update {
                it.copy(
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    email = profile.email,
                    avatarUrl = profile.avatarUrl,
                    dateOfBirth = dobString,
                    gender = gender ?: it.gender,
                    isMetric = profile.unitSystem.isMetricUnitSystem(fallback = it.isMetric),
                    heightCm = profile.heightCm?.toFloat() ?: it.heightCm,
                    weightKg = profile.weightKg?.toFloat() ?: it.weightKg,
                )
            }
        }
    }

    fun updateFirstName(value: String) = _state.update { it.copy(firstName = value) }
    fun updateLastName(value: String) = _state.update { it.copy(lastName = value) }
    fun updateEmail(value: String) = _state.update { it.copy(email = value) }
    fun updateDateOfBirth(value: String) = _state.update { it.copy(dateOfBirth = value) }
    fun selectGender(gender: GenderOption) =
        _state.update { it.copy(gender = gender, isGenderDropdownOpen = false) }

    fun selectMetric() = _state.update { it.copy(isMetric = true) }
    fun selectImperial() = _state.update { it.copy(isMetric = false) }
    fun updateHeight(sliderValue: Float) = _state.update {
        it.copy(heightCm = if (it.isMetric) sliderValue else sliderValue.inchesToCm())
    }

    fun updateWeight(sliderValue: Float) = _state.update {
        it.copy(weightKg = if (it.isMetric) sliderValue else sliderValue.lbToKg())
    }

    fun showDatePicker() = _state.update { it.copy(showDatePicker = true) }
    fun hideDatePicker() = _state.update { it.copy(showDatePicker = false) }

    fun uploadAvatar(uri: Uri) = launch {
        val uid = firebaseAuth.currentUser?.uid ?: return@launch
        val ref =
            storage.reference.child("${ProfileConstants.AVATAR_STORAGE_DIR}/$uid/${System.currentTimeMillis()}.jpg")
        ref.putFile(uri).await()
        val url = ref.downloadUrl.await().toString()
        userRepository.updateAvatarUrl(url)
            .onSuccess { _state.update { it.copy(avatarUrl = url) } }
            .onFailure { setError(it.message) }
    }

    fun save() = launch {
        val uid = firebaseAuth.currentUser?.uid ?: return@launch
        userRepository.updateUser(_state.value.toUserProfile(uid))
            .onSuccess { emitEvent(EditProfileEvent.SaveSuccess) }
            .onFailure { setError(it.message) }
    }
}

private fun EditProfileState.toUserProfile(uid: String): UserProfile {
    val dob = dateOfBirth.parseDob()
    val heightCmInt = heightCm.toInt()
    val weightKgInt = weightKg.toInt()
    return UserProfile(
        uid = uid,
        firstName = firstName,
        lastName = lastName,
        email = email,
        avatarUrl = avatarUrl,
        gender = gender.name,
        heightCm = heightCmInt,
        weightKg = weightKgInt,
        heightIn = heightCmInt.toFloat().cmToInches().roundToInt(),
        weightLb = weightKgInt.toFloat().kgToLb().roundToInt(),
        unitSystem = if (isMetric) MeasurementConstants.UNIT_SYSTEM_METRIC else MeasurementConstants.UNIT_SYSTEM_IMPERIAL,
        dateOfBirth = dob,
    )
}
