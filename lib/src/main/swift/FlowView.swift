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
//  Flow.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
#if os(iOS)

import SwiftUI
import PromiseKit

@available(iOS 13.0, OSX 10.15, tvOS 13.0, watchOS 6.0, *)
public protocol FlowableView: SwiftUI.View, Resolvable {
    associatedtype Input
    associatedtype Output
    init(context: Input, resolver: PromiseKit.Resolver<Output>)
}

@available(iOS 13.0, OSX 10.15, tvOS 13.0, watchOS 6.0, *)
public extension FlowableView where Input == Void {
    init(resolver: PromiseKit.Resolver<Output>) {
        self.init(context: (), resolver: resolver)
    }
}

@available(iOS 13.0, OSX 10.15, tvOS 13.0, watchOS 6.0, *)
public class FlowHostingController<Content: FlowableView>: UIHostingController<Content>, FlowViewController {
    public typealias Input = Content.Input
    public typealias Output = Content.Output

    public weak var delegate: ViewControllerDelegate? = nil

    private var proxy = Promise<Output>.pending()

    public override init(rootView: Content) {
        super.init(rootView: rootView)
    }

    public init(context: Input) {
        let view = Content(context: context, resolver: proxy.resolver)
        super.init(rootView: view)
    }

    @objc required dynamic init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    deinit {
        if (proxy.promise.isPending) {
            proxy.resolver.reject(FlowError.canceled)
        }
    }

    public func startFlow(context: Content.Input) -> Promise<Content.Output> {
        if (proxy.promise.isResolved) {
            proxy = Promise.pending()
        }
        self.rootView = Content(context: context, resolver: proxy.resolver)
        return proxy.promise
    }

    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        return self.delegate?.navigationController(navigationController, shouldPop: viewController) ?? true
    }

    override public func willMove(toParent parent: UIViewController?) {
        self.delegate?.willMove(toParent: parent)
    }
}

@available(iOS 13.0, OSX 10.15, tvOS 13.0, watchOS 6.0, *)
public extension NavStateMachine where Self: ViewCacher {
    func subflow<View: FlowableView>(to view: View.Type, context: View.Input, animated: Bool = true, transition: UIViewControllerTransitioningDelegate? = nil) -> Promise<View.Output> {
        let host = getCache(type: FlowHostingController<View>.self) { FlowHostingController<View>(context: context) }
        host.transitioningDelegate = transition
        return self.subflow(to: host, context: context, animated: animated)
    }
}

@available(iOS 13.0, OSX 10.15, tvOS 13.0, watchOS 6.0, *)
public extension NavStateMachine {
    func subflow<View: FlowableView>(to view: View, context: View.Input, animated: Bool = true, transition: UIViewControllerTransitioningDelegate? = nil) -> Promise<View.Output> {
        let host = FlowHostingController<View>(context: context)
        host.transitioningDelegate = transition
        return self.subflow(to: host, context: context, animated: animated)
    }
}

@available(iOS 13.0, OSX 10.15, tvOS 13.0, watchOS 6.0, *)
extension UINavigationController {
    @discardableResult
    func popToOrPush<Content: View, T:UIHostingController<Content>>(viewController: T, animated: Bool = true) -> T {
        let top = self.topViewController
        guard top != viewController else { return top as! T }
        if let found = self.viewControllers.first(where: { $0 is UIHostingController<Content> }) {
            self.popToViewController(found, animated: animated)
            return found as! T
        } else {
            self.pushViewController(viewController, animated: animated)
        }
        return viewController
    }
}

#endif
