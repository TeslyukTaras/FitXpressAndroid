package com.hexis.bi.ui.main.settings.editprofile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
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
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

class EditProfileViewModel(
    application: Application,
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val storage: FirebaseStorage,
) : BaseViewModel(application, initialLoading = true) {

    private val _state = MutableStateFlow(EditProfileState())
    val state = _state.asStateFlow()

    override fun onInitialize() {
        loadUser()
    }

    private fun loadUser() = launch {
        userRepository.getUser().onSuccess { profile ->
            val dobString = profile.dateOfBirth?.formatDob().orEmpty()
            val gender = GenderOption.entries.firstOrNull { it.name == profile.gender }
            _state.update {
                it.copy(
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    email = profile.email,
                    imageUrl = profile.imageUrl,
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
        val ref = storage.reference.child(
            "${ProfileConstants.AVATAR_STORAGE_DIR}/$uid/${ProfileConstants.PROFILE_IMAGE_FILE_NAME}"
        )
        val metadata = StorageMetadata.Builder()
            .setContentType(ProfileConstants.PROFILE_IMAGE_CONTENT_TYPE)
            .build()
        val bytes = prepareAvatarUploadBytes(uri)
        ref.putBytes(bytes, metadata).await()
        val url = ref.downloadUrl.await().toString()
        userRepository.updateImageUrl(url)
            .onSuccess { _state.update { it.copy(imageUrl = url) } }
            .onFailure { setError(it.message) }
    }

    fun save() = launch {
        if (!_state.value.canSave) return@launch
        val uid = firebaseAuth.currentUser?.uid ?: return@launch
        userRepository.updateUser(_state.value.toUserProfile(uid))
            .onSuccess { emitEvent(EditProfileEvent.SaveSuccess) }
            .onFailure { setError(it.message) }
    }

    private fun prepareAvatarUploadBytes(uri: Uri): ByteArray {
        val bitmap = decodeBitmap(uri)
        val scaled = bitmap.scaleToFit(ProfileConstants.PROFILE_IMAGE_MAX_DIMENSION_PX)
        if (scaled !== bitmap) bitmap.recycle()

        var quality = ProfileConstants.PROFILE_IMAGE_JPEG_QUALITY
        var bytes = scaled.compressJpeg(quality)
        while (
            bytes.size > ProfileConstants.PROFILE_IMAGE_MAX_BYTES &&
            quality > ProfileConstants.PROFILE_IMAGE_MIN_JPEG_QUALITY
        ) {
            quality -= ProfileConstants.PROFILE_IMAGE_QUALITY_STEP
            bytes = scaled.compressJpeg(quality)
        }
        scaled.recycle()
        return bytes
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(appContext.contentResolver, uri)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }
}

private fun Bitmap.scaleToFit(maxDimension: Int): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= maxDimension) return this

    val scale = maxDimension.toFloat() / largestSide.toFloat()
    val scaledWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val scaledHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
}

private fun Bitmap.compressJpeg(quality: Int): ByteArray {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, output)
    return output.toByteArray()
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
        imageUrl = imageUrl,
        gender = gender.name,
        heightCm = heightCmInt,
        weightKg = weightKgInt,
        heightIn = heightCmInt.toFloat().cmToInches().roundToInt(),
        weightLb = weightKgInt.toFloat().kgToLb().roundToInt(),
        unitSystem = if (isMetric) MeasurementConstants.UNIT_SYSTEM_METRIC else MeasurementConstants.UNIT_SYSTEM_IMPERIAL,
        dateOfBirth = dob,
    )
}
