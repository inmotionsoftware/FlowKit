package com.inmotionsoftware.example.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Credentials(val username: String, val password: String): Parcelable {
}