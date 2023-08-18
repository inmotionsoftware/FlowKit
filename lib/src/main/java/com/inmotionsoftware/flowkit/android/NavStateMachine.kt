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
//  NavStateMachine.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
package com.inmotionsoftware.flowkit.android

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.inmotionsoftware.flowkit.*
import com.inmotionsoftware.flowkit.Result
import com.inmotionsoftware.promisekt.*
import kotlinx.android.parcel.Parcelize
import java.lang.IllegalStateException
import java.util.*


interface NavStateMachine {
    fun <I, O, F: FlowFragment<I,O>> subflow(fragment: F, context: I, animated: Boolean = true): Promise<O>
    fun <S: FlowState, I, O, SM: StateMachineActivity<S, I, O>> subflow(stateMachine: Class<SM>, state: S, animated: Boolean = true): Promise<O>
    fun <O, A: Activity> subflow(activity: Class<A>, bundle: Bundle?, animated: Boolean = true): Promise<O>
    fun <O, A: FlowActivity<O>> subflow(activity: Class<A>, context: Unit, animated: Boolean = true): Promise<O> = subflow(activity=activity, animated=animated)
    fun <O, A: FlowActivity<O>> subflow(activity: Class<A>, animated: Boolean = true): Promise<O> = subflow(activity=activity, bundle=null, animated=animated)
    fun <I, O, A: FlowInputActivity<I,O>> subflow(activity: Class<A>, context: I, animated: Boolean = true): Promise<O> =
        subflow(activity=activity, bundle=Bundle().put("context", context), animated=animated)
}

internal const val FLOWKIT_BUNDLE_CONTEXT = "com.inmotionsoftware.flowkit.Context"

fun <T> ignore_error(lambda: () -> T): T? =
    when(val rt = attempt(lambda)) {
        is com.inmotionsoftware.promisekt.Result.fulfilled -> rt.value
        is com.inmotionsoftware.promisekt.Result.rejected -> null
    }

fun <T> attempt(lambda: () -> T): com.inmotionsoftware.promisekt.Result<T> {
    try {
        val rt = lambda()
        return com.inmotionsoftware.promisekt.Result.fulfilled(rt)
    } catch(e: Throwable) {
        return com.inmotionsoftware.promisekt.Result.rejected(e)
    }
}

interface StateMachineViewModelDelegate<S: FlowState, I,O>: StateMachineDelegate<S> {
    val stateMachine: StateMachine<S,I,O>
    fun resolve(result: O)
    fun reject(cause: Throwable)
}

class StateMachineViewModel<S: FlowState, I, O>: ViewModel(), StateMachineDelegate<S> {
    var state: S? = null
    var pending: Boolean = false
    lateinit var delegate: StateMachineViewModelDelegate<S, I, O>

    override fun stateDidChange(from: S, to: S) {
        state = to
        this.delegate.stateDidChange(from=from, to=to)
    }

    fun jumpToState(state: S) =
        nextState(prev=state, curr=state)
            .done { when(it) {
                is Result.Success -> delegate.resolve(it.value)
                is Result.Failure -> delegate.reject(it.cause)
            }}

    private fun nextState(prev: S, curr: S): Promise<Result<O>> {
        this.stateDidChange(from = prev, to = curr)
        delegate.stateMachine.getResult(state=curr)?.let {
            return delegate.stateMachine.onTerminate(state=curr, context=it)
                .map { Result.Success(it) as Result<O> }
                .recover { Promise.value(Result.Failure<O>(it)) }
        }

        return delegate.stateMachine.dispatch(prev=prev, state=curr)
            .thenMap { nextState(prev=curr, curr=it) }
            .recover { nextState(prev=curr, curr=delegate.stateMachine.createState(error=it)) }
    }
}

internal object StateMachineViewModelProvider {
    private class Owner: ViewModelStoreOwner {
        private val store = ViewModelStore()
        override fun getViewModelStore(): ViewModelStore = store
    }

    private val map = mutableMapOf<String, ViewModelStoreOwner>()

    private fun getViewModelOwner(key: String): ViewModelStoreOwner {
        val map = this.map
        map.get(key)?.let { return it }
        val owner = Owner()
        map.put(key, owner)
        return owner
    }

    fun <S: FlowState, I,O> finish(activity: StateMachineActivity<S, I, O>) {
        val key = activity.javaClass.canonicalName!!
        val map = this.map
        map.remove(key)?.viewModelStore?.clear()
    }

    fun <S: FlowState, I,O, SM: StateMachineActivity<S, I, O>> of(activity: SM): StateMachineViewModel<S, I,O> {
        val owner = getViewModelOwner(activity.javaClass.canonicalName!!)
        val key = getViewModelKey(viewModel=StateMachineViewModel::class.java, target=activity)
        @Suppress("UNCHECKED_CAST")
        return ViewModelProvider(owner).get(key, StateMachineViewModel::class.java) as StateMachineViewModel<S, I,O>
    }

