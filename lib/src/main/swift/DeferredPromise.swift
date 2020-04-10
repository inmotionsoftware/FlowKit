//
//  Promise+Extensions.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import PromiseKit

@propertyWrapper
public class DeferredPromise<Output> {
    public var pending = Promise<Output>.pending() {
        didSet {
            if (oldValue.promise.isPending) {
                oldValue.resolver.reject(FlowError.canceled)
            }
        }
    }

    public var projectedValue: Resolver<Output> { return pending.resolver }
    public var wrappedValue: Promise<Output> { return self.pending.promise }

    public init() {}
    deinit {
        if (wrappedValue.isPending) { pending.resolver.reject(FlowError.canceled) }
    }

    public func reset() {
        self.pending = Promise.pending()
    }

    public func resolve(_ value: Output) {
        self.projectedValue.fulfill(value)
    }

    public func reject(_ error: Error) {
        self.projectedValue.reject(error)
    }
}
