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
//  Flow.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

import android.os.Parcelable
import com.inmotionsoftware.promisekt.Promise
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

interface Flow<Input, Output> {
    fun startFlow(context: Input): Promise<Output>
}

sealed class FlowError: Throwable() {
    class Canceled: FlowError()
    class Back: FlowError()
}

sealed class Result<T>: Parcelable {
    @Parcelize class Success<T>(val value: @RawValue T): Result<T>(), Parcelable
    @Parcelize class Failure<T>(val cause: Throwable): Result<T>(), Parcelable
}

final class Bootstrap {
    companion object {

    }
}
