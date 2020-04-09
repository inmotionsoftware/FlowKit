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
    typealias Result = Output
    typealias Input = String
    typealias Output = Void

    var backDelegate: BackButtonDelegate?

    var resolver = Promise<Output>.pending()
    var isPending: Bool { return self.resolver.promise.isPending }
    var isResolved: Bool { return self.resolver.promise.isResolved }
    var isRejected: Bool { return self.resolver.promise.isRejected }

    @IBOutlet var email: UITextField!


    @IBAction
    public func onSubmit() {
        self.resolve(())
    }

    func startFlow(context: String) -> Promise<Void> {
        self.email.text = context
        return resolver.promise
    }

    func prepareForReuse() {
        resolver = Promise<Output>.pending()
    }

    func resolve(_ result: ForgotPasswordViewController.Output) {
        self.resolver.resolver.fulfill(result)
    }

    func reject(_ error: Error) {
        self.resolver.resolver.reject(error)
    }
}
