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

public protocol Backable {
    func back()
}

public protocol Cancelable {
    func cancel()
}

public protocol Resolvable {
    associatedtype Result

    var isPending: Bool { get }
    var isResolved: Bool { get }
    var isRejected: Bool { get }
    func resolve(_ result: Result)
    func reject(_ error: Error)
}

public extension Resolvable where Self: Backable {
    func back() { self.reject(FlowError.back) }
}

public extension Resolvable where Self: Cancelable {
    func cancel() { self.reject(FlowError.canceled) }
}

public protocol Reusable {
    func prepareForReuse()
}
