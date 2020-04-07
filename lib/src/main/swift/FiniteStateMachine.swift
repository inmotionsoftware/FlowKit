//
//  FiniteStateMachine.swift
//  FlowKit
//
//  Created by Brian Howard on 2/6/17.
//  Copyright © 2017 InMotion Software, LLC. All rights reserved.
//

import Foundation

/**
*/
public protocol StateType : Hashable {
}

/**
*/
public struct FSMTransition<State: StateType> : Hashable, Equatable, CustomStringConvertible {

    public let from: State
    public let to: State

    public init(from: State, to: State) {
        self.from = from
        self.to = to
    }

    /// The hash value.
    ///
    /// Hash values are not guaranteed to be equal across different executions of
    /// your program. Do not save hash values to use during a future execution.
    public var hashValue: Int {
        let a = from.hashValue
        let b = to.hashValue
        // Szudzik's function
        return a >= b ? a*a+a+b : a+b*b;
    }

    public var description: String {
        return "[\(self.from) → \(self.to)]"
    }

    public static func == (lhs: FSMTransition, rhs: FSMTransition) -> Bool {
        return lhs.from == rhs.from && lhs.to == rhs.to
    }
}

/**
*/
precedencegroup StateTransition {
    associativity: none
    assignment: false
}

infix operator => : StateTransition
infix operator → : StateTransition

/**
*/
public func →<State:StateType> (left: State, right: State) -> FSMTransition<State> {
    return FSMTransition(from: left, to: right)
}

/**
*/
public func =><State:StateType> (left: State, right: State) -> FSMTransition<State> {
    return FSMTransition(from: left, to: right)
}

/**
*/
public enum FSMError<State:StateType> : Error {
    public typealias Transition = FSMTransition<State>

    case invalidTransition(Transition)
    case concurrentTransition
    case anyToAny
}

fileprivate enum Wildcard<State: Hashable & Equatable> : Hashable, Equatable {
    case specific(State)
    case any

    public var hashValue: Int {
        switch (self) {
            case .specific(let state): return state.hashValue
            case .any: return Int.max
        }
    }

    public static func == (lhs: Wildcard, rhs: Wildcard) -> Bool {
        switch (lhs, rhs) {
            case (.any, .any): return true
            case (.specific(let l), .specific(let r)): return l == r
            default: return false
        }
    }
}

fileprivate class StateTransitions<State:StateType> {
    fileprivate typealias Transition = FSMTransition<State>
    fileprivate typealias StateSet = Set<State>
    fileprivate typealias Key = Wildcard<State>
    fileprivate typealias Val = Wildcard<StateSet>

    private var states = [Key : Val]()

    public func isValid(transition: Transition) -> Bool {
        // check for wildcard transitions to our new state
        if let wildcards = self.states[.any] {
            switch(wildcards) {
                case .any: break // shouldn't be allowed to get here...
                case .specific(let set):
                    if set.contains(transition.to) { return true }
            }
        }

        let state: Key = .specific(transition.from)
        guard let states = self.states[state] else { return false }
        switch(states) {
            case .any: return true
            case .specific(let set):
                return set.contains(transition.to)
        }
    }

    public func add(transition: Transition) {
        try! add(from: .specific(transition.from), to: .specific(transition.to))
    }

    public func isDeadEnd(state: State) -> Bool {
        guard let state = self.states[.specific(state)] else { return true }

        switch(state) {
            case .any:
                return false
            case .specific(let set):
                return set.count == 0
        }
    }

    public func add(from: Wildcard<State>, to: Wildcard<State>) throws {
        switch(to) {
            case .any:
                if (from == .any) { throw FSMError<State>.anyToAny }
                self.states[from] = .any

            case .specific(let to):
                guard let states = self.states[from] else {
                    var set = StateSet()
                    set.insert(to)
                    self.states[from] = .specific(set)
                    return
                }

                switch(states) {
                    case .any: break
                    case .specific(var set):
                        set.insert(to)
                        self.states[from] = .specific(set)
                }
        }
    }
}

public protocol StateTransitionDelegate: class {
    associatedtype State: StateType
    typealias StateMachine = FiniteStateMachine<State>
    func stateMachineWillTransition(_ stateMachine: StateMachine, from: State, to: State, with: Any?)
    func stateMachineDidFailTransition(_ stateMachine: StateMachine, from: State, to: State, with: Any?)
    func stateMachineDidTransition(_ stateMachine: StateMachine, from: State, to: State, with: Any?)
    func stateMachineDidEnd(_ stateMachine: StateMachine, on: State, with: Any?)
}

public class AnyDelegate<State:StateType>: StateTransitionDelegate {
    public typealias StateMachine = FiniteStateMachine<State>

    enum DispatchType {
        case didEnd
        case willTransition
        case didTransition
        case didFail
    }

    private let delegate: (StateMachine, DispatchType, State, State, Any?) -> Void

