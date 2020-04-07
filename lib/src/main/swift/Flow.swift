//
//  Flow.swift
//  FlowKit
//
//  Created by Khuong Huynh on 3/21/17.
//  Copyright © 2017 InMotion Software, LLC. All rights reserved.
//

import Foundation
import PromiseKit


public protocol FlowState: StateType {
    typealias Transition = FSMTransition<Self>
}

public protocol FlowResultBase {
    associatedtype Result
    var isBack: Bool { get }
    var isCancel: Bool { get }
    var isComplete: (Bool,Result?) { get }
}

public enum FlowResult<Return> {
    case complete(result: Return)
    case back
    case cancel
}

extension FlowResult: FlowResultBase {
    public typealias Result = Return

    public var isBack: Bool {
        switch(self) {
            case .back: return true
            default: return false
        }
    }

    public var isCancel: Bool {
        switch(self) {
            case .cancel: return true
            default: return false
        }
    }

    public var isComplete: (Bool,Result?) {
        switch(self) {
            case .complete(let t): return (true, t)
            default: return (false, nil)
        }
    }
}

public typealias FlowPromise<Result> = Promise<FlowResult<Result>>

public protocol Backable {
    func back()
}

public protocol Cancelable {
    func cancel()
}

public protocol Resolvable {
    associatedtype Result
    func resolve(_ result: Result)
    func reject(_ error: Error)
}

public protocol Flow: Resolvable, Cancelable, Backable where Result == Return {
    associatedtype Args
    associatedtype Return
    

    func reject(_ error: Error)
    func resolve(_ value: Return)
    func cancel()
    func back()

    func run(args: Args) -> FlowPromise<Return>
}

extension Flow where Args == Void {
    func run() -> FlowPromise<Return> {
        return run(args: ())
    }
}
