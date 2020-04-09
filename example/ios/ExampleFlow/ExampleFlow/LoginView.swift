//
//  ContentView.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/7/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import SwiftUI
import FlowKit
import PromiseKit

public enum LoginViewResult {
    case login(email: String, password: String)
    case register
    case forgotPassword(email: String)
}

struct LoginView: ViewFlow, View {
    typealias Input = String
    typealias Output = LoginViewResult

    private let promise = Promise<Output>.pending()

    @State private var email: String = ""
    @State private var password: String = ""

    func login() {
        self.promise.resolver.fulfill(.login(email: email, password: password))
    }

    func register() {
        self.promise.resolver.fulfill(.register)
    }

    func forgot() {
        self.promise.resolver.fulfill(.forgotPassword(email: email))
    }

    func startFlow(context: String) -> Promise<LoginViewResult> {
        return self.promise.promise
    }

    var body: some View {
        VStack {
            Text("Login")
            TextField("Email", text: $email)
                .padding(10)
                .border(Color.gray, width: 1)

            SecureField("Password", text: $password)
                .padding(10)
                .border(Color.gray, width: 1)

            HStack {
                Button("Login", action: login)
                Button("Register", action: register)
            }
            Spacer()
            Button("Forgot Password?", action: forgot)
        }
        .padding(20)
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        return LoginView()
    }
}
