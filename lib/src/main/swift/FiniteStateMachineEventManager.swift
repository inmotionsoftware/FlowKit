//
//  StateMachineEventManager.swift
//  FlowKit
//
//  Created by Brian Howard on 6/15/17.
//  Copyright © 2017 InMotion Software, LLC. All rights reserved.
//

import Foundation


/**
*/
public final class FiniteStateMachineEventManager<S:StateType>: StateTransitionDelegate {
    public typealias State = S
    public typealias StateMachine = FiniteStateMachine<State>
    public typealias Transition = StateMachine.Transition
    public typealias StateListener = (State,Any?) -> Bool
    public typealias TransListener = (Transition,Any?) -> Bool

    fileprivate var transListeners = [Transition:[TransListener]]()
    fileprivate var stateListeners = [State:[StateListener]]()
    fileprivate var any = [TransListener]()
    fileprivate var exit = [State:[StateListener]]()
    fileprivate var done = [StateListener]()
    fileprivate var fail = [TransListener]()

    fileprivate func debug(_ log: String) {
        print(log)
    }

    public func stateMachineWillTransition(_ stateMachine: StateMachine, from: State, to: State, with: Any?) {
        let transition = (from → to)
        dispatch(transitions: self.any, transition: transition, with: with)
        dispatch(matching: self.exit, state: transition.from, with: with)
        dispatch(matching: self.transListeners, transition: transition, with: with)
    }

    public func stateMachineDidFailTransition(_ stateMachine: StateMachine, from: State, to: State, with: Any?) {
        dispatch(transitions: self.fail, transition: from → to, with: with)
    }

    public func stateMachineDidTransition(_ stateMachine: StateMachine, from: State, to: State, with: Any?) {
        dispatch(matching: self.stateListeners, state: to, with: with)
    }

    public func stateMachineDidEnd(_ stateMachine: StateMachine, on: State, with: Any?) {
        dispatch(states: self.done, state: on, with: with)
    }
}

// MARK: - On STATE events
//------------------------------------------------------------------------------
extension FiniteStateMachineEventManager {

    @discardableResult
    public func on<T:ExpressibleByNilLiteral>(state: State, execute: @escaping (State,T) -> Void) -> Self {
        return on(state: state) { (state, any: Any?) -> Bool in
            guard let nonil = any else { execute(state, nil); return true }
            execute(state, nonil as? T ?? nil)
            return true
        }
    }

    @discardableResult
    public func on<T>(state: State, execute: @escaping (State,T) -> Void) -> Self {
        return on(state: state) { (state, any: Any?) -> Bool in
            guard let obj = any as? T else { return false }
            execute(state, obj)
            return true
        }
    }

