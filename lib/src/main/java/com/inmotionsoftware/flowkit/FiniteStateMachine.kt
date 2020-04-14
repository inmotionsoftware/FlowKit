package com.inmotionsoftware.flowkit

import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

//
//  Copyright © 2017 InMotion Software, LLC. All rights reserved.
//

class Builder<State> {
    internal val fromAny = HashSet<State>()
    internal val toAny = HashSet<State>()
    internal val states = HashMap<State, HashSet<State>>()
    internal var def: State? = null

    fun add(from: State, to: State): Builder<State> {
        val state = this.states[from]
        if (state == null) {
            val set = HashSet<State>()
            set.add(to)
            this.states[from] = set
        } else {
            state.add(to)
        }
        return this
    }

    fun addFromAny(from: State): Builder<State> {
        this.fromAny.add(from)
        return this
    }

    fun addToAny(to: State): Builder<State> {
        this.toAny.add(to)
        return this
    }

    fun initialState(state: State): Builder<State> {
        this.def = state
        return this
    }

    fun build(): FiniteStateMachine<State> = FiniteStateMachine(this)
}

class FiniteStateMachine<State> internal constructor(builder: Builder<State>) {
    inner class Transition(val from: State, val to: State) {
        val description: String
            get() = "$this.from → $this.to"
    }

    interface StateTransitionListener<State> {
        fun stateMachineWillTransition(stateMachine: FiniteStateMachine<State>, from: State, to: State, with: Any?)
        fun stateMachineDidFailTransition(stateMachine: FiniteStateMachine<State>, from: State, to: State, with: Any?)
        fun stateMachineDidTransition(stateMachine: FiniteStateMachine<State>, from: State, to: State, with: Any?)
        fun stateMachineDidEnd(stateMachine: FiniteStateMachine<State>, on: State, with: Any?)
    }

    private val fromAny: Set<State>
    private val toAny: Set<State>
    private val states: Map<State, Set<State>>
    private var transitioning: Boolean = false

    private var isPaused: Boolean = false
    private var lastTransition:  Triple<State, State, Any?>? = null

    var listener: StateTransitionListener<State>? = null
    var state: State
        private set

    init {
        this.fromAny = builder.fromAny
        this.toAny = builder.toAny
        this.states = builder.states
        this.state = builder.def!!
    }

    fun isValid(transition: Transition): Boolean {
        if (this.toAny.contains(transition.from)) { return true }
        if (this.fromAny.contains(transition.to)) { return true }
        return this.states[transition.from]?.contains(transition.to) ?: false
    }

    fun isDeadEnd(state: State): Boolean {
        if (this.toAny.contains(state)) { return false }
        if (this.states.containsKey(state)) { return false }
        return true
    }

    fun transition(to: State, with: Any? = null) {
        if (this.transitioning) { throw ConcurrentModificationException() }
        try {
            this.transitioning = true
            this.lastTransition = Triple(this.state, to, with)

            if (this.isPaused) { return }

            val from = this.state
            if (from == to) { return }

            // check if this a valid transition
            this.willTransition(from=from, to=to, with=with)
            this.state = to
            this.didTransition(from=from, to=to, with=with)

            if (this.isDeadEnd(to)) { this.didEnd(on=to, with=with) }
        } finally {
            this.transitioning = false
        }
    }

    fun pause() {
        this.isPaused = true
    }

    fun resume() {
        this.isPaused = false
        this.lastTransition?.let {
            this.state = it.first
            this.transition(to=it.second, with=it.third)
        }
    }

    private fun willTransition(from: State, to: State, with: Any?) {
        this.listener?.stateMachineWillTransition(stateMachine = this@FiniteStateMachine, from = from, to = to, with = with)
    }

    private fun didFailTransition(from: State, to: State, with: Any?) {
        this.listener?.stateMachineDidFailTransition(stateMachine = this@FiniteStateMachine, from = from, to = to, with = with)
    }

    private fun didTransition(from: State, to: State, with: Any?) {
        this.listener?.stateMachineDidTransition(stateMachine = this@FiniteStateMachine, from = from, to = to, with = with)
    }

    private fun didEnd(on: State, with: Any?) {
        this.listener?.stateMachineDidEnd(stateMachine = this@FiniteStateMachine, on = on, with = with)
    }

}
