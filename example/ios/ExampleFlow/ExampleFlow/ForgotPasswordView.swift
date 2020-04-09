//
//  ForgotPasswordView.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import SwiftUI
import FlowKit
import PromiseKit

struct ForgotPasswordView: FlowView, View {
    typealias Input = Void
    typealias Output = String

    @State private var email: String = ""

    var proxy = ProxyFlow<Output>()

    func submit() {
        resolve(email)
    }

    var body: some View {
        VStack {
            Text("Forgot Password")
            TextField("Email", text: $email)
                .padding(10)
                .border(Color.gray, width: 1)
            Button("Submit", action: submit)
        }
        .padding(20)
    }
}

struct ForgotPasswordView_Previews: PreviewProvider {
    static var previews: some View {
        return ForgotPasswordView()
    }
}
