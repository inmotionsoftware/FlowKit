//
//  FlowNavigationController.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import PromiseKit
import UIKit

/*
    Simple class for managing the navigation stack for a FlowStateMachine
**/
open class NavigationStateMachine<Output> {
    private let stack: [UIViewController]
    public let nav: UINavigationController
    public var animated: Bool = true

    public init(nav: UINavigationController) {
        self.nav = nav
        self.stack = self.nav.viewControllers
    }

    public func attach(_ promise: Promise<Output>) -> Promise<Output> {
        return promise.ensure {
            self.nav.setViewControllers(self.stack, animated: self.animated)
        }
    }
}

public extension NavigationStateMachine {
    @discardableResult
    func showAlert(title: String, message: String) -> Promise<Void> {
        return self.showAlert(title: title, message: message, actions: ["OK"]).then { _ -> Promise<Void> in return Promise() }
    }

    @discardableResult
    func showAlert(title: String? = nil, message: String, actions: [String], preferredActionIndex: Int? = nil) -> Promise<Int> {
        return Promise { seal in
            let vc = UIAlertController(title: title, message: message, preferredStyle: .alert)
            actions.enumerated().forEach { idx, title in
                let action = UIAlertAction(title: title, style: .default) { _ in seal.fulfill(idx) }
                vc.addAction(action)

                if (preferredActionIndex != nil && preferredActionIndex! == idx)
                    || idx == 0 {
                    vc.preferredAction = action
                }
            }

            self.nav.present(vc, animated: true, completion: nil)
        }
    }
}
