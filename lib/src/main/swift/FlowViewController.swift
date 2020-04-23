//
//  Flow.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

#if os(iOS)

import UIKit
import PromiseKit

/**
    This is a bit of hack to get global notifications of all navigation controller changes. We use this to forward
    back button notifications
 */
extension UINavigationController: UINavigationBarDelegate {
    public func navigationBar(_ navigationBar: UINavigationBar, shouldPop item: UINavigationItem) -> Bool {
        guard viewControllers.count >= (navigationBar.items?.count ?? 0) else { return true }
        guard let top = self.topViewController else { return true }
        guard top.navigationItem == item else { return false }

        guard let delegate = top as? BackDelegate else {
            self.popViewController(animated: true) // TODO: should we animate?
            return false
        }
        return delegate.navigationController(self, shouldPop: top)
    }
}

public protocol MoveDelegate {
    func willMove(toParent parent: UIViewController?)
}

public protocol BackDelegate: class {
    func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool
}

public typealias ViewControllerDelegate = (BackDelegate & MoveDelegate)


public protocol FlowViewController: UIViewController, Flow, ViewControllerDelegate {
    var delegate: ViewControllerDelegate? { get set }
}

public extension FlowViewController {
    func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        return self.delegate?.navigationController(navigationController, shouldPop: viewController) ?? false
    }

    func willMove(toParent parent: UIViewController?) {
        self.delegate?.willMove(toParent: parent)
    }
}

open class BaseFlowViewController<Input, Output>: UIViewController, FlowViewController {

    public typealias Input = Input
    public typealias Output = Output

    private var proxy = Promise<Output>.pending()
    public var delegate: ViewControllerDelegate?

    open func startFlow(context: Input) -> Promise<Output> {
        if (!proxy.promise.isPending) {
            proxy = Promise<Output>.pending()
        }
        do {
            try onBegin(context: context)
        } catch(let e) {
            proxy.resolver.reject(e)
        }
        return proxy.promise
    }

    override open func willMove(toParent parent: UIViewController?) {
        self.delegate?.willMove(toParent: parent)
        super.willMove(toParent: parent)
    }

    open func onBegin(context: Input) throws {}

    public func resolve(_ value: Output) {
        self.proxy.resolver.fulfill(value)
    }

    public func reject(_ error: Error) {
        self.proxy.resolver.reject(error)
    }

    public func cancel() {
        self.reject(FlowError.canceled)
    }

    public func back() {
        self.reject(FlowError.back)
    }
}

#endif
