// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a 
// copy of this software and associated documentation files (the "Software"), 
// to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, 
// and/or sell copies of the Software, and to permit persons to whom the 
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.
//
//  BundleUtil.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

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
