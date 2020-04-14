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

    @State private var email: String = ""
    @State private var password: String = ""

    private var error: String?
    public let resolver: Resolver<Output>

    init(context: String?, resolver: Resolver<Output>) {
        self.resolver = resolver
        self.error = context
    }

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
        return VStack {
            error.map { Text($0) }
            Spacer()
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
        return LoginView(context: nil, resolver: Promise.pending().resolver)
    }
}
