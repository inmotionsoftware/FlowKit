package com.inmotionsoftware.flowkit.android

import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import java.io.Serializable

fun <T> Bundle.put(key: String?, value: T): Bundle {
    if (value == null) return this

    when (value) {
        is Boolean -> this.putBoolean(key, value)
        is Byte -> this.putByte(key, value)
        is Char -> this.putChar(key, value)
        is Short -> this.putShort(key, value)
        is Int -> this.putInt(key, value)
        is Long -> this.putLong(key, value)
        is Float -> this.putFloat(key, value)
        is Double -> this.putDouble(key, value)
        is BooleanArray -> this.putBooleanArray(key, value)
        is ByteArray -> this.putByteArray(key, value)
        is CharArray -> this.putCharArray(key, value)
        is ShortArray -> this.putShortArray(key, value)
        is IntArray -> this.putIntArray(key, value)
        is LongArray -> this.putLongArray(key, value)
        is FloatArray -> this.putFloatArray(key, value)
        is DoubleArray -> this.putDoubleArray(key, value)
        is Parcelable -> this.putParcelable(key, value)
        is String -> this.putString(key, value)
        is CharSequence -> this.putCharSequence(key, value)
        is Serializable -> this.putSerializable(key, value)
        is Size -> this.putSize(key, value)
        is SizeF -> this.putSizeF(key, value)
        is Unit -> {
            /*this.remove(key)*/
        }
        else -> throw ClassCastException("cannot put type ${value} into bundle")
    }
    return this
}

inline fun <reified T> Bundle.getT(key: String?): T = this.get(key) as T
