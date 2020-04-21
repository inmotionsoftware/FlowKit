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
                else -> null
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


class ResultDispatcher: LifecycleObserver {
    private var requestCode: Int = 0

    protected var registry = mutableMapOf<Int, (resultCode: Int, data: Intent?) -> Unit>()
    protected fun nextRequestCode(): Int = ++this.requestCode

    inline fun <I,reified O, A: FlowActivity<I,O>> subflow(parent: Activity, activity: Class<A>, context: I): Promise<O> {
        val pending = Promise.pending<O>()

        val code = nextRequestCode()
        this.registry[code] = { resultCode: Int, data: Intent? ->
            val result = if (data == null) {
                // special case for unit
                if (O::class.java == Unit::class.java) {
                    Result.Success(Unit as O)
                } else {
                    Result.Failure(NullPointerException())
                }
            } else {
                when (resultCode) {
                    Activity.RESULT_OK -> toResult<O>(data)
                    Activity.RESULT_CANCELED -> toResult<O>(data)
                    else -> {
                        val err = "Unexpected result code (${resultCode}) while handling activity response"
                        Result.Failure(RuntimeException(err))
                    }
                }
            }

            when (result) {
                is Result.Success -> pending.second.fulfill(result.value)
                is Result.Failure -> pending.second.reject(result.cause)
            }
        }

        val intent = Intent(parent, activity)
            .putExtra(FLOW_KIT_ACTIVITY_INPUT, Bundle().put("context", context))

        parent.startActivityForResult(intent, code)
        return pending.first
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        this.registry.remove(requestCode)?.let { it(resultCode, data) }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy(owner: LifecycleOwner) {
        println("onDestroy")
//        if (registry.isEmpty()) return
//
//        // cancel any pending results
//        val result = Result.Failure<Any>(FlowError.Canceled())
//        val data = result.toIntent()
//        registry.values.forEach { it(Activity.RESULT_CANCELED, data) }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate(owner: LifecycleOwner) {
        println("onCreate")
    }
}

//abstract class DispatchActivity: AppCompatActivity() {
//    protected var dispatcher = ResultDispatcher()
//
//    @CallSuper
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        lifecycle.addObserver(dispatcher)
//    }
//
//    @CallSuper
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        dispatcher.onActivityResult(requestCode, resultCode, data)
//    }
//
//    inline fun <I, reified O, A: FlowActivity<I,O>> subflow(activity: Class<A>, context: I): Promise<O> {
//        return dispatcher.subflow(parent=this, activity=activity, context=context)
//    }
//
//    open fun handleBackButton(): Boolean = false
//
//    final override fun onBackPressed() {
//        // see if we have a fragment to handle the back button
//        (this.supportFragmentManager.fragments.firstOrNull() as? Backable?)?.let {
//            it.onBackPressed()
//            return
//        }
//
//        // See if our subclass wants to handle it
//        if (handleBackButton()) return
//
//        // finally let our superclass handle it
//        super.onBackPressed()
//    }
//}

abstract class DispatchActivity: AppCompatActivity() {

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @CallSuper
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }

    inline fun <I, reified O, A: FlowActivity<I,O>> subflow(activity: Class<A>, context: I): Promise<O> {
        val pending = Promise.pending<O>()

        val bundle = Bundle().put("context", context)
        val intent = Intent(parent, activity).putExtra(FLOW_KIT_ACTIVITY_INPUT, bundle)
        this.startActivityForResult(intent, 123)

        // TODO

        return pending.first
    }

    open fun handleBackButton(): Boolean = false

    final override fun onBackPressed() {
        // see if we have a fragment to handle the back button
        (this.supportFragmentManager.fragments.firstOrNull() as? Backable?)?.let {
            it.onBackPressed()
            return
        }

        // See if our subclass wants to handle it
        if (handleBackButton()) return

        // finally let our superclass handle it
        super.onBackPressed()
    }
}

abstract class FlowActivity<Input, Output>: DispatchActivity() {

    abstract var input: Input

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!(this.input is Unit)) {
            this.input = intent.getBundleExtra(FLOW_KIT_ACTIVITY_RESULT)?.get("context") as Input
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        // TODO
    }

    private fun finish(code: Int, result: Result<Output>) {
        setResult(code, result.toIntent())
        finish()
    }

    fun resolve(value: Output) {
        finish(code=Activity.RESULT_OK, result=Result.Success(value))
    }

    fun reject(error: Throwable) {
        finish(code=Activity.RESULT_CANCELED, result=Result.Failure(error))
    }

    fun cancel() {
        this.reject(FlowError.Canceled())
    }

    fun back() {
        this.reject(FlowError.Back())
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//        back()
//    }
}
