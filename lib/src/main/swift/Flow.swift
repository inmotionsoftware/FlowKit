//
//  Flow.swift
//  FlowKit
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import Foundation
import PromiseKit

public protocol Flow {
    associatedtype Input
    associatedtype Output
    func startFlow(context: Input) -> Promise<Output>
}

extension Flow where Input == Void {
    func startFlow() -> Promise<Output> {
        return self.startFlow(context: ())
    }
}

public enum FlowError: Error {
    case canceled
    case back
}

public protocol Backable {
    func back()
}

public protocol Cancelable {
    func cancel()
}

public protocol Resolvable {
    associatedtype Result

    var isPending: Bool { get }
    var isResolved: Bool { get }
    var isRejected: Bool { get }
    func resolve(_ result: Result)
    func reject(_ error: Error)
}

public extension Resolvable where Self: Backable {
    func back() { self.reject(FlowError.back) }
}

public extension Resolvable where Self: Cancelable {
    func cancel() { self.reject(FlowError.canceled) }
}

public protocol Reusable {
    func prepareForReuse()
}

import SwiftUI


public protocol MoveDelegate {
    func willMove(toParent parent: UIViewController?)
}

public protocol BackButtonDelegate: class {
    func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool
}


public typealias ViewControllerDelegate = (BackButtonDelegate & MoveDelegate)

public protocol ViewFlow: SwiftUI.View, Flow {

}

public protocol ViewControllerFlow: UIViewController, Flow, ViewControllerDelegate {
    var delegate: ViewControllerDelegate? { get set }
}

public class ViewSubflow<View: ViewFlow>: Flow {
    public typealias Input = View.Input
    public typealias Output = View.Output

    private let view: View
    private let nav: UINavigationController

    public init(view: View, nav: UINavigationController) {
        self.view = view
        self.nav = nav
    }

    public func startFlow(context: Input) -> Promise<Output> {
        let blah = Blah.init(rootView: self.view)
        let sub = ViewControllerSubflow.init(viewController: blah, nav: self.nav)
        return sub.startFlow(context: context)
    }
}

/**
    This is a bit of hack to get global notifications of all navigation controller changes. We use this to forward
    back button notifications
 */
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

public class Blah<Content: View & ViewFlow>: UIHostingController<Content>, Flow, ViewControllerFlow {

    public typealias Input = Content.Input
    public typealias Output = Content.Output

    public weak var delegate: ViewControllerDelegate? = nil

    public func startFlow(context: Content.Input) -> Promise<Content.Output> {
        return self.rootView.startFlow(context: context)
    }

    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        return self.delegate?.navigationController(navigationController, shouldPop: viewController) ?? true
    }

    override public func willMove(toParent parent: UIViewController?) {
        self.delegate?.willMove(toParent: parent)
    }
}

public class ViewControllerSubflow<View: ViewControllerFlow>: Flow, ViewControllerDelegate {
    public typealias Input = View.Input
    public typealias Output = View.Output

    private(set) public var nav: UINavigationController
    private weak var delegate: ViewControllerDelegate? = nil
    private let promise = Promise<Output>.pending()
    private let viewController: View

    public init(viewController: View, nav: UINavigationController) {
        self.viewController = viewController
        self.nav = nav
    }

    public func startFlow(context: Input) -> Promise<Output> {
        self.viewController.delegate = self
        return startFlow(context: context, animated: true)
    }

    public func startFlow(context: Input, animated: Bool) -> Promise<Output> {
        // listen for back button delegates
        self.delegate = self

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
            .done { self.promise.resolver.fulfill($0) }
            .catch { self.promise.resolver.reject($0) }

        return promise.promise.ensure {
            guard self.viewController != self.nav.topViewController else { return }
            self.nav
                .beginOrAmmendTransaction(animated: true)
                .popTo(viewController: self.viewController)
        }
    }
}

extension ViewControllerSubflow {
    private func cancel() {
        self.promise.resolver.reject(FlowError.canceled)
    }

    private func back() {
        self.promise.resolver.reject(FlowError.back)
    }

    public func willMove(toParent parent: UIViewController?) {
        // Cancel our promise if the view controller is being popped from the
        // view stack
        if parent == nil && self.promise.promise.isPending {
            self.cancel()
        }
    }

    private func shouldPop(_ navigationController: UINavigationController) -> Bool {
        guard self.promise.promise.isPending else { return false }
        self.back()
        navigationController
            .beginOrAmmendTransaction(animation: .back)
            .popViewController()
        return true
    }

    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        // if we handled the pop, tell the delegate not to
        if self.shouldPop(navigationController) { return false }
        return self.delegate?.navigationController(navigationController, shouldPop: viewController) ?? true
    }
}
