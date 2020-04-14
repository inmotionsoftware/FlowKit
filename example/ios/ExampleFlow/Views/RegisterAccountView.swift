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

struct RegisterView: FlowableView {
    typealias Input = Void
    typealias Output = User

    @State private var firstName: String = ""
    @State private var lastName: String = ""
    @State private var email: String = ""
    @State private var password: String = ""

    public let resolver: Resolver<Output>

    init(context: Void, resolver: Resolver<User>) {
        self.resolver = resolver
    }

    func register() {
        let user = User(firstName: firstName, lastName: lastName, email: email, password: password)
        self.resolve(user)
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
        let pending = Promise<User>.pending()
        return RegisterView(resolver: pending.resolver)
    }
}
