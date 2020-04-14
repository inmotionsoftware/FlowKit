//
//  Flow.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import SwiftUI
import PromiseKit

public protocol Resolvable {
    associatedtype Output
    var resolver: Resolver<Output> { get }
}

public protocol FlowableView: SwiftUI.View, Resolvable {
    associatedtype Input
    associatedtype Output
    init(context: Input, resolver: PromiseKit.Resolver<Output>)
}

public extension FlowableView where Input == Void {
    init(resolver: PromiseKit.Resolver<Output>) {
        self.init(context: (), resolver: resolver)
    }
}

public extension Resolvable {
    func resolve(_ value: Output) {
        self.resolver.fulfill(value)
    }

    func reject(_ error: Error) {
        self.resolver.reject(error)
    }

    func cancel() {
        self.reject(FlowError.canceled)
    }

    func back() {
        self.reject(FlowError.back)
    }
}

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
