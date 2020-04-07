//
//  DeferredPromise.swift
//  FlowKit
//
//  Created by Brian Howard on 6/14/17.
//  Copyright © 2017 InMotion Software, LLC. All rights reserved.
//

import PromiseKit

internal struct FlowResolver<T> {
    public typealias Result = FlowResult<T>

    private(set) var promise: FlowPromise<T>!
    fileprivate var resolver: Resolver<Result>!
    
    public init() {
        self.init(initializer: nil)
    }
    
    public init(initializer: (() throws -> Void)? ) {
        self.resolver = nil
        self.promise = FlowPromise { resolver -> Void in
            self.resolver = resolver

            if let initializer = initializer {
                try initializer()
            }
        }
    }

    public func resolve(_ result: Result) {
        self.resolver.fulfill(result)
    }
}

extension FlowResolver: Resolvable {
    public func resolve(_ value: T) {
        self.resolver.fulfill(.complete(result: value))
    }
    
    public func reject(_ error: Error) {
        self.resolver.reject(error)
    }
}

extension FlowResolver: Backable {
    public func back() {
        self.resolve(.back)
    }
}

extension FlowResolver: Cancelable {
    public func cancel() {
        self.resolve(.cancel)
    }
}