    @discardableResult
    public func on(state: State, execute: @escaping (State,Any?) -> Void) -> Self {
        return on(state: state) { state, any -> Bool in
            execute(state, any)
            return true
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

// MARK: - on ANY events
//------------------------------------------------------------------------------
extension FiniteStateMachineEventManager {
    @discardableResult
    public func any<T:ExpressibleByNilLiteral>(execute: @escaping (Transition,T) -> Void) -> Self {
        return any { trans, any -> Bool in
            guard let nonil = any else { execute(trans, nil); return true }
            execute(trans, nonil as? T ?? nil)
            return true

        }
    }

    @discardableResult
    public func any<T>(execute: @escaping (Transition,T) -> Void) -> Self {
        return any { trans, any -> Bool in
            guard let obj = any as? T else { return false }
            execute(trans, obj)
            return true
        }
    }

    @discardableResult
    public func any(execute: @escaping (Transition,Any?) -> Void) -> Self {
        return any { trans, any -> Bool in
            execute(trans, any)
            return true
        }
    }

    @discardableResult
    private func any(execute: @escaping TransListener) -> Self {
        any.append(execute)
        return self
    }
}

// MARK: - On EXIT events
//------------------------------------------------------------------------------
extension FiniteStateMachineEventManager {
    @discardableResult
    public func on<T>(exit: State, execute: @escaping (State,T) -> Void) -> Self {
        return on(exit: exit) { (state, any: Any?) -> Bool in
            guard let obj = any as? T else { return false }
            execute(state, obj)
            return true
        }
    }

    @discardableResult
    public func on<T:ExpressibleByNilLiteral>(exit: State, execute: @escaping (State,T) -> Void) -> Self {
        return on(exit: exit) { (state, any: Any?) -> Bool in
            guard let nonil = any else { execute(state, nil); return true }
            execute(state, nonil as? T ?? nil)
            return true
        }
    }

    @discardableResult
    public func on(exit: State, execute: @escaping (State,Any?) -> Void) -> Self {
        return on(exit: exit) { state, any -> Bool in
            execute(state, any)
            return true
        }
    }

    @discardableResult
    private func on(exit: State, execute: @escaping StateListener) -> Self {
        if var listener = self.exit[exit] {
            listener.append(execute)
            self.exit[exit] = listener
        } else {
            var e = [StateListener]()
            e.append(execute)
            self.exit[exit] = e
        }
        return self
    }
}

// MARK: - on TRANSITION events
//------------------------------------------------------------------------------
extension FiniteStateMachineEventManager {
    @discardableResult
    public func on<T>(from: State, to: State, execute: @escaping (Transition,T) -> Void) -> Self {
        return on(transition: Transition(from: from, to: to), execute: execute)
    }

    @discardableResult
    public func on<T:ExpressibleByNilLiteral>(from: State, to: State, execute: @escaping (Transition,T) -> Void) -> Self {
        return on(transition: Transition(from: from, to: to), execute: execute)
    }

    @discardableResult
    public func on(from: State, to: State, execute: @escaping (Transition,Any?) -> Void) -> Self {
        return on(transition: Transition(from: from, to: to), execute: execute)
    }

    @discardableResult
    public func on<T>(transition: Transition, execute: @escaping (Transition,T) -> Void) -> Self {
        return on(transition: transition) { (transition, any: Any?) -> Bool in
            guard let obj = any as? T else { return false }
            execute(transition, obj)
            return true
        }
    }

    @discardableResult
    public func on<T:ExpressibleByNilLiteral>(transition: Transition, execute: @escaping (Transition,T) -> Void) -> Self {
        return on(transition: transition) { (transition, any: Any?) -> Bool in
            guard let nonil = any else { execute(transition, nil); return true }
            execute(transition, nonil as? T ?? nil)
            return true
        }
    }

    @discardableResult
    public func on(transition: Transition, execute: @escaping (Transition,Any?) -> Void) -> Self {
        return on(transition: transition) { trans, any -> Bool in
            execute(trans, any)
            return true
        }
    }

    @discardableResult
    private func on(transition: Transition, execute: @escaping TransListener) -> Self {
        if var listener = self.transListeners[transition] {
            listener.append(execute)
            self.transListeners[transition] = listener
        } else {
            var e = [TransListener]()
            e.append(execute)
            self.transListeners[transition] = e
        }
        return self
    }
}

// MARK: - on FAIL events
//------------------------------------------------------------------------------
extension FiniteStateMachineEventManager {

    @discardableResult
    public func fail<T:ExpressibleByNilLiteral>(execute: @escaping (Transition,T) -> Void) -> Self {
        return fail { trans, any -> Bool in
            guard let nonil = any else { execute(trans, nil); return true }
            execute(trans, nonil as? T ?? nil)
            return true

        }
    }

    @discardableResult
    public func fail<T>(execute: @escaping (Transition,T) -> Void) -> Self {
        return fail { trans, any -> Bool in
            guard let obj = any as? T else { return false }
            execute(trans, obj)
            return true
        }
    }

    @discardableResult
    public func fail(execute: @escaping (Transition,Any?) -> Void) -> Self {
        return fail { trans, any -> Bool in
            execute(trans, any)
            return true
        }
    }


    @discardableResult
    private func fail(execute: @escaping TransListener) -> Self {
        fail.append(execute)
        return self
    }
}

// MARK: - On DONE events
//------------------------------------------------------------------------------
extension FiniteStateMachineEventManager {

    @discardableResult
    public func done<T:ExpressibleByNilLiteral>(execute: @escaping (State,T) -> Void) -> Self {
        return done { state, any -> Bool in
            guard let nonil = any else { execute(state, nil); return true }
            execute(state, nonil as? T ?? nil)
            return true

        }
    }

    @discardableResult
    public func done<T>(execute: @escaping (State,T) -> Void) -> Self {
        return done { state, any -> Bool in
            guard let obj = any as? T else { return false }
            execute(state, obj)
            return true
        }
    }

    @discardableResult
    public func done(execute: @escaping (State,Any?) -> Void) -> Self {
        return done { state, any -> Bool in
            execute(state, any)
            return true
        }
    }

    @discardableResult
    private func done(execute: @escaping StateListener) -> Self {
        self.done.append(execute)
        return self
    }
}

// MARK: - Dispatching events
//------------------------------------------------------------------------------
fileprivate extension FiniteStateMachineEventManager {
    func dispatch(transitions: [TransListener], transition: Transition, with: Any?) {
        guard transitions.count > 0 else { return }
        let dispatched = transitions.reduce(false) { $1(transition, with) || $0 }
        if !dispatched { debug("WARNING!!! transition '\(transition)' had event listeners but all listeners were filtered") }
    }

    func dispatch(states: [StateListener], state: State, with: Any?) {
        guard states.count > 0 else { return }
        let dispatched = states.reduce(false) { $1(state, with) || $0 }
        if !dispatched { debug("WARNING!!! state: '\(state)' had event listeners but all listeners were filtered") }
    }

    func dispatch(matching: [State:[StateListener]], state: State, with: Any?) {
        guard let states = matching[state] else { return }
        dispatch(states: states, state: state, with: with)
    }

    func dispatch(matching: [Transition:[TransListener]], transition: Transition, with: Any?) {
        guard let transitions = matching[transition] else { return }
        dispatch(transitions: transitions, transition: transition, with: with)
    }
}
