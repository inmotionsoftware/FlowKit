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

class ForgotPasswordViewController: UIViewController, FlowViewController {
    typealias Input = String
    typealias Output = String

    var delegate: ViewControllerDelegate?
    @IBOutlet var email: UITextField!

    private var proxy = Promise<Output>.pending()

    func startFlow(context: String) -> Promise<String> {
        if (!proxy.promise.isPending) {
            proxy.resolver.reject(FlowError.canceled)
            proxy = Promise<Output>.pending()
        }
        return proxy.promise
    }

    @IBAction
    public func onSubmit() {
        self.proxy.resolver.fulfill(email.text ?? "")
    }

    func attach(context: Input) {
        self.email.text = context
    }
}
