//
//  StateMachine.swift
//  FlowKit
//
//  Created by Brian Howard on 4/13/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import PromiseKit
import os

public protocol StateMachine {
    associatedtype State
    associatedtype Input
    associatedtype Output
    typealias Result = Swift.Result<Output, Error>

    func dispatch(state: State) -> Promise<State>
    func terminal(state: State) -> Result?
    func firstState(context: Input) -> State
}

public protocol StateMachineDelegate {
    associatedtype State
    func stateDidChange(from: State, to: State)
}

public struct StateMachineHost<SM: StateMachine>: Flow {
    public typealias Input = SM.Input
    public typealias Output = SM.Output
    public typealias State = SM.State
    typealias Delegate = (State, State) -> Void

    private let delegate: Delegate?
    public let stateMachine: SM

    public init<D: StateMachineDelegate>(stateMachine: SM, delegate: D) where D.State == State {
        self.stateMachine = stateMachine
        self.delegate = { delegate.stateDidChange(from: $0, to: $1) }
    }

    public init(stateMachine: SM) {
        self.stateMachine = stateMachine
        self.delegate = nil
    }

    public func startFlow(context: Input) -> Promise<Output> {
        let begin = self.stateMachine.firstState(context: context)
        return self
            .nextState(prev: begin, curr: begin)
            .map {
                switch ($0) {
                case .success(let out): return out
                case .failure(let err): throw err
                }
            }
    }

    fileprivate func nextState(prev: State, curr: State) -> Promise<SM.Result> {
        if let d = self.delegate { d(prev, curr) }
        guard let result = stateMachine.terminal(state: curr) else {
            return self.stateMachine.dispatch(state: curr).then { self.nextState(prev: curr, curr: $0) }
        }

        return Promise.value(result)
    }
}

public extension StateMachine {
    func subflow<SM: StateMachine>(to stateMachine: SM, context: SM.Input) -> Promise<SM.Output> {
        return StateMachineHost(stateMachine: stateMachine)
            .startFlow(context: context)
    }
}

public extension Bootstrap {
    static func startFlow<SM: StateMachine>(stateMachine: SM, context: SM.Input) {
        let _ = StateMachineHost(stateMachine: stateMachine)
            .startFlow(context: context)
            .ensure {
                os_log("Root flow is being restarted", type: .error)
                startFlow(stateMachine: stateMachine, context: context)
        }
    }
}