    fun <I, O, F: FlowFragment<I,O>> of(activity: Activity, fragment: F): FlowViewModel<I,O> {
        val owner = getViewModelOwner(activity.javaClass.canonicalName!!)
        val key = getViewModelKey(viewModel=FlowViewModel::class.java, target= fragment)
        @Suppress("UNCHECKED_CAST")
        return ViewModelProvider(owner).get(key, FlowViewModel::class.java) as FlowViewModel<I,O>
    }

    fun <I, O, F: FlowFragment<I,O>> of(activity: Activity, fragment: F, factory: ViewModelProvider.Factory): FlowViewModel<I,O> {
        val owner = getViewModelOwner(activity.javaClass.canonicalName!!)
        val key = getViewModelKey(viewModel=FlowViewModel::class.java, target= fragment)
        @Suppress("UNCHECKED_CAST")
        return ViewModelProvider(owner, factory).get(key, FlowViewModel::class.java) as FlowViewModel<I,O>
    }
}

private const val DEFAULT_KEY = "com.inmotionsoftware.flowkit.android.ViewModelProvider.DefaultKey"
internal fun <VM: ViewModel> getViewModelKey(viewModel: Class<VM>, target: Any): String {
    return "${DEFAULT_KEY}:${viewModel.canonicalName}:${target.javaClass.canonicalName}"
}

abstract class StateMachineActivity<S: FlowState,I,O>: AppCompatActivity(), StateMachineViewModelDelegate<S, I, O>, NavStateMachine, StateMachine<S,I,O> {
    override val stateMachine: StateMachine<S,I,O> get() = this

    private val fragmentCache = mutableMapOf<Class<Fragment>, Fragment>()

    fun <F: Fragment> getFragment(fragment: Class<F>): F {
        val key = fragment as Class<Fragment>
        fragmentCache.get(key)?.let { return it as F }
        val f = fragment.newInstance()
        fragmentCache[key] = f
        return f
    }

    var delegate: StateMachineDelegate<S>? = null
    private var viewId = 0
    private val viewModel by lazy {
        val vm = StateMachineViewModelProvider.of(this)
        vm.delegate = this
        vm
    }

    private fun <O2> dispatch(intent: Intent): Promise<O2> {
        val pending = Promise.pending<O2>()
        val code = FlowDispatcher.register() { code, data ->
            val result = if (data == null) {
                Result.Failure(NullPointerException())
            } else {
                getResult<O2>(data)
            }
            pending.second.resolve(result)
        }

        // start a new ViewModel store on the stack. We use this to scope all of the subflow's
        // ViewModels, this allows us to destroy them after the flow unwinds
        try {
            this.startActivityForResult(intent, code)
        } catch (t: Throwable) {
            pending.second.reject(t)
        }
        return pending.first
    }

    private fun <A: Activity> shouldAnimate(activity: Class<A>): Boolean {
        val am = this.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val cn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            am.getAppTasks().firstOrNull()?.getTaskInfo()?.topActivity;
        } else {
            //noinspection deprecation
            am.getRunningTasks(1).firstOrNull()?.topActivity;
        }
        cn?.let {
            if (it.javaClass == activity) {
                return false
            }
        }

