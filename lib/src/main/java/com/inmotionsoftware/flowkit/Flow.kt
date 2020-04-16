package com.inmotionsoftware.flowkit

import com.inmotionsoftware.promisekt.Promise

interface Flow<Input, Output> {
    fun startFlow(context: Input): Promise<Output>
}

sealed class FlowError: Throwable() {
    class Canceled: FlowError()
    class Back: FlowError()
}

sealed class Result<T> {
    class Success<T>(val value: T): Result<T>()
    class Failure<T>(val cause: Throwable): Result<T>()
}

final class Bootstrap {
    companion object {

    }
}
