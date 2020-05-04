package com.inmotionsoftware.flowkit.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.flowkit.Result
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.fulfill
import com.inmotionsoftware.promisekt.reject

val FLOW_KIT_ACTIVITY_RESULT: String = "FLOWKIT_RESULT"
val FLOW_KIT_ACTIVITY_INPUT: String = "FLOWKIT_INPUT"

inline fun <reified T> toResult(intent: Intent): Result<T> =
    intent
        .getBundleExtra(FLOW_KIT_ACTIVITY_RESULT)
        ?.get("result")?.let {
            when (it) {
                is Throwable -> Result.Failure<T>(it)
                is T -> Result.Success<T>(it)
                else -> {
                    val result: Result<T>? = null
                    result
                }
            }
        } ?:  Result.Failure<T>(java.lang.NullPointerException())


fun <T> Result<T>.toIntent(): Intent =
    when (this) {
        is Result.Failure -> {
            val bundle = Bundle().put("result", this.cause)
            Intent().putExtra(FLOW_KIT_ACTIVITY_RESULT, bundle)
        }

        is Result.Success -> {
            val bundle = Bundle().put("result", this.value)
            Intent().putExtra(FLOW_KIT_ACTIVITY_RESULT, bundle)
        }
    }

abstract class FlowActivity<Output>: AppCompatActivity() {

    private fun finish(code: Int, result: Result<Output>) {
        setResult(code, result.toIntent())
        finish()
    }

    protected open fun resolve(value: Output) {
        finish(code=Activity.RESULT_OK, result=Result.Success(value))
    }

    protected open fun reject(error: Throwable) {
        finish(code=Activity.RESULT_CANCELED, result=Result.Failure(error))
    }

    protected open fun cancel() {
        this.reject(FlowError.Canceled())
    }

    protected open fun back() {
        this.reject(FlowError.Back())
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        back()
    }
}

abstract class FlowInputActivity<Input, Output>: FlowActivity<Output>() {

    abstract var input: Input

    protected open fun loadInput(bundle: Bundle?): Input = bundle?.get("context") as Input

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.input = loadInput(intent.getBundleExtra(FLOW_KIT_ACTIVITY_RESULT))
    }
}
