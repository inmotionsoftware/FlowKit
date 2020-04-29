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
