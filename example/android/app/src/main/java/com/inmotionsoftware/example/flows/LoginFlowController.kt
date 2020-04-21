package com.inmotionsoftware.example.flows

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inmotionsoftware.example.*
import com.inmotionsoftware.example.models.Credentials
import com.inmotionsoftware.example.models.User

import com.inmotionsoftware.example.flows.LoginFlowState.FromBegin
import com.inmotionsoftware.example.flows.LoginFlowState.FromPrompt
import com.inmotionsoftware.example.flows.LoginFlowState.FromAuthenticate
import com.inmotionsoftware.example.flows.LoginFlowState.FromForgotPass
import com.inmotionsoftware.example.flows.LoginFlowState.FromEnterAccountInfo
import com.inmotionsoftware.example.flows.LoginFlowState.FromCreateAccount
import com.inmotionsoftware.example.service.UserService
import com.inmotionsoftware.example.views.CreateAccountFragment
import com.inmotionsoftware.example.views.ForgotPasswordFragment
import com.inmotionsoftware.example.views.LoginFragment
import com.inmotionsoftware.example.views.LoginViewResult
import com.inmotionsoftware.flowkit.*
import com.inmotionsoftware.flowkit.Result
import com.inmotionsoftware.flowkit.android.*
import com.inmotionsoftware.flowkit.back
import com.inmotionsoftware.flowkit.cancel
import com.inmotionsoftware.flowkit.canceled
import com.inmotionsoftware.flowkit.subflow
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.*
import java.io.Serializable
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.*
import kotlin.random.Random
typealias User = com.inmotionsoftware.example.models.User
typealias OAuthToken = com.inmotionsoftware.example.models.OAuthToken
typealias Credentials = com.inmotionsoftware.example.models.Credentials

typealias ActivitResultDelegate = (resultCode: Int, data: Intent?) -> Unit

fun <T> Resolver<T>.resolve(result: Result<T>) {
    when (result) {
        is Result.Success -> fulfill(result.value)
        is Result.Failure -> reject(result.cause)
    }
}

interface UnitSerializable: Serializable {
    private fun readObjectNoData() {}
}


object FlowDispatcher {
    private val registry = mutableMapOf<Int,ActivitResultDelegate>()
    private val rand = Random(Date().time)
    private var counter = 0

    private fun nextRequestCode(): Int {
        return (rand.nextInt() and 0x0000FF00) or (++counter)
    }

    fun register(delegate: ActivitResultDelegate): Int {
        val code = nextRequestCode()
        registry.put(code, delegate)
        return code
    }

    fun dispatch(requestCode: Int, resultCode: Int, data: Intent?) {
        registry.remove(requestCode)?.invoke(resultCode, data)
    }
}


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
//fun <I,O,F: FlowFragment<I,O>> FragmentContainer.subflow(fragmentClass: Class<F>, context: I): Promise<O> =
//    subflow(fragment=fragmentClass.newInstance(), context=context)
//
//fun <I,O,F: FlowFragment<I,O>> FragmentContainer.subflow(fragment: F, context: I): Promise<O> {
//    if (this.isDestroyed) {
//        return Promise(error=IllegalStateException("Trying to add fragment to destroyed Activity"))
//    }
//
//    val pending = Promise.pending<O>()
//    this.runOnUiThread {
//        @Suppress("UNCHECKED_CAST")
//        val viewModel = ViewModelProvider(this).get(FlowViewModel::class.java) as FlowViewModel<I,O>
//        viewModel.input.value = context
//        viewModel.resolver = pending.second
//        pushFragment(fragment)
//    }
//    return pending.first
//}
//

//
//abstract class FlowControllerActivity<I,O>: FlowActivity<I,O>() {
////    class MyViewModel: ViewModel() {
////    }
////
////    val viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
//
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
//    inline fun <reified I2, reified O2, A: FlowActivity<I2,O2>> subflow(activityClass: Class<A>, context: I2): Promise<O2> {
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
//abstract class StateMachineActivity<S,I,O, SM: StateMachine<S,I,O>>(val stateMachine: SM) : Flow<I,O>, FlowControllerActivity<I,O>(), StateMachineDelegate<S> {
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

fun <O2> getResult(intent: Intent): Result<O2> {
    val r = intent.getBundleExtra(FLOW_KIT_ACTIVITY_RESULT)?.get("result")
    val v = r as? O2

    return if (v == null) {
        val e = r as? Throwable
        if (e != null) {
            Result.Failure<O2>(e)
        } else {
            Result.Failure<O2>(NullPointerException())
        }
    } else {
        Result.Success(v)
    }
}

interface Navigation {
    fun <S, I, O, SM: StateMachine<S, I, O>> subflow(stateMachine: Class<SM>, context: I): Promise<O>
    fun <I, O, F: FlowFragment<I,O>> subflow(fragment: F, context: I): Promise<O>
    fun <I, O, A: FlowActivity<I,O>> subflow2(activity: Class<A>, context: I): Promise<O>
}

