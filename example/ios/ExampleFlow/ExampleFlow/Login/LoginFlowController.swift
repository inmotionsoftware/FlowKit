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


class LoginFlowController: NavigationStateMachine<OAuthToken>, LoginFlowStateMachine {

    private lazy var forgotPassword = ForgotPasswordViewController(nibName: "ForgotPassword", bundle: Bundle.main)
    @FlowView var loginView = LoginView()

    func onBegin(state: LoginFlowState, context: String) -> Promise<LoginFlowState.Begin> {
        return Promise.value(.prompt(context))
    }

    func onPrompt(state: LoginFlowState, context: String) -> Promise<LoginFlowState.Prompt> {
        return self.startFlow(view: $loginView, nav: nav, context: context)
            .map { result in
                self.animated = true
                switch (result) {
                    case .forgotPassword(let email): return .forgotPass(email)
                    case .login(let email, let pass): return .authenticate(Credentials(username: email, password: pass))
                    case .register: return .enterAccountInfo(())
                }
            }
            .back {
                self.animated = false
                return .prompt(context)
            }
            .cancel {
                self.animated = false
                return .prompt(context)
            }
    }

    func onAuthenticate(state: LoginFlowState, context: Credentials) -> Promise<LoginFlowState.Authenticate> {
        return self
            .autenticate(credentials: context)
            .map { .end($0) }
            .recover { _ in Promise.value(.prompt(context.username)) }
    }

    func onForgotPass(state: LoginFlowState, context: String) -> Promise<LoginFlowState.ForgotPass> {
        return self.startFlow(view: forgotPassword, nav: nav, context: context)
            .map { LoginFlowState.ForgotPass.prompt($0) }
            .back { .prompt("") }
    }

    func onEnterAccountInfo(state: LoginFlowState, context: Void) -> Promise<LoginFlowState.EnterAccountInfo> {
        self.startFlow(view: RegisterView(), nav: nav, context: context)
            .map { .createAccount($0) }
    }

    func onCreateAccount(state: LoginFlowState, context: User) -> Promise<LoginFlowState.CreateAccount> {
        return Promise.value(.authenticate(Credentials(username: "", password: "")))
    }

//    func onTerminate(state: LoginFlowState, context: LoginFlowState.Result) -> Promise<LoginFlowState.Result> {
//        return Promise.value(context)
//    }
//    func onEnd(state: LoginFlowState, context: OAuthToken) -> Promise<LoginFlowState.End> {
//        return Promise.value(.terminate(context))
//    }
//    func onFail(state: LoginFlowState, context: Error) -> Promise<LoginFlowState.Fail> {
//        return Promise.value(.terminate(context))
//    }
}

extension LoginFlowController {
    func autenticate(credentials: Credentials) -> Promise<OAuthToken> {
        let token = OAuthToken(token: "049584309583.AB089EPF451.84050D9AB89CE7", type: "Bearer", expiration: Date())
        return Promise.value(token)
    }
}