    public init<D: StateTransitionDelegate>(_ delegate: D?) where D.State == State {

        self.delegate = { [weak delegate] stateMachine, type, from, to, obj in
            guard let del = delegate else { return }
            switch(type) {
                case .didEnd:
                    D.stateMachineDidEnd(del)(stateMachine, on: to, with: obj)
                case .willTransition:
                    D.stateMachineWillTransition(del)(stateMachine, from: from, to: to, with: obj)
                case .didTransition:
                    D.stateMachineDidTransition(del)(stateMachine, from: from, to: to, with: obj)
                case .didFail:
                    D.stateMachineDidFailTransition(del)(stateMachine, from: from, to: to, with: obj)
            }
        }
    }

    public func stateMachineDidEnd(_ stateMachine: FiniteStateMachine<State>, on: State, with: Any?) {
        self.delegate(stateMachine, .didEnd, on, on, with)
    }

    public func stateMachineDidTransition(_ stateMachine: FiniteStateMachine<State>, from: State, to: State, with: Any?) {
        self.delegate(stateMachine, .didTransition, from, to, with)
    }

    public func stateMachineDidFailTransition(_ stateMachine: FiniteStateMachine<State>, from: State, to: State, with: Any?) {
        self.delegate(stateMachine, .didFail, from, to, with)
    }

    public func stateMachineWillTransition(_ stateMachine: FiniteStateMachine<State>, from: State, to: State, with: Any?) {
        self.delegate(stateMachine, .willTransition, from, to, with)
    }
}

extension StateTransitionDelegate {
    var erasure: AnyDelegate<State> {
        return AnyDelegate(self)
    }
}


/**
*/
public class FiniteStateMachine<S:StateType> {
    public typealias State = S
    public typealias Error = FSMError<State>
    public typealias Transition = FSMTransition<State>

    var delegate: AnyDelegate<S>?

    /**
    */
    public class Builder {
        public typealias Transition = FiniteStateMachine.Transition

        private var states: StateTransitions<State>
        private var initialState: State?

        public init() {
            self.states = StateTransitions()
        }

        @discardableResult
        public func initialState( state: State ) -> Self {
            self.initialState = state
            return self
        }

        @discardableResult
        public func add(from: State, to: State) -> Self {
            return add(Transition(from: from, to: to))
        }

        @discardableResult
        public func add(fromAnyTo: State) -> Self {
            try! self.states.add(from: .any, to: .specific(fromAnyTo))
            return self
        }

        @discardableResult
        public func add(toAnyFrom: State) -> Self {
            // implicit default state
            if self.initialState == nil {
                self.initialState = toAnyFrom
            }

            try! self.states.add(from: .specific(toAnyFrom), to: .any)
            return self
        }

        @discardableResult
        public func add(_ transition: Transition) -> Self {

            // implicit default state
            if self.initialState == nil {
                self.initialState = transition.from
            }

            self.states.add(transition: transition)
            return self
        }

        public func build() -> FiniteStateMachine {
            return FiniteStateMachine(state: self.initialState!, states: self.states)
        }
    }

    private(set) public var state: State
    private let states: StateTransitions<State>
    private(set) public var transitioning : Bool

//    public weak var delegate: D?

    fileprivate init(state: State, states: StateTransitions<State>) {
        self.state = state
        self.states = states
        self.transitioning = false
    }

    public func isValid(from: State, to: State) -> Bool {
        return isValid(transition: Transition(from: from, to: to))
    }

    public func isValid(transition: Transition) -> Bool {
        return self.states.isValid(transition: transition)
    }

    public func isDeadEnd(state: State) -> Bool {
        return self.states.isDeadEnd(state: state)
    }

    /// transition the state machine from the current state to the specified state
    /// - parameter to: The next state
    /// - parameter with: Any object you want passed along to the listener
    /// - throws:
    public func transition(to: State, with: Any? = nil) throws -> Void {
        // make sure we are not concurrently transitioning
        guard !self.transitioning else { throw Error.concurrentTransition }
        self.transitioning = true
        defer { self.transitioning = false }

        let from = self.state

        // are we already on this state?
        guard from != to else { return }

        guard self.isValid(transition: from → to) else {
            try self.didFailTransition(from: from, to: to, with: with)
            return
        }

        self.willTransition(from: from, to: to, with: with)
        self.state = to
        self.didTransition(from: from, to: to, with: with)

        // is this a dead end state?
        if isDeadEnd(state: to) { self.didEnd(on: to, with: with) }
    }

    private func willTransition(from: State, to: State, with: Any?) {
        guard let delegate = self.delegate else { return }
        delegate.stateMachineWillTransition(self, from: from, to: to, with: with)
    }

    private func didFailTransition(from: State, to: State, with: Any?) throws {
        // no delegate, throw an error
        guard let delegate = self.delegate else { throw Error.invalidTransition(from → to) }
        delegate.stateMachineDidFailTransition(self, from: from, to: to, with: with)
    }

    private func didTransition(from: State, to: State, with: Any?) {
        //logD("[\(from) → \(to)]")
        guard let delegate = self.delegate else { return }
        delegate.stateMachineDidTransition(self, from: from, to: to, with: with)
    }

    private func didEnd(on: State, with: Any?) {
        guard let delegate = self.delegate else { return }
        delegate.stateMachineDidEnd(self, on: on, with: with)
    }
}

/**
*/
typealias Transition<State:StateType> = FSMTransition<State>
