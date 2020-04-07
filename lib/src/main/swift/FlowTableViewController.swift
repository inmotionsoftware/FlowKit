//
//  FlowTableViewController.swift
//  FlowKit
//
//  Created by Khuong Huynh on 8/12/17.
//  Copyright © 2017 InMotion Software, LLC. All rights reserved.
//

import UIKit
import PromiseKit

open class FlowTableViewController<Args,Return>: UITableViewController, Flow {
    
    internal weak var backDelegate: BackButtonDelegate?
    
    fileprivate var promise: FlowResolver<Return>? {
        didSet {
            // cancel out existing promise...
            if let p = oldValue, p.promise.isPending {
                p.resolve(.cancel)
            }
        }
    }
    
    deinit { // just in case...
        self.promise = nil
    }
    
    public var isPending: Bool {
        guard let promise = self.promise else { return false }
        guard let p = promise.promise else { return false }
        return p.isPending
    }

    public var isResolved: Bool {
        guard let promise = self.promise else { return false }
        guard let p = promise.promise else { return false }
        return p.isResolved
    }

    public func run(args: Args) -> FlowPromise<Return> {
        let p = FlowResolver<Return>() { try self.flowWillRun(args: args) }
        self.promise = p
        return p.promise
    }
    
    open func prepareForReuse() {}
    open func flowWillRun(args: Args) throws {}
    
    override open func willMove(toParent parent: UIViewController?) {
        // Cancel our promise if the view controller is being popped from the
        // view stack
        if parent == nil && self.isPending {
            self.cancel()
        }
        super.willMove(toParent: parent)
    }
}

extension FlowTableViewController: BackButtonDelegate {
    private func shouldPop(_ navigationController: UINavigationController) -> Bool {
        guard self.isPending else { return false }
        self.back()
        navigationController
            .beginOrAmmendTransaction(animation: .back)
            .popViewController()
        return true
    }
    
    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        // if we handled the pop, tell the delegate not to
        if self.shouldPop(navigationController) { return false }
        
        // check the view controller delegate
        guard let delegate = self.backDelegate else { return true }
        return delegate.navigationController(navigationController, shouldPop: viewController)
    }
}

fileprivate extension FlowTableViewController {
    func _resolve(result: FlowResult<Return>) {
        guard let promise = self.promise else {
            NSLog("WARNING: %@", "Promise is nil. It either has not been created via a flow() method or has been cancelled.")
            return
        }
        guard promise.promise.isPending else {
            NSLog("WARNING: %@", "Promise has already been resolved.")
            return
        }
        promise.resolve(result)
        self.promise = nil // clear out the promise now...
    }
}

// Resolving
extension FlowTableViewController: Resolvable {
    public func reject(_ error: Error) {
        guard let promise = self.promise else {
            NSLog("WARNING: %@", "Promise is nil. It either has not been created via a flow() method or has been cancelled.")
            return
        }
        promise.reject(error)
        self.promise = nil // clear out the promise now...
    }
    
    public func resolve(_ value: Return) {
        self._resolve(result: .complete(result: value))
    }
}

extension FlowTableViewController: Cancelable {
    public func cancel() {
        self._resolve(result: .cancel)
    }
}

extension FlowTableViewController: Backable {
    public func back() {
        self._resolve(result: .back)
    }
}

public extension FlowTableViewController {
    
    func resolvePromise(_ result: Return) {
        self.resolve(result)
    }
    
    func cancelPromise(_ result: Return) {
        self.cancel()
    }
    
    func cancelFlow() {
        self.cancel()
    }
    
    func rejectPromise(_ error: Error) {
        self.reject(error)
    }
}

extension FlowTableViewController where Args == Void  {
    open func run() -> FlowPromise<Return> {
        return self.run(args: ())
    }
}
