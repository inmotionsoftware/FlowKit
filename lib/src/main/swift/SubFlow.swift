// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a 
// copy of this software and associated documentation files (the "Software"), 
// to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, 
// and/or sell copies of the Software, and to permit persons to whom the 
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.
//
//  SubFlow.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
#if os(iOS)

import PromiseKit
import UIKit

internal class SubFlow<View: FlowViewController>: Flow, ViewControllerDelegate {
    public typealias Input = View.Input
    public typealias Output = View.Output

    private(set) public var nav: UINavigationController
    private weak var delegate: ViewControllerDelegate? = nil

    private var viewController: View
    private let animated: Bool

    private var proxy = Promise<Output>.pending()

    deinit {
        if (proxy.promise.isPending) {
            proxy.resolver.reject(FlowError.canceled)
        }
    }

    public init(viewController: View, nav: UINavigationController, animated: Bool) {
        self.viewController = viewController
        self.nav = nav
        self.animated = animated
    }

    public func startFlow(context: Input) -> Promise<Output> {
        self.viewController.delegate = self
        // listen for back button delegates
        if (!proxy.promise.isPending) {
            self.proxy = Promise<Output>.pending()
        }

        // push on the stack
        if let trans = self.nav.currentTransaction {
            trans.popToOrPush(viewController: self.viewController)
            trans.commit()
        } else {
            self.nav.popToOrPush(viewController: self.viewController, animated: animated)
        }

        // force load of the view
        let _ = self.viewController.view

        self.viewController.startFlow(context: context)
            .map { val -> Void in self.proxy.resolver.fulfill(val) }
            .catch { self.proxy.resolver.reject($0) }

        return proxy.promise
    }
}

extension SubFlow {
    public func willMove(toParent parent: UIViewController?) {
        // Cancel our promise if the view controller is being popped from the
        // view stack
        if parent == nil && self.proxy.promise.isPending {
            self.proxy.resolver.reject(FlowError.canceled)
        }
    }

    private func shouldPop(_ navigationController: UINavigationController) -> Bool {
        if self.proxy.promise.isPending {
            self.proxy.resolver.reject(FlowError.back)
        }
        return false
    }

    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        // if we handled the pop, tell the delegate not to
        if self.shouldPop(navigationController) { return false }
        return self.delegate?.navigationController(navigationController, shouldPop: viewController) ?? false
    }
}

#endif
