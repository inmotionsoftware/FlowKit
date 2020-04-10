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
    @FlowView var registerView = RegisterView()

    private let service = UserService()

    func onBegin(state: LoginFlowState, context: Void) -> Promise<LoginFlowState.Begin> {
        return Promise.value(.prompt(nil))
    }

    func onPrompt(state: LoginFlowState, context: String?) -> Promise<LoginFlowState.Prompt> {
        if let err = context {
            self.showAlert(title: "Error", message: err)
        }
    
        return self.startFlow(view: $loginView, nav: nav, context: context)
            .map { result in
                self.animated = true
                switch (result) {
                    case .forgotPassword(let email): return .forgotPass(email)
                    case .login(let email, let pass): return .authenticate(Credentials(username: email, password: pass))
                    case .register: return .enterAccountInfo(nil)
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
        return self.service
            .autenticate(credentials: context)
            .map {
                .end($0)
            }
            .recover {
                Promise.value(.prompt($0.localizedDescription))
        }
    }

    func onForgotPass(state: LoginFlowState, context: String) -> Promise<LoginFlowState.ForgotPass> {
        return self
            .startFlow(view: forgotPassword, nav: nav, context: context)
            .map { .prompt($0) }
            .canceled { _ in .prompt(nil) }
            .recover { Promise.value(.prompt($0.localizedDescription)) }
    }

    func onEnterAccountInfo(state: LoginFlowState, context: String?) -> Promise<LoginFlowState.EnterAccountInfo> {
        if let err = context {
            self.showAlert(title: "Error", message: err)
        }
        
        return self
            .startFlow(view: $registerView, nav: nav, context: ())
            .map { .createAccount($0) }
            .back {
                self.animated = false
                return .prompt(context)
            }
    }

    func onCreateAccount(state: LoginFlowState, context: User) -> Promise<LoginFlowState.CreateAccount> {
        return self.service
            .createAccount(user: context)
            .map {
                let creds = Credentials(username: context.email, password: context.password)
                return .authenticate(creds)
            }
            .recover { Promise.value(.enterAccountInfo($0.localizedDescription)) }
    }
}
