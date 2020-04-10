//
//  ForgotPasswordViewController.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import Foundation
import UIKit
import FlowKit
import PromiseKit

class ForgotPasswordViewController: UIViewController, FlowViewController, FlowResolver {

    typealias Input = String
    typealias Output = String

    var delegate: ViewControllerDelegate?
    var proxy = DeferredPromise<String>()

    func startFlow(context: String) -> Promise<String> {
        self.proxy = DeferredPromise()
        return proxy.wrappedValue.ensure{ print("ForgotPasswordViewController Done") }
    }

    @IBOutlet var email: UITextField!

    @IBAction
    public func onSubmit() {
        self.resolve(email.text ?? "")
    }

    func attach(context: Input) {
        self.email.text = context
    }
}
