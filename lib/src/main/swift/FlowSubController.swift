//
//  FlowSubController.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import PromiseKit
import UIKit

internal class FlowSubController<View: FlowViewController>: Flow, ViewControllerDelegate {
    public typealias Input = View.Input
    public typealias Output = View.Output

    private(set) public var nav: UINavigationController
    private weak var delegate: ViewControllerDelegate? = nil
    private let proxy = DeferredPromise<Output>()
    private let viewController: View
    private let animated: Bool

    public init(viewController: View, nav: UINavigationController, animated: Bool = true) {
        self.viewController = viewController
        self.nav = nav
        self.animated = animated
    }

    public func startFlow(context: Input) -> Promise<Output> {
        self.viewController.delegate = self
        // listen for back button delegates
        proxy.reset()
        // push on the stack
        if let trans = self.nav.currentTransaction {
            trans.popToOrPush(viewController: self.viewController)
            trans.commit()
        } else {
            self.nav.popToOrPush(viewController: self.viewController, animated: animated)
        }

        // intercept the promise
        self.viewController
            .startFlow(context: context)
            .done { self.proxy.resolve($0) }
            .catch { self.proxy.reject($0) }
        return proxy.wrappedValue
    }
}

extension FlowSubController {
    public func willMove(toParent parent: UIViewController?) {
        // Cancel our promise if the view controller is being popped from the
        // view stack
        if parent == nil && self.proxy.wrappedValue.isPending {
            self.proxy.reject(FlowError.canceled)
        }
    }

    private func shouldPop(_ navigationController: UINavigationController) -> Bool {
        if self.proxy.wrappedValue.isPending {
            self.proxy.reject(FlowError.back)
        }
        return false
    }

    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        // if we handled the pop, tell the delegate not to
        if self.shouldPop(navigationController) { return false }
        return self.delegate?.navigationController(navigationController, shouldPop: viewController) ?? false
    }
}
