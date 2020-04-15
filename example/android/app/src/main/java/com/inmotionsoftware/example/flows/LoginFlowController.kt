package com.inmotionsoftware.example.flows

import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.flowkit.Result
import com.inmotionsoftware.promisekt.Promise

class LoginFlowController: LoginFlowStateMachine {
    override fun onBegin(state: LoginFlowState, context: Unit): Promise<LoginFlowState.FromBegin> {
        return Promise(error=FlowError.Canceled())
    }

    override fun onPrompt(state: LoginFlowState, context: String?): Promise<LoginFlowState.FromPrompt> {
        return Promise(error=FlowError.Canceled())
    }

    override fun onAuthenticate(state: LoginFlowState, context: Credentials): Promise<LoginFlowState.FromAuthenticate> {
        return Promise(error=FlowError.Canceled())
    }

    override fun onForgotPass(state: LoginFlowState, context: String): Promise<LoginFlowState.FromForgotPass> {
        return Promise(error=FlowError.Canceled())
    }

    override fun onEnterAccountInfo(state: LoginFlowState, context: String?): Promise<LoginFlowState.FromEnterAccountInfo> {
        return Promise(error=FlowError.Canceled())
    }

    override fun onCreateAccount(state: LoginFlowState, context: User): Promise<LoginFlowState.FromCreateAccount> {
        return Promise(error=FlowError.Canceled())
    }
}