//
//  Promise+Extensions.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import PromiseKit
import os

@propertyWrapper
public class DeferredPromise<Output> {
    public var pending = Promise<Output>.pending() {
        didSet {
            if (oldValue.promise.isPending) {
                os_log("Canceling dangling promise", type: .error)
                oldValue.resolver.reject(FlowError.canceled)
            }
        }
    }

    public var projectedValue: Resolver<Output> { return pending.resolver }
    public var wrappedValue: Promise<Output> { return self.pending.promise }

    public init() {}
    deinit {
        if (wrappedValue.isPending) {
            os_log("Canceling dangling promise", type: .error)
            pending.resolver.reject(FlowError.canceled)
        }
    }

    public func reset() {
        if (self.wrappedValue.isResolved) {
            print("resetting promise")
            self.pending = Promise.pending()
        }
    }

    public func resolve(_ value: Output) {
        if (self.wrappedValue.isResolved) {
            os_log("Attempting to fullfill a resolved promise", type: .error)
        }
        self.projectedValue.fulfill(value)
    }

    public func reject(_ error: Error) {
        if (self.wrappedValue.isResolved) {
            os_log("Attempting to reject a resolved promise", type: .error)
        }
        self.projectedValue.reject(error)
    }
}
