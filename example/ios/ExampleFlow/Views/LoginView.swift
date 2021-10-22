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
//  ContentView.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/7/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import SwiftUI
import FlowKit
import PromiseKit

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
        self.resolve(.login(Credentials(username: email, password: password)) )
    }

    func register() {
        self.resolve(.register)
    }

    func forgot() {
        self.resolve(.forgotPassword(email))
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
