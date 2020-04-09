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

class ForgotPasswordViewController: UIViewController, ViewControllerFlow {
    typealias Input = String
    typealias Output = String

    var delegate: ViewControllerDelegate?

    private let promise = Promise<Output>.pending()

    func startFlow(context: String) -> Promise<String> {
        return self.promise.promise
    }

    func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        return self.delegate?.navigationController(navigationController, shouldPop: viewController) ?? false
    }

    @IBOutlet var email: UITextField!

    @IBAction
    public func onSubmit() {
        self.promise.resolver.fulfill(email.text ?? "")
    }

    func attach(context: Input) {
        self.email.text = context
    }
}
