//
//  RegisterAccountView.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import SwiftUI
import PromiseKit
import FlowKit

struct RegisterView: ViewFlow, View {


    typealias Input = Void
    typealias Output = User

    @State private var firstName: String = ""
    @State private var lastName: String = ""
    @State private var email: String = ""
    @State private var password: String = ""

    private let promise = Promise<Output>.pending()

    func startFlow(context: Void) -> Promise<User> {
        return self.promise.promise
    }

    func register() {
        let user = User(firstName: firstName, lastName: lastName, email: email, password: password)
        self.promise.resolver.fulfill(user)
    }

    func attach(context: Void) {
        print("attached")
    }

    var body: some View {
        VStack {
            Text("Login")
            TextField("First Name", text: $firstName)
                .padding(10)
                .border(Color.gray, width: 1)

            TextField("Last name", text: $lastName)
                    .padding(10)
                    .border(Color.gray, width: 1)

            TextField("Email", text: $email)
                .padding(10)
                .border(Color.gray, width: 1)

            SecureField("Password", text: $password)
                .padding(10)
                .border(Color.gray, width: 1)

            Button("Register", action: register)
        }
        .padding(20)
    }
}

struct RegisterView_Previews: PreviewProvider {
    static var previews: some View {
        return RegisterView()
    }
}
