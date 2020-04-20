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

public extension Flow where Input == Void {
    func startFlow() -> Promise<Output> {
        return self.startFlow(context: ())
    }
}

public enum FlowError: Error {
    case canceled
    case back
}

public protocol Resolvable {
    associatedtype Output
    var resolver: Resolver<Output> { get }
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

public enum Bootstrap {

}
