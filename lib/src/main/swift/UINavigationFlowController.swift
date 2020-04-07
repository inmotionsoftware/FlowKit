//
//  UINavigationFlowController.swift
//  FlowKit
//
//  Created by Brian Howard on 6/14/17.
//  Copyright © 2017 InMotion Software, LLC. All rights reserved.
//

import Foundation
import UIKit
import PromiseKit

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

open class UINavigationFlowController<State:FlowState, Args, Return>
        : FlowController<State, Args, Return> {

    public typealias State = UINavigationFlowController.State

    // TODO: may need to be weak...
    private(set) public var navigationController: UINavigationController
    private weak var rootViewController: UIViewController?

    fileprivate var bc: (Backable & Cancelable)?

    public init(navigationController nav: UINavigationController) {
        self.navigationController = nav
        super.init()
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

    override open func run(args: Args) -> FlowPromise<Return> {
        let nav = self.navigationController
        // The current top view controller in the navigation stack becomes
        // this flow root view controller.
        self.rootViewController = nav.topViewController

        // wire up the delegate and bridge
        return super
            .run(args: args)
            .ensure {
                guard let root = self.rootViewController else { return }
                guard root != nav.topViewController else { return }
                nav.beginOrAmmendTransaction(animated: true).popTo(viewController: root)
            }
    }

    public func flow<Args, Return>(viewController: FlowViewController<Args, Return>, args: Args, animated: Bool = true ) -> FlowPromise<Return> {
        self.popOrPush(viewController: viewController, animated: animated)
        let _ = viewController.view // force load of the view controller
        viewController.prepareForReuse()
        viewController.backDelegate = self
        return viewController.run(args: args)
    }

    public func flow<Args, Return>(viewController: FlowViewController<Args, Return>, args: Args, execute: (FlowViewController<Args, Return>, Args) -> Void ) -> FlowPromise<Return> {
        execute(viewController, args)
        let _ = viewController.view // force load of the view controller
        viewController.prepareForReuse()
        viewController.backDelegate = self
        return viewController.run(args: args)
    }

    public func flow<Args, Return>(viewController: FlowTableViewController<Args, Return>, args: Args, animated: Bool = true ) -> FlowPromise<Return> {
        self.popOrPush(viewController: viewController, animated: animated)
        let _ = viewController.view // force load of the view controller
        viewController.prepareForReuse()
        viewController.backDelegate = self
        return viewController.run(args: args)
    }
    
    public func flow<Args, Return>(viewController: FlowTableViewController<Args, Return>, args: Args, execute: (FlowTableViewController<Args, Return>, Args) -> Void ) -> FlowPromise<Return> {
        execute(viewController, args)
        let _ = viewController.view // force load of the view controller
        viewController.prepareForReuse()
        viewController.backDelegate = self
        return viewController.run(args: args)
    }

//}
//extension UINavigationFlowController {
    override public func cancel() {
        super.cancel()
        if let bc = self.bc {
            bc.cancel()
            self.bc = nil
        }
    }
}

// MARK: - Promise Extension

public protocol FlowInjector {
    func flow<Return>(promise: Promise<Return>) -> FlowPromise<Return>
}

extension UINavigationFlowController: FlowInjector {

    public func flow<Return>(execute: () -> Promise<Return>) -> FlowPromise<Return> {
        return flow(promise: execute())
    }

    public func flow<Return>(promise: Promise<Return>) -> FlowPromise<Return> {

        let resolver = FlowResolver<Return>()
        // cancel the existing
        if let bc = self.bc { bc.cancel() }
        bc = resolver

        promise.then{ Promise.value(resolver.resolve($0)) }
        .catch{ resolver.reject($0) }

        return resolver.promise
    }
}


public extension Promise {
    func resolveFlow(_ controller: FlowInjector) -> FlowPromise<T> {
        return controller.flow(promise: self)
    }
}

// MARK: - Back Handler

extension UINavigationFlowController: BackButtonDelegate {
    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        if let bc = self.bc {
            bc.back()
            self.bc = nil
        }
        return false
    }
}

// MARK: - Convenience for Void args

extension UINavigationFlowController where Args == Void {
    public func flow<Return>(viewController: FlowViewController<Args, Return>, animated: Bool = true ) -> FlowPromise<Return> {
        return self.flow(viewController: viewController, args: (), animated: animated)
    }

    public func flow<Return>(viewController: FlowViewController<Args, Return>, execute: @escaping (FlowViewController<Args, Return>) -> Void ) -> FlowPromise<Return> {
        return self.flow(viewController: viewController, args: ()) { view, args in
            execute(view)
        }
    }

    public func flow<Return>(viewController: FlowTableViewController<Args, Return>, animated: Bool = true ) -> FlowPromise<Return> {
        return self.flow(viewController: viewController, args: (), animated: animated)
    }
    
    public func flow<Return>(viewController: FlowTableViewController<Args, Return>, execute: @escaping (FlowTableViewController<Args, Return>) -> Void ) -> FlowPromise<Return> {
        return self.flow(viewController: viewController, args: ()) { view, args in
            execute(view)
        }
    }

}
