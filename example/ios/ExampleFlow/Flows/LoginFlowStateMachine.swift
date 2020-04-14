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

extension StateMachine {
    @discardableResult
    func showAlert(title: String, message: String) -> Promise<Void> {
        return self.showAlert(title: title, message: message, actions: ["OK"]).then { _ -> Promise<Void> in return Promise() }
    }

    @discardableResult
    func showAlert(title: String? = nil, message: String, actions: [String], preferredActionIndex: Int? = nil) -> Promise<Int> {
        return Promise(error: FlowError.canceled)
    }
}

public class LoginFlowController: ViewCache, NavStateMachine, LoginFlowStateMachine {

    public var nav: UINavigationController!
    public typealias State = LoginFlowState
    private var animated: Bool = false
    private let service = UserService()

    public func onBegin(state: State, context: Void) -> Promise<State.Begin> {
        return Promise.value(.prompt(nil))
    }

    public func onPrompt(state: State, context: String?) -> Promise<State.Prompt> {
        return self.subflow(to: LoginView.self, context: context)
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

    public func onAuthenticate(state: State, context: Credentials) -> Promise<State.Authenticate> {
        return self.service
            .autenticate(credentials: context)
            .map {
                .end($0)
            }
            .recover {
                Promise.value(.prompt($0.localizedDescription))
        }
    }

    public func onForgotPass(state: State, context: String) -> Promise<State.ForgotPass> {
        return self
            .subflow(to: ForgotPasswordViewController.self, nib: "ForgotPassword", context: context)
            .map { .prompt($0) }
            .canceled { _ in .prompt(nil) }
            .recover { Promise.value(.prompt($0.localizedDescription)) }
    }

    public func onEnterAccountInfo(state: State, context: String?) -> Promise<State.EnterAccountInfo> {
        if let err = context {
            self.showAlert(title: "Error", message: err)
        }

        return self
            .subflow(to: RegisterView.self, context: ())
            .map { .createAccount($0) }
            .back {
                self.animated = false
                return .prompt(context)
            }
    }

    public func onCreateAccount(state: State, context: User) -> Promise<State.CreateAccount> {
        return self.service
            .createAccount(user: context)
            .map {
                let creds = Credentials(username: context.email, password: context.password)
                return .authenticate(creds)
            }
            .recover { Promise.value(.enterAccountInfo($0.localizedDescription)) }
    }
}
