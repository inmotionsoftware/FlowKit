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
    @IBOutlet var error: UILabel!

    private var proxy = Promise<Output>.pending()

    func startFlow(context: String) -> Promise<String> {
        if (!proxy.promise.isPending) {
            proxy.resolver.reject(FlowError.canceled)
            proxy = Promise<Output>.pending()
        }
        self.error.text = nil
        return proxy.promise
    }

    override func willMove(toParent parent: UIViewController?) {
        self.delegate?.willMove(toParent: parent)
        super.willMove(toParent: parent)
    }

    @IBAction
    public func onSubmit() {
        if let email = email.text, email.isValidEmail() {
            self.error.text = nil
            self.proxy.resolver.fulfill(email)
        } else {
            self.error.text = "Invalid email"
        }
    }
}
