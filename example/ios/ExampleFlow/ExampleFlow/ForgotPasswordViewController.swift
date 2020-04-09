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

    var proxy = ProxyFlow<String>()
    var backDelegate: BackButtonDelegate?

    @IBOutlet var email: UITextField!

    @IBAction
    public func onSubmit() {
        self.resolve(email.text ?? "")
    }

    func attach(context: Input) {
        self.email.text = context
    }

    func prepareForReuse() {
        proxy = ProxyFlow<String>()
    }
}
