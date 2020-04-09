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

struct LoginView: FlowView, View {
    typealias Input = String
    typealias Output = LoginViewResult
    var proxy = ProxyFlow<Output>()

    @State private var email: String = ""
    @State private var password: String = ""

    func login() {
        resolve(.login(email: email, password: password))
    }

    func register() {
        resolve(.register)
    }

    func forgot() {
        resolve(.forgotPassword(email: email))
    }

    func attach(context: Input) {
        self.email = context
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
