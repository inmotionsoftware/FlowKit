package com.inmotionsoftware.example.flows

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
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
import com.inmotionsoftware.flowkit.android.*
import com.inmotionsoftware.promisekt.*
import java.util.*
import kotlin.random.Random


//open class FragmentContainer: AppCompatActivity() {
//
//    // this is our fragment target...
//    val viewId: Int = android.view.View.generateViewId()
//
//    @CallSuper
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//        val layout = LinearLayout(this.applicationContext)
//        layout.layoutParams = params
//        layout.orientation = LinearLayout.VERTICAL
//        setContentView(layout, params)
//        layout.id = viewId
//    }
//
//    fun pushFragment(fragment: Fragment) {
//        this.supportFragmentManager.beginTransaction()
//            .replace(this.viewId, fragment)
//            .addToBackStack(null)
//            .commit()
//    }
//}
//
//fun <I,O,F: FlowFragment<I, O>> FragmentContainer.subflow(fragmentClass: Class<F>, context: I): Promise<O> =
//    subflow(fragment=fragmentClass.newInstance(), context=context)
//
//fun <I,O,F: FlowFragment<I, O>> FragmentContainer.subflow(fragment: F, context: I): Promise<O> {
//    if (this.isDestroyed) {
//        return Promise(error=IllegalStateException("Trying to add fragment to destroyed Activity"))
//    }
//
//    val pending = Promise.pending<O>()
//    this.runOnUiThread {
//        @Suppress("UNCHECKED_CAST")
//        val viewModel = ViewModelProvider(this).get(FlowViewModel::class.java) as FlowViewModel<I, O>
//        viewModel.input.value = context
//        viewModel.resolver = pending.second
//        pushFragment(fragment)
//    }
//    return pending.first
//}
//
//
//abstract class FlowActivity2<Input, Output>: AppCompatActivity() {
//
//    abstract var input: Input
//
//    @CallSuper
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        if (!(this.input is Unit)) {
//            this.input = intent.getBundleExtra(FLOW_KIT_ACTIVITY_RESULT)?.get("context") as Input
//        }
//    }
//
//    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
//        super.onSaveInstanceState(outState, outPersistentState)
//        // TODO
//    }
//
//    private fun finish(code: Int, result: Result<Output>) {
//        setResult(code, result.toIntent())
//        finish()
//    }
//
//    fun resolve(value: Output) {
//        finish(code= Activity.RESULT_OK, result= Result.Success(value))
//    }
//
//    fun reject(error: Throwable) {
//        finish(code= Activity.RESULT_CANCELED, result= Result.Failure(error))
//    }
//
//    fun cancel() {
//        this.reject(FlowError.Canceled())
//    }
//
//    fun back() {
//        this.reject(FlowError.Back())
//    }
//
//    override fun onBackPressed() {
//        super.onBackPressed()
//        back()
//    }
//}
//
//abstract class FlowControllerActivity2<I,O>: FlowActivity2<I,O>() {
//    inline fun <reified T> handleResult(resolver: Resolver<T>, resultCode: Int, intent: Intent?) {
//        val result = if (intent == null) {
//            // special case for unit
//            if (T::class.java == Unit::class.java) {
//                Result.Success(Unit as T)
//            } else {
//                Result.Failure(NullPointerException())
//            }
//        } else {
//            when (resultCode) {
//                Activity.RESULT_OK -> toResult<T>(intent)
//                Activity.RESULT_CANCELED -> toResult<T>(intent)
//                else -> {
//                    val err = "Unexpected result code (${resultCode}) while handling activity response"
//                    Result.Failure(RuntimeException(err))
//                }
//            }
//        }
//        resolver.resolve(result)
//    }
//
//    inline fun <reified I2, reified O2, A: FlowActivity<I2, O2>> subflow(activityClass: Class<A>, context: I2): Promise<O2> {
//        val pending = Promise.pending<O2>()
//        val code = FlowDispatcher.register() { id, intent -> handleResult<O2>(pending.second, id, intent) }
//
//        val intent = Intent(this, activityClass)
//            .putExtra(FLOW_KIT_ACTIVITY_INPUT, Bundle().put("context", context))
//        this.startActivityForResult(intent, code)
//
//        return pending.first
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        FlowDispatcher.dispatch(requestCode, resultCode, data)
//    }
//}
//
//abstract class StateMachineActivity<S,I,O, SM: StateMachine<S, I, O>>(val stateMachine: SM) : Flow<I, O>, FlowControllerActivity2<I,O>(),
//    StateMachineDelegate<S> {
//    inner class MyViewModel: ViewModel() {
//        val currentState = MutableLiveData<S>()
//    }
//
//    var viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
//
//    @CallSuper
//    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
//        super.onSaveInstanceState(outState, outPersistentState)
//        // TODO: save our state
//
//    }
//
//    override fun startFlow(context: I): Promise<O> {
//        val begin = this.stateMachine.firstState(context=context)
//        return this.jumpToState(state=begin)
//    }
//
//    @CallSuper
//    override fun stateDidChange(from: S, to: S) {
////        this.delegate?.stateDidChange(from = from, to = to)
//        this.viewModel.currentState.value = to
//        this.stateMachine.stateDidChange(from=from, to=to)
//    }
//
//    private fun jumpToState(state: S): Promise<O> =
//        nextState(prev=state, curr=state)
//            .map {
//                when (it) {
//                    is Result.Success -> it.value
//                    is Result.Failure -> throw it.cause
//                }
//            }
//
//    private fun nextState(prev: S, curr: S): Promise<Result<O>> {
//        this.stateDidChange(from=prev, to=curr)
//        this.stateMachine.getResult(state=curr)?.let {
//            return this.stateMachine.onTerminate(state=curr, context=it)
//        }
//        return stateMachine.dispatch(state=curr).thenMap { nextState(prev=curr, curr=it) }
//    }
//    @CallSuper
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        // TODO
//
//        val state = savedInstanceState?.getBundle("")?.get("state") as? S ?: this.stateMachine.firstState(input)
//        jumpToState(state)
//
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//    }
//}


//class BlahHost {
//
//    override fun stateDidChange(from: S, to: S) {
//        this.viewModel.currentState.value = to
//        this.stateMachine.stateDidChange(from=from, to=to)
//    }
//
//    private fun jumpToState(state: S): Promise<O> =
//        nextState(prev=state, curr=state)
//            .map {
//                when (it) {
//                    is Result.Success -> it.value
//                    is Result.Failure -> throw it.cause
//                }
//            }
//
//    private fun nextState(prev: S, curr: S): Promise<Result<O>> {
//        this.stateDidChange(from=prev, to=curr)
//        this.stateMachine.getResult(state=curr)?.let {
//            return this.stateMachine.onTerminate(state=curr, context=it)
//        }
//        return stateMachine.dispatch(state=curr).thenMap { nextState(prev=curr, curr=it) }
//    }
//}

//class LoginFlowController2: NavigableStateMachine(), LoginFlowStateMachine  {
//
//    override fun onBegin(state: LoginFlowState, context: Unit): Promise<LoginFlowState.FromBegin> {
//        TODO("Not yet implemented")
//    }
//
//    override fun onPrompt(state: LoginFlowState, context: String?): Promise<LoginFlowState.FromPrompt> {
//        TODO("Not yet implemented")
//    }
//
//    override fun onAuthenticate(state: LoginFlowState, context: Credentials): Promise<LoginFlowState.FromAuthenticate> {
//        TODO("Not yet implemented")
//    }
//
//    override fun onForgotPass(state: LoginFlowState, context: String): Promise<LoginFlowState.FromForgotPass> {
//        TODO("Not yet implemented")
//    }
//
//    override fun onEnterAccountInfo(state: LoginFlowState, context: String?): Promise<LoginFlowState.FromEnterAccountInfo> {
//        TODO("Not yet implemented")
//    }
//
//    override fun onCreateAccount(state: LoginFlowState, context: User): Promise<LoginFlowState.FromCreateAccount> {
//        TODO("Not yet implemented")
//    }
//}

