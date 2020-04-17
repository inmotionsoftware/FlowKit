package com.inmotionsoftware.example.flows

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.inmotionsoftware.example.*
import com.inmotionsoftware.example.models.Credentials
import com.inmotionsoftware.example.models.User
import com.inmotionsoftware.promisekt.Promise

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
import com.inmotionsoftware.flowkit.android.*
import com.inmotionsoftware.flowkit.subflow
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover

typealias User = com.inmotionsoftware.example.models.User
typealias OAuthToken = com.inmotionsoftware.example.models.OAuthToken
typealias Credentials = com.inmotionsoftware.example.models.Credentials

class LoginFlowController: NavStateMachine, LoginFlowStateMachine {
    var animated: Boolean = true
    override lateinit var nav: FragContainer
    private val service = UserService()

    override fun onBegin(state: LoginFlowState, context: Unit): Promise<FromBegin> {
        return Promise.value(FromBegin.Prompt(null))
    }

    override fun onPrompt(state: LoginFlowState, context: String?): Promise<FromPrompt> =
        this.subflow2(fragment=LoginFragment::class.java, context=context)
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
        this.subflow2(fragment=ForgotPasswordFragment::class.java, context=context)
        .map { FromForgotPass.Prompt(it) as FromForgotPass }
            .canceled { FromForgotPass.Prompt(null) as FromForgotPass }
            .recover { Promise.value(FromForgotPass.Prompt(it.localizedMessage)) }

    override fun onEnterAccountInfo(state: LoginFlowState, context: String?): Promise<FromEnterAccountInfo> =
        this.subflow2(fragment=CreateAccountFragment::class.java, context=context)
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