interface NavigationContainer {
    abstract var nav: Navigation
}

abstract class BlahActivity<S,I,O>: AppCompatActivity(), StateMachineDelegate<S>, Navigation, StateMachine<S,I,O> {

    inner class Model: ViewModel() {
        val state = MutableLiveData<S>()
        val input = MutableLiveData<I>()
    }

    var delegate: StateMachineDelegate<S>? = null
    private val viewId = View.generateViewId()
    val viewModel = Model()

    private fun <I2,O2> dispatch(intent: Intent): Promise<O2> {
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

    override fun <I2, O2, A: FlowActivity<I2,O2>> subflow2(activity: Class<A>, context: I2): Promise<O2> {
        val intent = Intent(this, activity)
        intent.putExtra("context", Bundle().put("input", context))
        return dispatch<I2, O2>(intent)
    }

    override fun <S2, I2, O2, SM2: StateMachine<S2, I2, O2>> subflow(activity: Class<SM2>, context: I2): Promise<O2> {
        val intent = Intent(this, activity)
        intent.putExtra("context", Bundle().put("input", context))
        return dispatch<I2, O2>(intent)
    }

    override fun <I2, O2, F: FlowFragment<I2,O2>> subflow(fragment: F, context: I2): Promise<O2> {
        if (this.isDestroyed) {
            return Promise(error=IllegalStateException("Trying to add fragment to destroyed Activity"))
        }

        val pending = Promise.pending<O2>()
        this.runOnUiThread {
            @Suppress("UNCHECKED_CAST")
            val viewModel = ViewModelProvider(this).get(FlowViewModel::class.java) as FlowViewModel<I2,O2>
            viewModel.input.value = context
            viewModel.resolver = pending.second
            pushFragment(fragment)
        }
        return pending.first
    }

    private fun pushFragment(fragment: Fragment) {
        this.supportFragmentManager.beginTransaction()
            .replace(this.viewId, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun stateDidChange(from: S, to: S) {
        this.runOnUiThread {
            Log.d(this.localClassName, "stateDidChange: ${from} -> ${to}")
            this.viewModel.state.value = to
        }
        this.delegate?.stateDidChange(from=from, to=to)
    }

    private fun jumpToState(state: S): Promise<O> =
        nextState(prev=state, curr=state)
            .map {
                when (it) {
                    is Result.Success -> it.value
                    is Result.Failure -> throw it.cause
                }
            }

    private fun nextState(prev: S, curr: S): Promise<Result<O>> {
        this.stateDidChange(from=prev, to=curr)
        this.getResult(state=curr)?.let {
            return this.onTerminate(state=curr, context=it)
        }
        return dispatch(state=curr).thenMap { nextState(prev=curr, curr=it) }
    }

    private fun createView() {
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val layout = LinearLayout(this.applicationContext)
        layout.layoutParams = params
        layout.orientation = LinearLayout.VERTICAL
        setContentView(layout, params)
        layout.id = viewId
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onRestart() {
        super.onRestart()
    }

    open fun getInput(): I {
        // get our input
        val i: I? = this.intent.getBundleExtra("context")?.get("input") as? I
        return i!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createView()
        val input = getInput()

        this.viewModel.input.value = input

        // load our state machine...
        val state = if (savedInstanceState == null) {
            this.firstState(input)
        } else {
            // TODO: read from the current state from the savedInstanceState
            val s = savedInstanceState.get("state")
            (s as? S) ?: this.firstState(input)
        }

        Log.d(this.localClassName, "loading state: ${state}")

        jumpToState(state)
            .done { this.resolve(it) }
            .catch { this.reject(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = this.viewModel.state.value
        Log.d(this.localClassName, "saving state: ${state}")
        outState.put("state", state)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        val state = this.viewModel.state.value
        Log.d(this.localClassName, "saving state: ${state}")
        outState.put("state", state)
    }

    fun finish(result: Result<O>) {
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

    fun resolve(result: O) {
        this.finish(Result.Success(result))
    }

    fun reject(cause: Throwable) {
        this.finish(Result.Failure(cause))
    }

    override fun onBackPressed() {
        // TODO
        super.onBackPressed()
    }
}

class LoginFlowController() : BlahActivity<LoginFlowState, Unit, OAuthToken>(), LoginFlowStateMachine {

    var animated: Boolean = true
    private val service = UserService()

    override fun onBegin(state: LoginFlowState, context: Unit): Promise<FromBegin> =
        Promise.value(FromBegin.Prompt(null))

    override fun onPrompt(state: LoginFlowState, context: String?): Promise<FromPrompt> =
        this.subflow(fragment=LoginFragment(), context=context)
            .map {
                when(it) {
                    is LoginViewResult.ForgotPassword -> FromPrompt.ForgotPass(it.email)
                    is LoginViewResult.Login -> FromPrompt.Authenticate(Credentials(username=it.email, password=it.password))
                    is LoginViewResult.Register -> FromPrompt.EnterAccountInfo(null)
                }
            }
            .back {
                this.animated = false
                FromPrompt.Prompt(context)
            }
            .cancel {
                this.animated = false
                FromPrompt.Prompt(context)
            }

    override fun onAuthenticate(state: LoginFlowState, context: Credentials): Promise<FromAuthenticate> =
        this.service
            .autenticate(credentials=context)
            .map {
                FromAuthenticate.End(it) as FromAuthenticate
            }
            .recover {
                Promise.value(FromAuthenticate.Prompt(it.localizedMessage))
            }

    override fun onForgotPass(state: LoginFlowState, context: String): Promise<FromForgotPass> =
        this.subflow(fragment=ForgotPasswordFragment(), context=context)
        .map { FromForgotPass.Prompt(it) as FromForgotPass }
            .canceled { FromForgotPass.Prompt(null) as FromForgotPass }
            .recover { Promise.value(FromForgotPass.Prompt(it.localizedMessage)) }

    override fun onEnterAccountInfo(state: LoginFlowState, context: String?): Promise<FromEnterAccountInfo> =
        this.subflow(fragment=CreateAccountFragment(), context=context)
            .map { FromEnterAccountInfo.CreateAccount(it) as FromEnterAccountInfo }
            .back {
                this.animated = false
                FromEnterAccountInfo.Prompt(context)
            }

    override fun onCreateAccount(state: LoginFlowState, context: User): Promise<FromCreateAccount> =
        this.service
            .createAccount(user=context)
            .map {
                val creds = Credentials(username=context.email, password=context.password)
                FromCreateAccount.Authenticate(creds) as FromCreateAccount
            }
            .recover { Promise.value(FromCreateAccount.EnterAccountInfo(it.localizedMessage)) }
}


//
//class LoginFlowController: FragmentContainerActivity<LoginFlowState,Unit,OAuthToken>(), LoginFlowStateMachine {
//    var animated: Boolean = true
//    override var input = Unit
//    private val service = UserService()
//
//    override fun onBegin(state: LoginFlowState, context: Unit): Promise<FromBegin> =
//        Promise.value(FromBegin.Prompt(null))
//
//    override fun onPrompt(state: LoginFlowState, context: String?): Promise<FromPrompt> =
//        this.subflow2(fragment=LoginFragment::class.java, context=context)
//            .map {
//                when(it) {
//                    is LoginViewResult.ForgotPassword -> FromPrompt.ForgotPass(it.email)
//                    is LoginViewResult.Login -> FromPrompt.Authenticate(Credentials(username=it.email, password=it.password))
//                    is LoginViewResult.Register -> FromPrompt.EnterAccountInfo(null)
//                }
//            }
//            .back {
//                this.animated = false
//                FromPrompt.Prompt(context)
//            }
//            .cancel {
//                this.animated = false
//                FromPrompt.Prompt(context)
//            }
//
//    override fun onAuthenticate(state: LoginFlowState, context: Credentials): Promise<FromAuthenticate> =
//        this.service
//            .autenticate(credentials=context)
//            .map {
//                FromAuthenticate.End(it) as FromAuthenticate
//            }
//            .recover {
//                Promise.value(FromAuthenticate.Prompt(it.localizedMessage))
//            }
//
//    override fun onForgotPass(state: LoginFlowState, context: String): Promise<FromForgotPass> =
//        this.subflow2(fragment=ForgotPasswordFragment::class.java, context=context)
//        .map { FromForgotPass.Prompt(it) as FromForgotPass }
//            .canceled { FromForgotPass.Prompt(null) as FromForgotPass }
//            .recover { Promise.value(FromForgotPass.Prompt(it.localizedMessage)) }
//
//    override fun onEnterAccountInfo(state: LoginFlowState, context: String?): Promise<FromEnterAccountInfo> =
//        this.subflow2(fragment=CreateAccountFragment::class.java, context=context)
//            .map { FromEnterAccountInfo.CreateAccount(it) as FromEnterAccountInfo }
//            .back {
//                this.animated = false
//                FromEnterAccountInfo.Prompt(context)
//            }
//
//    override fun onCreateAccount(state: LoginFlowState, context: User): Promise<FromCreateAccount> =
//        this.service
//            .createAccount(user=context)
//            .map {
//                val creds = Credentials(username=context.email, password=context.password)
//                FromCreateAccount.Authenticate(creds) as FromCreateAccount
//            }
//            .recover { Promise.value(FromCreateAccount.EnterAccountInfo(it.localizedMessage)) }
//}
