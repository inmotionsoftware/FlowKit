package com.inmotionsoftware.example.flows

import com.inmotionsoftware.example.flows.LoginFlowState.*
import com.inmotionsoftware.example.service.UserService
import com.inmotionsoftware.example.views.CreateAccountFragment
import com.inmotionsoftware.example.views.ForgotPasswordFragment
import com.inmotionsoftware.example.views.LoginFragment
import com.inmotionsoftware.flowkit.android.StateMachineActivity
import com.inmotionsoftware.flowkit.back
import com.inmotionsoftware.flowkit.cancel
import com.inmotionsoftware.flowkit.canceled
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover

typealias UUID = java.util.UUID
typealias Date = java.util.Date

class CreateAccountFlowController(): StateMachineActivity<CreateAccountState, String?, Credentials>(), CreateAccountStateMachine {
    private val service = UserService()

    override fun onBegin(state: CreateAccountState, context: String?): Promise<CreateAccountState.FromBegin> {
        TODO("Not yet implemented")
    }

    override fun onEnterInfo(state: CreateAccountState, context: String?): Promise<CreateAccountState.FromEnterInfo> =
        this.subflow2(fragment=CreateAccountFragment::class.java, context=context)
            .map { CreateAccountState.FromEnterInfo.Submit(it) }

    override fun onSubmit(state: CreateAccountState, context: User): Promise<CreateAccountState.FromSubmit> =
        this.service
            .createAccount(user=context)
            .map {
                val creds = Credentials(username=context.email, password=context.password)
                CreateAccountState.FromSubmit.End(creds) as CreateAccountState.FromSubmit
            }
            .recover { Promise.value(CreateAccountState.FromSubmit.EnterInfo(null)) }

}

class LoginFlowController() : StateMachineActivity<LoginFlowState, Unit, OAuthToken>(), LoginFlowStateMachine {

    private val service = UserService()

    override fun onBegin(state: LoginFlowState, context: Unit): Promise<FromBegin> =
        Promise.value(FromBegin.Prompt(null))

    override fun onPrompt(state: LoginFlowState, context: String?): Promise<FromPrompt> =
        this.subflow2(fragment=LoginFragment::class.java, context=context)
            .map {
                when(it) {
                    is LoginViewResult.ForgotPassword -> FromPrompt.ForgotPass(it.context)
                    is LoginViewResult.Login -> FromPrompt.Authenticate(it.context)
                    is LoginViewResult.Register -> FromPrompt.CreateAccount(null)
                }
            }
            .back { FromPrompt.Prompt(context) }
            .cancel { FromPrompt.Prompt(context) }

    override fun onAuthenticate(state: LoginFlowState, context: Credentials): Promise<FromAuthenticate> =
        this.service
            .autenticate(credentials=context)
            .map { FromAuthenticate.End(it) as FromAuthenticate }
            .recover {
                Promise.value(FromAuthenticate.Prompt(it.localizedMessage))
            }

    override fun onForgotPass(state: LoginFlowState, context: String): Promise<FromForgotPass> =
        this.subflow2(fragment=ForgotPasswordFragment::class.java, context=context)
        .map { FromForgotPass.Prompt(it) as FromForgotPass }
            .canceled { FromForgotPass.Prompt(null) as FromForgotPass }
            .recover { Promise.value(FromForgotPass.Prompt(it.localizedMessage)) }

    override fun onCreateAccount( state: LoginFlowState, context: String?): Promise<FromCreateAccount> =
        this.subflow(stateMachine=CreateAccountFlowController::class.java, state=CreateAccountState.Begin(context))
            .map {
                val creds = Credentials(username=it.username, password=it.password)
                FromCreateAccount.Authenticate(creds) as FromCreateAccount
            }
            .recover { Promise.value(FromCreateAccount.Prompt(it.localizedMessage)) }
}