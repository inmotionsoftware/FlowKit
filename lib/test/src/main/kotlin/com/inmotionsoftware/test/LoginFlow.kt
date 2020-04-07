package com.inmotionsoftware.test
import com.inmotionsoftware.promisekt.*
import com.inmotionsoftware.test.LoginFlowStateMachine

data class Credentials(val username: String, val password: String)
data class User(val email: String, val password: String)

sealed class PromptResult {
    class Login(val creds: Credentials):  PromptResult()
    class CreateAccount: PromptResult()
    class ForgotPassword(val email: String): PromptResult()
}

data class OAuthToken(val token: String)

abstract class AuthenticatedStateMachine {
    fun doLogin(creds: Credentials): Promise<OAuthToken> {
        return Promise.value(OAuthToken("AB5658871FED"))
    }
}

class LoginFlowStateMachineImpl: AuthenticatedStateMachine(), LoginFlowStateMachine {

    override fun onBegin(state: LoginFlowStateMachine.State, context: Unit): Promise<LoginFlowStateMachine.Begin> {
        return Promise.value(LoginFlowStateMachine.Begin.Prompt(context = Unit))
    }

    override fun onPrompt(state: LoginFlowStateMachine.State, context: Unit): Promise<LoginFlowStateMachine.Prompt> {
        return this.showPromptScreen()
            .map {
                when (it) {
                    is PromptResult.Login -> LoginFlowStateMachine.Prompt.Authenticate(context = it.creds)
                    is PromptResult.ForgotPassword -> LoginFlowStateMachine.Prompt.ForgotPass(context = it.email)
                    is PromptResult.CreateAccount -> LoginFlowStateMachine.Prompt.EnterAccountInfo(Unit)
                }
            }
    }

    override fun onAuthenticate(state: LoginFlowStateMachine.State, context: Credentials): Promise<LoginFlowStateMachine.Authenticate> {
        TODO("Not yet implemented")
    }

    override fun onForgotPass(state: LoginFlowStateMachine.State, context: String): Promise<LoginFlowStateMachine.ForgotPass> {
        TODO("Not yet implemented")
    }

    override fun onEnterAccountInfo(state: LoginFlowStateMachine.State, context: Unit): Promise<LoginFlowStateMachine.EnterAccountInfo> {
        TODO("Not yet implemented")
    }

    override fun onCreateAccount(state: LoginFlowStateMachine.State, context: User): Promise<LoginFlowStateMachine.CreateAccount> {
        TODO("Not yet implemented")
    }
}

fun LoginFlowStateMachineImpl.showPromptScreen(): Promise<PromptResult> {
    return Promise.value(PromptResult.Login(Credentials(username = "", password = "")))
}