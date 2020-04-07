package com.inmotionsoftware.flowkit
import com.inmotionsoftware.promisekt.Promise

public final inline class FlowResult<out T> constructor(internal val value: Any?) {
    companion object {
        inline fun <T> Success(value: T): FlowResult<T> =
            FlowResult(value)
        inline fun <T> Failure(cause: Throwable): FlowResult<T> =
            FlowResult(
                Failed(cause)
            )
    }

    val isSuccess: Boolean get() = !isFailure
    val isFailure: Boolean get() = value is Failed

    fun getOrNull(): T? = (value as? T)
    fun errorOrNull(): Throwable? = (value as? Failed)?.error

    override fun toString(): String =
        when {
            isSuccess -> "Success($value)"
            else -> value.toString()
        }

    class Failed(val error: Throwable) {
        override fun toString(): String = "Failure($error)"
    }
}

class CancelFlow(type: Type): Error("Canceled by ${type.name}") {
    enum class Type {
        UP,
        Back
    }
}

interface Flow<Input,Output> {
    fun start(context: Input): Promise<FlowResult<Output>>
}