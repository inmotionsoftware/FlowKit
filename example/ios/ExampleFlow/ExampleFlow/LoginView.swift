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

struct LoginView: FlowableView {
    typealias Input = String?
    typealias Output = LoginViewResult

    @State private var error: String = ""
    @State private var email: String = ""
    @State private var password: String = ""

    var proxy = DeferredPromise<LoginViewResult>()

    func login() {
        self.resolve(.login(email: email, password: password))
    }

    func register() {
        self.resolve(.register)
    }

    func forgot() {
        self.resolve(.forgotPassword(email: email))
    }

    var body: some View {
        VStack {
            Text("Login")
            if (!error.isEmpty) { Text(error) }
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
