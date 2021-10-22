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
//  LoginFlowController.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.example.flows

import android.os.Bundle
import com.inmotionsoftware.example.flows.LoginFlowState.*
import com.inmotionsoftware.example.service.UserService
import com.inmotionsoftware.example.views.CreateAccountFragment
import com.inmotionsoftware.example.views.ForgotPasswordFragment
import com.inmotionsoftware.example.views.LoginFragment
import com.inmotionsoftware.flowkit.android.StateMachineActivity
import com.inmotionsoftware.flowkit.back
import com.inmotionsoftware.flowkit.cancel
import com.inmotionsoftware.flowkit.canceled
import com.inmotionsoftware.promisekt.*
import com.inmotionsoftware.promisekt.recover
import java.util.concurrent.Executor
import kotlin.Result
import com.inmotionsoftware.example.flows.CreateAccountState.FromSubmit
import com.inmotionsoftware.flowkit.FlowState

typealias UUID = java.util.UUID
typealias Date = java.util.Date

class CreateAccountFlowController(): StateMachineActivity<CreateAccountState, String?, Credentials>(), CreateAccountStateMachine {
    val service = UserService()

    override fun onBegin(state: CreateAccountState, context: String?): Promise<CreateAccountState.FromBegin> =
        Promise.value(CreateAccountState.FromBegin.EnterInfo(context))

    override fun onEnterInfo(state: CreateAccountState, context: String?): Promise<CreateAccountState.FromEnterInfo> =
        this.subflow2(fragment=CreateAccountFragment::class.java, context=context)
            .map { CreateAccountState.FromEnterInfo.Submit(it) }

    override fun onSubmit(state: CreateAccountState, context: User): Promise<FromSubmit> =
        this.service
            .createAccount(user=context)
            .map {
                val creds = Credentials(username=context.email, password=context.password)
                FromSubmit.End(creds) as FromSubmit
            }
            .recover { Promise.value(FromSubmit.EnterInfo(it.localizedMessage)) }

}

class LoginFlowController() : StateMachineActivity<LoginFlowState, Unit, OAuthToken>(), LoginFlowStateMachine {

    private val service = UserService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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