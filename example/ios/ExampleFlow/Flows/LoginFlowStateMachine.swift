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

extension UUID {
    static func randomUUID() -> UUID { return UUID() }
}

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

public class CreateAccountFlowController: ViewCache, NavStateMachine, CreateAccountStateMachine {
    public var nav: UINavigationController!
    public typealias State = CreateAccountState
    private let service = UserService()

    public func onBegin(state: State, context: String?) -> Promise<State.Begin> {
        return Promise.value(.enterInfo(context))
    }

    public func onEnterInfo(state: State, context: String?) -> Promise<State.EnterInfo> {
        if let err = context {
            self.showAlert(title: "Error", message: err)
        }

        return self
            .subflow(to: RegisterView.self, context: ())
            .map { .submit($0) }
    }

    public func onSubmit(state: State, context: User) -> Promise<State.Submit> {
        return self.service
            .createAccount(user: context)
            .map { Credentials(username: context.email, password: context.password) }
            .map { .end($0) }
            .recover { Promise.value(.enterInfo($0.localizedDescription)) }
    }
}

public class LoginFlowController: ViewCache, NavStateMachine, LoginFlowStateMachine {

    public var nav: UINavigationController!
    public typealias State = LoginFlowState
    private let service = UserService()

    public func onBegin(state: State, context: Void) -> Promise<State.Begin> {
        return Promise.value(.prompt(nil))
    }

    public func onPrompt(state: State, context: String?) -> Promise<State.Prompt> {
        return self.subflow(to: LoginView.self, context: context)
            .map {
                switch ($0) {
                    case .forgotPassword(let email): return .forgotPass(email)
                    case .login(let creds): return .authenticate(creds)
                    case .register: return .createAccount(nil)
                }
            }
            .back { .prompt(context) }
            .cancel { .prompt(context) }
    }

    public func onAuthenticate(state: State, context: Credentials) -> Promise<State.Authenticate> {
        return self.service
            .autenticate(credentials: context)
            .map { .end($0) }
            .recover { Promise.value(.prompt($0.localizedDescription)) }
    }

    public func onForgotPass(state: State, context: String) -> Promise<State.ForgotPass> {
        return self
            .subflow(to: ForgotPasswordViewController.self, nib: "ForgotPassword", context: context)
            .map { .prompt($0) }
            .canceled { _ in .prompt(nil) }
            .recover { Promise.value(.prompt($0.localizedDescription)) }
    }

    public func onCreateAccount(state: State, context: String?) -> Promise<State.CreateAccount> {
        return self.subflow(to: CreateAccountFlowController(), context: context)
            .map { .authenticate($0) }
            .back{ .prompt(nil) }
    }
}
