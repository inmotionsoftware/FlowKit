//
//  FlowNavigationController.swift
//  FlowKit
//
//  Created by Brian Howard on 6/14/17.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import SwiftUI
import Foundation
import UIKit
import PromiseKit

public protocol BackableView {
    var backDelegate: BackButtonDelegate? { get set }
}

public protocol BackButtonDelegate: class {
    func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool
}

extension UINavigationController: UINavigationBarDelegate {
    public func navigationBar(_ navigationBar: UINavigationBar, shouldPop item: UINavigationItem) -> Bool {
        guard viewControllers.count >= (navigationBar.items?.count ?? 0) else { return true }
        guard let top = self.topViewController else { return true }
        guard top.navigationItem == item else { return false }

        guard let delegate = top as? BackButtonDelegate else {
            self.popViewController(animated: true) // TODO: should we animate?
            return false
        }
        return delegate.navigationController(self, shouldPop: top)
    }
}

open class FlowNavigationController<Output> {
    private(set) public var navigationController: UINavigationController
    private weak var rootViewController: UIViewController?
    fileprivate weak var cancelDelegate: (Backable & Cancelable & AnyObject)?

    public init(navigationController nav: UINavigationController)   {
        self.navigationController = nav
    }

    public func popOrPush<Content: View>(hosting: UIHostingController<Content>, animated: Bool) {
        if let trans = self.navigationController.currentTransaction {
            trans.popToOrPush(viewController: hosting)
            trans.commit()
            return
        }
        self.navigationController.popToOrPush(viewController: hosting, animated: animated)
    }

    public func popOrPush(viewController: UIViewController, animated: Bool) {
        if let trans = self.navigationController.currentTransaction {
            trans.popToOrPush(viewController: viewController)
            trans.commit()
            return
        }
        self.navigationController.popToOrPush(viewController: viewController, animated: animated)
    }

    public func setRootViewController(_ viewController: UIViewController) {
        if let trans = self.navigationController.currentTransaction {
            trans.setRootViewController(viewController)
            trans.commit()
            return
        }
        self.navigationController.setRootViewController(viewController)
    }

    open func attach(_ promise: Promise<Output>) -> Promise<Output> {
        let nav = self.navigationController
        // The current top view controller in the navigation stack becomes
        // this flow root view controller.
        self.rootViewController = nav.topViewController

        return promise.ensure {
            guard let root = self.rootViewController else { return }
            guard root != nav.topViewController else { return }
            nav.beginOrAmmendTransaction(animated: true).popTo(viewController: root)
        }
    }

    open func prepareForReuse() {}

    public func cancel() {
        self.cancelDelegate?.cancel()
        self.cancelDelegate = nil
    }
}

extension FlowNavigationController {
    public func startFlow<F: Flow>(flow: F, context: F.Input) -> Promise<F.Output> {
        return flow.startFlow(context: context)
    }
}

extension FlowNavigationController {
    public func startflow<Input, Output, View: FlowView>(flow: View, args: Input, animated: Bool = true ) -> Promise<Output> where Input == View.Input, Output == View.Output {
        let container = FlowHostingController<View>(rootView: flow)
        self.popOrPush(viewController: container, animated: animated)
        let _ = container.view // force load of the view controller
        container.prepareForReuse()
        container.backDelegate = self
        return container.startFlow(context: args)
    }

    public func startflow<Input, Output, View: FlowViewController>(flow: View, args: Input, animated: Bool = true ) -> Promise<Output> where Input == View.Input, Output == View.Output {
        var viewController = flow
        self.popOrPush(viewController: viewController, animated: animated)
        let _ = viewController.view // force load of the view controller
        viewController.prepareForReuse()
        viewController.backDelegate = self
        return viewController.startFlow(context: args)
    }
}

// MARK: - Back Handler
extension FlowNavigationController: BackButtonDelegate {
    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        self.cancelDelegate?.back()
        self.cancelDelegate = nil
        return false
    }
}