        return true
    }

    override fun <O2, A: Activity> subflow(activity: Class<A>, bundle: Bundle?, animated: Boolean): Promise<O2> {
        if (this.isDestroyed) {
            return Promise(error=IllegalStateException("Trying to add fragment to destroyed Activity"))
        }

        val intent = Intent(this, activity)
        intent.putExtra(FLOWKIT_BUNDLE_CONTEXT, bundle)
        if (!animated) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            this.overridePendingTransition(0, 0);
        }
        return dispatch<O2>(intent)
    }

    override fun <S2: FlowState, I2, O2, SM2: StateMachineActivity<S2, I2, O2>> subflow(stateMachine: Class<SM2>, state: S2, animated: Boolean): Promise<O2> =
        subflow(activity=stateMachine, bundle=Bundle().put("state", state), animated=animated)

    fun <I2, O2, F: FlowFragment<I2,O2>> subflow2(fragment: Class<F>, context: I2, animated: Boolean = true): Promise<O2> =
            subflow(fragment=getFragment(fragment), context=context, animated=animated)

    override fun <I2, O2, F: FlowFragment<I2,O2>> subflow(fragment: F, context: I2, animated: Boolean): Promise<O2> {
        if (this.isDestroyed) {
            return Promise(error=IllegalStateException("Trying to add fragment to destroyed Activity"))
        }

        val pending = Promise.pending<O2>()
        this.runOnUiThread {
            val factory = FlowViewModelFactory()
            val viewModel = StateMachineViewModelProvider.of(activity=this, fragment=fragment, factory=factory)

            viewModel.init(context, pending.second)
            pushFragment(fragment, animated)
        }
        return pending.first
    }

    private fun pushFragment(fragment: Fragment, animated: Boolean) {
        val trans = this.supportFragmentManager
            .beginTransaction()
            .replace(this.viewId, fragment)
            .addToBackStack(null)

            if (!animated) trans.setCustomAnimations(0, 0)
            trans.commitAllowingStateLoss()
    }


    protected open fun createFragmentContainerView(restoredViewId: Int?): Int {
        val viewId = restoredViewId ?: View.generateViewId()

        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val layout = LinearLayout(this.applicationContext)
        layout.layoutParams = params
        layout.orientation = LinearLayout.VERTICAL
        setContentView(layout, params)
        layout.id = viewId
        return viewId
    }

    override fun stateDidChange(from: S, to: S) {
        if (this.isDestroyed) {
            throw IllegalStateException("activity has been destroyed!")
        }
        super.stateDidChange(from, to)
        this.delegate?.stateDidChange(from, to)
    }

    open fun defaultState(): S {
        return this.intent.getBundleExtra(FLOWKIT_BUNDLE_CONTEXT).get("state") as S
    }

    private fun setup(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            this.viewId = createFragmentContainerView(savedInstanceState.getInt("view_id"))

            val fragments = supportFragmentManager.fragments
            // This implementation use local cache variable
            // as the source of truth for fragments.
            // In case of restoring we need to put all saved fragments to cache
            fragments.forEach { fragment ->
                val key = fragment.javaClass as Class<Fragment>
                fragmentCache[key] = fragment
            }
        } else {
            this.viewId = createFragmentContainerView(null)
        }
        this.viewModel.delegate = this

        // check the ViewModel's state. If we have one in memory it's very likely that it is more
        // up to date then our saved instance.
        if (this.viewModel.state == null) {

            // we don't have a ViewModel state, let's try to load one from our saved instance. Failing
            // that we'll load the default state
            val state = if (savedInstanceState != null) {
                (savedInstanceState.get("state") as? S) ?: this.defaultState()
            } else {
                this.defaultState()
            }
            // load our state machine...
            this.viewModel.state = state
            Log.d(this.localClassName, "loading state: ${state}")

            // jump into the state machine
            this.viewModel.jumpToState(state)
        } else {
            //
            // Jump to the saved state if there is one.
            // Use case:
            //    1. Put the app to the background
            //    2. Receive push notification
            //    3. Select the notification to open the app
            //
            // Without jumping to the saved state, the app would present a blank screen
            //
            this.viewModel.apply {
                state?.let { jumpToState(it) }
            }
        }
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //clear out the viewModel for app restore
        this.viewModel.state = null

        setup(savedInstanceState)
    }

    @CallSuper
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = this.viewModel.state
        Log.d(this.localClassName, "saving state: ${state}")
        outState.put("state", state)
        outState.putInt("view_id", viewId)
    }

    @CallSuper
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        this.viewModel.pending = true
        FlowDispatcher.dispatch(requestCode, resultCode, data)
    }

    protected fun finish(result: Result<O>) {
        val intent = result.toIntent()
        val code = when(result) {
            is Result.Success -> Activity.RESULT_OK
            is Result.Failure -> {
                result.cause.printStackTrace()
                Activity.RESULT_CANCELED
            }
        }
        this.setResult(code, intent)
        this.finish()
        StateMachineViewModelProvider.finish(this)
    }

    protected fun clearProvider(){
        StateMachineViewModelProvider.finish(this)
    }

    override fun resolve(result: O) {
        if (this.isDestroyed) return
        this.finish(Result.Success(result))
    }

    override fun reject(cause: Throwable) {
        if (this.isDestroyed) return
        this.finish(Result.Failure(cause))
    }

    override fun onBackPressed() {
        (this.supportFragmentManager.fragments.firstOrNull() as? Backable)?.let {
            it.onBackPressed()
            return
        }
        super.onBackPressed()
    }
}

abstract class BootstrapActivity: StateMachineActivity<BootstrapActivity.State, Unit, Unit>(), StateMachine<BootstrapActivity.State, Unit, Unit> {

    sealed class State: FlowState {
        @Parcelize class Begin(val context: Unit): State(), Parcelable
        @Parcelize class Fail(val context: Throwable): State(), Parcelable
    }

    override fun defaultState() = createState(Unit)
    override final fun createState(context: Unit) = State.Begin(context=context)
    override fun createState(error: Throwable): State = State.Fail(context=error)

    override final fun dispatch(prev: State, state: State): Promise<State> =
        onBegin(state=prev, context=Unit)
            .recover { onFail(state=prev, context=it) }
            .ensure {
                Log.w(BootstrapActivity::class.java.simpleName, "Bootstrap StateMachine was resolved, restarting now")
                dispatch(prev=prev, state=prev)
            }
            .map { defaultState() }

    override final fun getResult(state: State): Result<Unit>? = null
    override final fun onTerminate(state: State, context: Result<Unit>) :  Promise<Unit> =
        when (context) {
            is Result.Success -> Promise.value(context.value)
            is Result.Failure -> Promise(error=context.cause)
        }

    abstract fun onBegin(state: State, context: Unit): Promise<Unit>
    open fun onFail(state: State, context: Throwable): Promise<Unit> {
        Log.e(BootstrapActivity::class.java.simpleName, "Uncaught error occured", context)
        return Promise.value(Unit)
    }
}