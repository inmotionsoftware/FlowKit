//
//  FlowController.swift
//  FlowKit
//
//  Created by Khuong Huynh on 3/21/17.
//  Copyright © 2017 HEB. All rights reserved.
//

import UIKit
import PromiseKit

open class FlowController<S: FlowState, Input, Output>
        : Flow
        , Cancelable {

    public typealias State = S
    public typealias Args = Input
    public typealias Return = Output

    public typealias EventManager = FlowController
    public typealias StateMachine = FiniteStateMachine<State>
    public typealias Builder = StateMachine.Builder
    public typealias Transition = State.Transition

    fileprivate(set) public var stateMachine: StateMachine?
    fileprivate var promise: FlowResolver<Return>?

    public typealias StateListener = (State,Any?) -> Void
    public typealias Listener = (Transition,Any?) -> Void
    public typealias TransitionListener = (Transition,Any?) -> Void

    fileprivate var stateListeners = [State:[StateListener]]()
    fileprivate var transitionListeners = [Transition:[TransitionListener]]()
    fileprivate var listeners: [Listener] = []

    open func run(args: Args) -> FlowPromise<Return> {

        let builder = Builder()
        self.register(states: builder)
        let stateMachine = builder.build()
        stateMachine.delegate = AnyDelegate(self)
        self.register(events: self)
        self.stateMachine = stateMachine

        let resolver = FlowResolver<Return>() {
            try self.flowWillRun()
        }
        self.promise = resolver
        return resolver.promise
    }

    open func flowWillRun() throws { try onRun() }
    open func onRun() throws {}

    open func register(states: Builder) {
        assertionFailure("")
    }

    open func register(events: EventManager) {
        assertionFailure("")
    }

    public func cancel() {
        guard let promise = self.promise else { return }
        promise.resolve(.cancel)
        self.stateMachine = nil
    }
}

// MARK: - transitions
//------------------------------------------------------------------------------
public extension FlowController {
    @discardableResult
    func transitionAsync(from: State, to: State, with: Any? = nil) -> Promise<Void> {
        return Promise.value(from → to).then { Promise.value(try self.transition(from: $0.from, to: $0.to, with: with)) }
    }

    func transition(from: State, to: State, with: Any? = nil) throws -> Void {
        let fsm = self.stateMachine!
        guard fsm.state == from else { throw FSMError<State>.concurrentTransition }
        try fsm.transition(to: to, with: with)
    }

    func transition(fromAnyTo to: State, with: Any? = nil) throws -> Void {
        let fsm = self.stateMachine!
        try fsm.transition(to: to, with: with)
    }

    func transition(to: State, with: Any? = nil) throws -> Void {
        let fsm = self.stateMachine!
        try fsm.transition(to: to, with: with)
    }

    @discardableResult
    func transitionAsync(to: State, with: Any? = nil) -> Promise<Void> {
        return Promise.value(to).then { Promise.value(try self.transition(to: $0, with: with)) }
    }  
}

// MARK: - resolving the controller
//------------------------------------------------------------------------------
extension FlowController: Resolvable {
    public func resolve(_ value: Return) {
        guard let promise = self.promise else { return }
        promise.resolve(.complete(result: value))
        self.stateMachine = nil
    }

    public func reject(_ error: Swift.Error) {
        guard let promise = self.promise else { return }
        promise.reject(error)
        self.stateMachine = nil
    }
}

extension FlowController: Backable {
    public func back() {
        guard let promise = self.promise else { return }
        promise.resolve(.back)
        self.stateMachine = nil
    }
}

public extension FlowController {
    func finish(result: Return) {
        self.resolve(result)
    }
}

// MARK: - Common listener methods
//------------------------------------------------------------------------------
public extension FlowController {
    func onCancel(state: State, with: Any?) {
        self.cancel()
    }

    func onBack(state: State, with: Any?) {
        self.back()
    }
    
    func onError(state: State, with error: Swift.Error) {
        self.reject(error)
    }
}

// MARK: - Void convenience
//------------------------------------------------------------------------------
public extension FlowController where Args == Void {
    func run() -> FlowPromise<Return> {
        return self.run(args: ())
    }
}


// MARK: - On STATE events
//------------------------------------------------------------------------------
extension FlowController {
    @discardableResult
    public func on<F:FlowController>(state: State, execute: @escaping (F) -> (State) -> Void) -> Self {
        return on(state: state) { [unowned self] (state, any: Any?) in
            guard let this = self as? F else { return }
            execute(this)(state)
        }
    }

    @discardableResult
    public func on<F:FlowController, T:ExpressibleByNilLiteral>(state: State, execute: @escaping (F) -> (State,T) -> Void) -> Self {
        return on(state: state) { [unowned self] (state, any: Any?) in
            guard let this = self as? F else { return }
            guard let nonil = any else { execute(this)(state, nil); return }
            execute(this)(state, nonil as? T ?? nil)
        }
    }

    @discardableResult
    public func on<F:FlowController, T>(state: State, execute: @escaping (F) ->(State,T) -> Void) -> Self {
        return on(state: state) { [unowned self] (state, any: Any?) in
            guard let this = self as? F else { return }
            guard let obj = any as? T else { return }
            execute(this)(state, obj)
        }
    }

    @discardableResult
    public func on<F:FlowController>(state: State, execute: @escaping (F) ->(State,Any?) -> Void) -> Self {
        return on(state: state) { [unowned self] state, any in
            guard let this = self as? F else { return }
            execute(this)(state, any)
        }
    }

