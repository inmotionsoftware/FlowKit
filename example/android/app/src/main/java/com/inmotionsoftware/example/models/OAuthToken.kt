package com.inmotionsoftware.example.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class OAuthToken(val token: String, val type: String, val expiration: Date): Parcelable {
}