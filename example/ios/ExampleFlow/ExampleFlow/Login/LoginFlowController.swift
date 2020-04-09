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

class LoginFlowController: LoginFlowStateMachine {
    private var animated = false

    private let nav: UINavigationController
    init(nav: UINavigationController) {
        self.nav = nav
    }

    func onBegin(state: LoginFlowState, context: String) -> Promise<LoginFlowState.Begin> {
        return Promise.value(.prompt(context))
    }

    func onPrompt(state: LoginFlowState, context: String) -> Promise<LoginFlowState.Prompt> {
        return ViewSubflow(view: LoginView(), nav: nav)
            .startFlow(context: context)
            .map { result in
                self.animated = true
                switch (result) {
                    case .forgotPassword(let email): return .forgotPass(email)
                    case .login(let email, let pass): return .authenticate(Credentials(username: email, password: pass))
                    case .register: return .enterAccountInfo(())
                }
            }
            .recover { err -> Promise<LoginFlowState.Prompt> in
                self.animated = false
                guard err is FlowError else { throw err }
                return self.onPrompt(state: state, context: context)
            }
    }

    func onAuthenticate(state: LoginFlowState, context: Credentials) -> Promise<LoginFlowState.Authenticate> {
        return Promise.value(.end(OAuthToken(token: "", type: "", expiration: Date())))
    }

    func onForgotPass(state: LoginFlowState, context: String) -> Promise<LoginFlowState.ForgotPass> {
        let view = ForgotPasswordViewController.init(nibName: "ForgotPassword", bundle: Bundle.main)
        let flow = ViewControllerSubflow(viewController: view, nav: self.nav)

        return flow.startFlow(context: context)
            .map { email in
                return LoginFlowState.ForgotPass.prompt(email)
            }
            .recover { err -> Promise<LoginFlowState.ForgotPass> in
                guard err is FlowError else { throw err }
                return Promise.value(LoginFlowState.ForgotPass.prompt(""))
            }
    }

    func onEnterAccountInfo(state: LoginFlowState, context: Void) -> Promise<LoginFlowState.EnterAccountInfo> {
        return ViewSubflow(view: RegisterView(), nav: nav)
            .startFlow(context: context)
            .map { .createAccount($0) }
    }

    func onCreateAccount(state: LoginFlowState, context: User) -> Promise<LoginFlowState.CreateAccount> {
        return Promise.value(.authenticate(Credentials(username: "", password: "")))
    }
}