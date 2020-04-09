//
//  FlowTableViewController.swift
//  FlowKit
//
//  Created by Khuong Huynh on 8/12/17.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import UIKit
import PromiseKit
//
//open class FlowTableViewController<Input, Output>: UITableViewController, BaseFlowViewController, BackableView {
//    public weak var backDelegate: BackButtonDelegate?
//
//    private var proxy = ProxyFlow<Output>()
//    public var isPending: Bool { return self.proxy.isPending }
//    public var isResolved: Bool { return self.proxy.isResolved }
//    public var isRejected: Bool { return self.proxy.isRejected }
//    public func resolve(_ result: Output) { self.proxy.resolve(result) }
//    public func reject(_ error: Error) { self.proxy.reject(error) }
//
//    final public func startFlow(context: Input) -> Promise<Output> {
//        prepareForReuse()
//        return proxy.startFlow {}
//    }
//
//    open func prepareForReuse() {}
//
//    override open func willMove(toParent parent: UIViewController?) {
//        // Cancel our promise if the view controller is being popped from the
//        // view stack
//        if parent == nil && self.isPending {
//            self.cancel()
//        }
//        super.willMove(toParent: parent)
//    }
//}
//
//extension FlowTableViewController: BackButtonDelegate {
//    private func shouldPop(_ navigationController: UINavigationController) -> Bool {
//        guard self.isPending else { return false }
//        self.back()
//        navigationController
//            .beginOrAmmendTransaction(animation: .back)
//            .popViewController()
//        return true
//    }
//
//    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
//        // if we handled the pop, tell the delegate not to
//        if self.shouldPop(navigationController) { return false }
//
//        // check the view controller delegate
//        guard let delegate = self.backDelegate else { return true }
//        return delegate.navigationController(navigationController, shouldPop: viewController)
//    }
//}
