package com.hexis.bi.ui.main.settings.editprofile

import android.app.Application
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.hexis.bi.data.user.UserProfile
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
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
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val dobString = profile.dateOfBirth?.let { dateFormat.format(it) }.orEmpty()
            val gender = GenderOption.entries.firstOrNull { it.name == profile.gender }
            _state.update {
                it.copy(
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    email = profile.email,
                    avatarUrl = profile.avatarUrl,
                    dateOfBirth = dobString,
                    gender = gender ?: it.gender,
                    isMetric = if (profile.unitSystem != null) profile.unitSystem == "Metric" else it.isMetric,
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
    fun updateHeight(value: Float) = _state.update { it.copy(heightCm = value) }
    fun updateWeight(value: Float) = _state.update { it.copy(weightKg = value) }

    fun showDatePicker() = _state.update { it.copy(showDatePicker = true) }
    fun hideDatePicker() = _state.update { it.copy(showDatePicker = false) }

    fun uploadAvatar(uri: Uri) = launch {
        val uid = firebaseAuth.currentUser?.uid ?: return@launch
        val ref = storage.reference.child("avatars/$uid/${System.currentTimeMillis()}.jpg")
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
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    val dob = dateFormat.runCatching { parse(dateOfBirth) }.getOrNull()
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
        heightIn = (heightCmInt / 2.54).roundToInt(),
        weightLb = (weightKgInt * 2.20462).roundToInt(),
        unitSystem = if (isMetric) "Metric" else "Imperial",
        dateOfBirth = dob,
    )
}
