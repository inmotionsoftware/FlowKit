//
//  DeferredPromise.swift
//  FlowKit
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import PromiseKit

public struct Resolver<Output>: Resolvable {
    private let proxy = Promise<Output>.pending()

    public var promise: Promise<Output> {
        return proxy.promise
    }

    public func reject(_ error: Error) {
        proxy.resolver.reject(error)
    }

    public func resolve(_ result: Output) {
        proxy.resolver.fulfill(result)
    }
}

public class ProxyFlow<Output>: Resolvable {
    fileprivate var resolver: Resolver<Output> = Resolver() {
        didSet {
            // cancel out existing promise...
            if oldValue.promise.isPending {
                oldValue.reject(FlowError.canceled)
            }
        }
    }

    public var promise: Promise<Output> { return self.resolver.promise }
    public var isPending: Bool { return self.resolver.promise.isPending }
    public var isResolved: Bool { return self.resolver.promise.isResolved }
    public var isRejected: Bool { return self.resolver.promise.isRejected }

    public init() {}

    deinit { // just in case...
        if self.resolver.promise.isPending {
            self.resolver.reject(FlowError.canceled)
        }
    }

    final public func startFlow( initializer: () -> Void ) -> Promise<Output> {
        let resolver = Resolver<Output>()
        self.resolver = resolver
        initializer()
        return resolver.promise
    }

    public func reject(_ error: Error) {
        guard self.isPending else {
            NSLog("WARNING: Promise has already been resolved.")
            return
        }
        self.resolver.reject(error)
    }

    public func resolve(_ value: Output) {
        guard self.isPending else {
            NSLog("WARNING: Promise has already been resolved.")
            return
        }
        self.resolver.resolve(value)
    }
}

extension ProxyFlow: Backable {}
extension ProxyFlow: Cancelable {}
