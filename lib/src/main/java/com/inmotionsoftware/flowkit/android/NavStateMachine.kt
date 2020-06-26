package com.inmotionsoftware.flowkit.android

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inmotionsoftware.flowkit.*
import com.inmotionsoftware.flowkit.Result
import com.inmotionsoftware.promisekt.*
import kotlinx.android.parcel.Parcelize


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

abstract class StateMachineActivity<S: FlowState,I,O>: AppCompatActivity(), StateMachineDelegate<S>, NavStateMachine, StateMachine<S,I,O> {

    private val fragmentCache = mutableMapOf<Class<Fragment>, Fragment>()

    fun <F: Fragment> getFragment(fragment: Class<F>): F {
        val key = fragment as Class<Fragment>
        fragmentCache.get(key)?.let { return it as F }
        val f = fragment.newInstance()
        fragmentCache[key] = f
        return f
    }


    inner class Model: ViewModel() {
        val state = MutableLiveData<S>()
        val pending = MutableLiveData<Boolean>()
    }

    var delegate: StateMachineDelegate<S>? = null
    private var viewId = 0
    val viewModel = Model()

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

        this.startActivityForResult(intent, code)
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
        val intent = Intent(this, activity)
        intent.putExtra(FLOWKIT_BUNDLE_CONTEXT, bundle)
        if (!animated) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            this.overridePendingTransition(0, 0);
        }
        return dispatch<O2>(intent)
    }

    override fun <S2: FlowState, I2, O2, SM2: StateMachineActivity<S2, I2, O2>> subflow(stateMachine: Class<SM2>, state: S2, animated: Boolean): Promise<O2>  {
        val intent = Intent(this, stateMachine)
        intent.putExtra(FLOWKIT_BUNDLE_CONTEXT, Bundle().put("state", state))
        if (!animated) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            this.overridePendingTransition(0, 0);
        }
        return dispatch<O2>(intent)
    }

    fun <I2, O2, F: FlowFragment<I2,O2>> subflow2(fragment: Class<F>, context: I2, animated: Boolean = true): Promise<O2> =
            subflow(fragment=getFragment(fragment), context=context, animated=animated)

    override fun <I2, O2, F: FlowFragment<I2,O2>> subflow(fragment: F, context: I2, animated: Boolean): Promise<O2> {
        if (this.isDestroyed) {
            return Promise(error= java.lang.IllegalStateException("Trying to add fragment to destroyed Activity"))
        }

        val pending = Promise.pending<O2>()
        this.runOnUiThread {
            @Suppress("UNCHECKED_CAST")
            val viewModel = ViewModelProvider(this).get(FlowViewModel::class.java) as FlowViewModel<I2,O2>
            viewModel.input.value = context
            viewModel.resolver = pending.second
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
            trans.commit()
    }

    override fun stateDidChange(from: S, to: S) {
        this.runOnUiThread {
            Log.d(this.localClassName, "stateDidChange: ${from} -> ${to}")
            this.viewModel.state.value = to
        }
        this.delegate?.stateDidChange(from=from, to=to)
    }

    protected open fun jumpToState(state: S): Promise<O> =
        nextState(prev=state, curr=state)
            .map { when(it) {
                is Result.Success -> it.value
                is Result.Failure -> throw it.cause
            }}

    private fun nextState(prev: S, curr: S): Promise<Result<O>> {
        this.stateDidChange(from=prev, to=curr)
        this.getResult(state=curr)?.let {
            return this.onTerminate(state=curr, context=it)
                .map { Result.Success(it) as Result<O> }
                .recover { Promise.value(Result.Failure<O>(it)) }
        }
        return dispatch(state=curr)
            .thenMap { nextState(prev=curr, curr=it) }
            .recover { nextState(prev=curr, curr=createState(error=it)) }
    }

    protected open fun createFragmentContainerView(): Int {
        val viewId = View.generateViewId()

        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val layout = LinearLayout(this.applicationContext)
        layout.layoutParams = params
        layout.orientation = LinearLayout.VERTICAL
        setContentView(layout, params)
        layout.id = viewId
        return viewId
    }

    override fun onResume() {
        super.onResume()

        val pending = this.viewModel.pending.value
        if (pending == null || !pending) {
            // start the statemachine
            val state = this.viewModel.state.value!!
            Log.d(this.localClassName, "loading state: ${state}")
            jumpToState(state)
                .done { this.resolve(it) }
                .catch { this.reject(it) }
        }
    }

    open fun defaultState(): S {
        return this.intent.getBundleExtra(FLOWKIT_BUNDLE_CONTEXT).get("state") as S
    }

    private fun setup(savedInstanceState: Bundle?) {
        this.viewId = createFragmentContainerView()

        // load our state machine...
        this.viewModel.state.value = if (savedInstanceState == null) {
            this.defaultState()
        } else {
            (savedInstanceState.get("state") as? S) ?: this.defaultState()
        }
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup(savedInstanceState)
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setup(savedInstanceState)
    }

    @CallSuper
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = this.viewModel.state.value
        Log.d(this.localClassName, "saving state: ${state}")
        outState.put("state", state)
    }

    @CallSuper
    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        val state = this.viewModel.state.value
        Log.d(this.localClassName, "saving state: ${state}")
        outState.put("state", state)
    }

    @CallSuper
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        this.viewModel.pending.value = true
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
    }

    protected fun resolve(result: O) {
        this.finish(Result.Success(result))
    }

    protected fun reject(cause: Throwable) {
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

    override final fun dispatch(state: State): Promise<State> =
        onBegin(state=state, context=Unit)
            .recover { onFail(state=state, context=it) }
            .ensure {
                Log.w(BootstrapActivity::class.java.simpleName, "Bootstrap StateMachine was resolved, restarting now")
                dispatch(state=state)
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