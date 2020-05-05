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

class ForgotPasswordViewController: BaseFlowViewController<String, String> {

    @IBOutlet var email: UITextField!
    @IBOutlet var error: UILabel!

    @IBAction
    public func onSubmit() {
        if let email = email.text, email.isValidEmail() {
            self.error.text = nil
            self.resolve(email)
        } else {
            self.error.text = "Invalid email"
        }
    }
}
