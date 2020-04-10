//
//  Flow.swift
//  FlowKit
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import Foundation
import PromiseKit

public protocol Flow {
    associatedtype Input
    associatedtype Output
    func startFlow(context: Input) -> Promise<Output>
}

extension Flow where Input == Void {
    func startFlow() -> Promise<Output> {
        return self.startFlow(context: ())
    }
}

public enum FlowError: Error {
    case canceled
    case back
}


public protocol FlowResolver {
    associatedtype Output
    var proxy: DeferredPromise<Output> { get }
}

public extension FlowResolver {

    func cancel() {
        self.reject(FlowError.canceled)
    }

    func back() {
        self.reject(FlowError.back)
    }

    func resolve(_ value: Output) {
        self.proxy.resolve(value)
    }

    func reject(_ error: Error) {
        self.proxy.reject(error)
    }
}
