//
//  LoginFlowController.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import Foundation
import PromiseKit
import SwiftUI
import FlowKit

class LoginFlowController: FlowNavigationController<OAuthToken>, LoginFlowStateMachine {

    func onBegin(state: LoginFlowState, context: String) -> Promise<LoginFlowState.Begin> {
        return Promise.value(.prompt(context))
    }

    func onPrompt(state: LoginFlowState, context: String) -> Promise<LoginFlowState.Prompt> {
        return self
            .startflow(flow: LoginView(), args: context, animated: true)
            .map { result in
                switch (result) {
                    case .forgotPassword(let email): return .forgotPass(email)
                    case .login(let email, let pass): return .authenticate(Credentials(username: email, password: pass))
                    case .register: return .enterAccountInfo(())
                }
            }
            .recover { err -> Promise<LoginFlowState.Prompt> in
                guard err is FlowError else { throw err }
                return self.onPrompt(state: state, context: context)
            }
    }

    func onAuthenticate(state: LoginFlowState, context: Credentials) -> Promise<LoginFlowState.Authenticate> {
        return Promise.value(.end(OAuthToken(token: "", type: "", expiration: Date())))
    }

    func onForgotPass(state: LoginFlowState, context: String) -> Promise<LoginFlowState.ForgotPass> {
        return self
            .startflow(flow: ForgotPasswordView(), args: (), animated: true)
            .map { email in
                return LoginFlowState.ForgotPass.prompt(email)
            }
            .recover { err -> Promise<LoginFlowState.ForgotPass> in
                guard err is FlowError else { throw err }
                return Promise.value(LoginFlowState.ForgotPass.prompt(""))
            }
    }

    func onEnterAccountInfo(state: LoginFlowState, context: Void) -> Promise<LoginFlowState.EnterAccountInfo> {
        return self
            .startflow(flow: RegisterView(), args: ())
            .map { .createAccount($0) }
    }

    func onCreateAccount(state: LoginFlowState, context: User) -> Promise<LoginFlowState.CreateAccount> {
        return Promise.value(.authenticate(Credentials(username: "", password: "")))
    }
}
