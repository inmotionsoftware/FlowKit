package com.inmotionsoftware.example.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class User(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
): Parcelable {
    val userId: UUID = UUID.randomUUID()
}