    @discardableResult
    private func on(state: State, execute: @escaping StateListener) -> Self {
        if var listener = self.stateListeners[state] {
            listener.append(execute)
            self.stateListeners[state] = listener
        } else {
            var e = [StateListener]()
            e.append(execute)
            self.stateListeners[state] = e
        }
        return self
    }
}

// MARK: - on Global events
//------------------------------------------------------------------------------
extension FlowController {
    @discardableResult
    public func any<F:FlowController>(execute: @escaping (F) -> (Transition) -> Void) -> Self {
        return any{ [unowned self] (transition: Transition, any: Any?) in
            guard let this = self as? F else { return }
            execute(this)(transition)
        }
    }

    @discardableResult
    public func any<F:FlowController, T>(execute: @escaping (F) -> (Transition,T) -> Void) -> Self {
        return any{ [unowned self] (transition: Transition, any: Any?) in
            guard let this = self as? F else { return }
            guard let obj = any as? T else { return }
            execute(this)(transition, obj)
        }
    }

    @discardableResult
    public func any<F:FlowController, T:ExpressibleByNilLiteral>(execute: @escaping (F) -> (Transition,T) -> Void) -> Self {
        return any{ [unowned self] (transition, any: Any?) -> Void in
            guard let this = self as? F else { return }
            guard let nonil = any else { execute(this)(transition, nil); return }
            execute(this)(transition, nonil as? T ?? nil)
        }
    }

    @discardableResult
    public func any<F:FlowController>(execute: @escaping (F) -> (Transition,Any?) -> Void) -> Self {
        return any{ [unowned self] trans, any -> Void in
            guard let this = self as? F else { return }
            execute(this)(trans, any)
        }
    }

    @discardableResult
    private func any(execute: @escaping Listener) -> Self {
        self.listeners.append(execute)
        return self
    }
}

// MARK: - on TRANSITION events
//------------------------------------------------------------------------------
extension FlowController {
    @discardableResult
    public func on<F:FlowController>(from: State, to: State, execute: @escaping (F) -> (Transition) -> Void) -> Self {
        return on(transition: Transition(from: from, to: to), execute: execute)
    }

    @discardableResult
    public func on<F:FlowController, T>(from: State, to: State, execute: @escaping (F) -> (Transition,T) -> Void) -> Self {
        return on(transition: Transition(from: from, to: to), execute: execute)
    }

    @discardableResult
    public func on<F:FlowController, T:ExpressibleByNilLiteral>(from: State, to: State, execute: @escaping (F) -> (Transition,T) -> Void) -> Self {
        return on(transition: Transition(from: from, to: to), execute: execute)
    }

    @discardableResult
    public func on<F:FlowController>(from: State, to: State, execute: @escaping (F) -> (Transition,Any?) -> Void) -> Self {
        return on(transition: Transition(from: from, to: to), execute: execute)
    }

    @discardableResult
    public func on<F:FlowController>(transition: Transition, execute: @escaping (F) -> (Transition) -> Void) -> Self {
        return on(transition: transition) { [unowned self] (transition: Transition, any: Any?) in
            guard let this = self as? F else { return }
            execute(this)(transition)
        }
    }

    @discardableResult
    public func on<F:FlowController, T>(transition: Transition, execute: @escaping (F) -> (Transition,T) -> Void) -> Self {
        return on(transition: transition) { [unowned self] (transition: Transition, any: Any?) in
            guard let this = self as? F else { return }
            guard let obj = any as? T else { return }
            execute(this)(transition, obj)
        }
    }

    @discardableResult
    public func on<F:FlowController, T:ExpressibleByNilLiteral>(transition: Transition, execute: @escaping (F) -> (Transition,T) -> Void) -> Self {
        return on(transition: transition) { [unowned self] (transition, any: Any?) -> Void in
            guard let this = self as? F else { return }
            guard let nonil = any else { execute(this)(transition, nil); return }
            execute(this)(transition, nonil as? T ?? nil)
        }
    }

    @discardableResult
    public func on<F:FlowController>(transition: Transition, execute: @escaping (F) -> (Transition,Any?) -> Void) -> Self {
        return on(transition: transition) { [unowned self] trans, any -> Void in
            guard let this = self as? F else { return }
            execute(this)(trans, any)
        }
    }

    @discardableResult
    private func on(transition: Transition, execute: @escaping TransitionListener) -> Self {
        if var listener = self.transitionListeners[transition] {
            listener.append(execute)
            self.transitionListeners[transition] = listener
        } else {
            var e = [TransitionListener]()
            e.append(execute)
            self.transitionListeners[transition] = e
        }
        return self
    }
}

// MARK: - Delegate
//------------------------------------------------------------------------------
extension FlowController: StateTransitionDelegate {
    public func stateMachineWillTransition(_ stateMachine: StateMachine, from: State, to: State, with: Any?) {
        let transition = from => to
        self.listeners.forEach{ $0(transition, with) }
        guard let listeners = self.transitionListeners[transition] else { return }
        listeners.forEach{
            $0(transition, with)
        }
    }

    public func stateMachineDidFailTransition(_ stateMachine: StateMachine, from: State, to: State, with: Any?) {
        assertionFailure("Invalid transition: \(from → to)")
    }

    public func stateMachineDidTransition(_ stateMachine: StateMachine, from: State, to: State, with: Any?) {
        guard let listeners = self.stateListeners[to] else { return }
        listeners.forEach{ $0(to, with) }
    }

    public func stateMachineDidEnd(_ stateMachine: StateMachine, on: State, with: Any?) {
    }
}
