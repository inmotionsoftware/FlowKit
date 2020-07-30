//
//  StateMachine.swift
//  FlowKit
//
//  Created by Brian Howard on 4/13/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import PromiseKit
import os

public protocol FlowState {}

public protocol StateFactory {
    associatedtype State: FlowState
    associatedtype Input
    func createState(context: Input) -> State
    func createState(error: Error) -> State
}

public protocol StateMachine: StateFactory {
    associatedtype Output
    typealias Result = Swift.Result<Output, Error>

    func dispatch(prev: State, state: State) -> Promise<State>
    func getResult(state: State) -> Result?
    func onTerminate(state: State, context: Result) -> Promise<Output>
}

public extension StateMachine {
    func onTerminate(state: State, context: Result) -> Promise<Output> {
        switch (context) {
        case .success(let out): return Promise.value(out)
        case .failure(let err): return Promise(error: err)
        }
    }
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
        let begin = self.stateMachine.createState(context: context)
        return self.jumpToState(state: begin)
    }

    fileprivate func jumpToState(state: State) -> Promise<Output> {
        return nextState(prev: state, curr: state)
            .map {
                switch($0) {
                case .success(let val): return val
                case .failure(let err): throw err
                }
            }
    }

    fileprivate func nextState(prev: State, curr: State) -> Promise<Swift.Result<Output, Error>> {
        self.delegate.map { $0(prev, curr) }
        guard let result = stateMachine.getResult(state: curr) else {
        return self.stateMachine.dispatch(prev: prev, state: curr)
            .then { self.nextState(prev: curr, curr: $0) }
            .recover { self.nextState(prev: curr, curr: self.stateMachine.createState(error: $0)) }
        }

        return self.stateMachine.onTerminate(state: curr, context: result)
            .map {.success($0) }
            .recover { Promise.value(.failure($0)) }
    }
}

public extension StateMachine {
    func subflow<SM: StateMachine>(to stateMachine: SM, context: SM.Input) -> Promise<SM.Output> {
        return StateMachineHost(stateMachine: stateMachine)
            .startFlow(context: context)
    }

    func subflow<SM: StateMachine>(to stateMachine: SM, state: SM.State) -> Promise<SM.Output> {
        return StateMachineHost(stateMachine: stateMachine)
            .jumpToState(state: state)
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
