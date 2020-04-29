package com.inmotionsoftware.example.flows

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.*
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.*
import kotlin.random.Random
typealias User = com.inmotionsoftware.example.models.User
typealias OAuthToken = com.inmotionsoftware.example.models.OAuthToken
typealias Credentials = com.inmotionsoftware.example.models.Credentials


class LoginFlowController() : StateMachineActivity<LoginFlowState, Unit, OAuthToken>(), LoginFlowStateMachine {

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
