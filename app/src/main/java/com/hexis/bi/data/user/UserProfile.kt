package com.hexis.bi.data.user

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val gender: String? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val heightIn: Int? = null,
    val weightLb: Int? = null,
    val unitSystem: String? = null,
    val dateOfBirth: Date? = null,
)
