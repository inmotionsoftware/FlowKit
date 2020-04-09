//
//  FlowHostingController.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import SwiftUI
import PromiseKit

public class FlowHostingController<Content: View>: UIHostingController<Content>, BackableView, FlowViewController where Content: Flow, Content: Reusable {

    public typealias Input = Content.Input
    public typealias Output = Content.Output
    public typealias Result = Content.Output
    public weak var backDelegate: BackButtonDelegate? = nil

    private let resolver = Promise<Output>.pending()
    public var isPending: Bool { return self.resolver.promise.isPending }
    public var isResolved: Bool { return self.resolver.promise.isResolved }
    public var isRejected: Bool { return self.resolver.promise.isRejected }
    public func resolve(_ result: Content.Output) { self.resolver.resolver.fulfill(result) }
    public func reject(_ error: Error) { self.resolver.resolver.reject(error) }

    public func startFlow(context: Input) -> Promise<Output> {
        self.rootView.startFlow(context: context)
        .done { self.resolver.resolver.fulfill($0) }
        .catch { self.resolver.resolver.reject($0) }
        return resolver.promise
    }

    private func shouldPop(_ navigationController: UINavigationController) -> Bool {
        guard self.resolver.promise.isPending else { return false }
        self.resolver.resolver.reject(FlowError.back)
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

    public func prepareForReuse() {
        self.rootView.prepareForReuse()
    }
}
