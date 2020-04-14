//
//  FlowController.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map

interface FlowState

//
// StateListener
//

interface StateListener<STATE: FlowState> {
    fun <WITH> on(state: STATE, execute: (STATE, WITH) -> Unit ): StateListener<STATE>
}

@Deprecated("use on, instead of onType and onNullableType", ReplaceWith("this.on(state = state, execute = execute)"))
fun <STATE: FlowState, T> StateListener<STATE>.onType(state: STATE, execute: (STATE, T) -> Unit ): StateListener<STATE> {
    return this.on(state = state, execute = execute)
}

@Deprecated("use on, instead of onType and onNullableType", ReplaceWith("this.on(state = state, execute = execute)"))
fun <STATE: FlowState, T> StateListener<STATE>.onNullableType(state: STATE, execute: (STATE, T?) -> Unit ): StateListener<STATE> {
    return this.on(state=state, execute = execute)
}

//
// FlowController
//

interface FlowController<STATE: FlowState, ARGS, RETURN> : Flow<ARGS, RETURN> {

    interface Delegate<STATE: FlowState, ARGS> {
        fun onFlowControllerStart(args: ARGS)
        fun onFlowControllerPause()
        fun onFlowControllerResume()
        fun onFlowControllerEnd()

        // Before Start and Resume
        fun onFlowControllerSetup(activity: FlowActivity)
        // After Pause and before End
        fun onFlowControllerTearDown()

        fun registerStates(states: Builder<STATE>)
        fun registerEvents(listener: StateListener<STATE>)
    }

    interface TerminateCallback {
        fun onFlowControllerTerminate(flow: FlowController<*, *, *>)
    }

    var flowControllerDelegate: Delegate<STATE, ARGS>?
    var flowControllerTerminateCallback: TerminateCallback?

    fun state(): STATE
    fun transitionAsync(from: STATE, to: STATE, with: Any? = null): Promise<Unit>
    fun transitionAsync(to: STATE, with: Any? = null): Promise<Unit>
    fun transition(from: STATE, to: STATE, with: Any? = null)
    fun transition(to: STATE, with: Any? = null)
    fun registerSubFlow(flow: FlowController<*, *, *>)

    fun startFlow(activity: FlowActivity, args: ARGS): FlowPromise<RETURN>
    fun pauseFlow()
    fun resumeFlow(activity: FlowActivity): FlowPromise<RETURN>
}

fun <STATE: FlowState, RETURN> FlowController<STATE, Unit, RETURN>.startFlow(activity: FlowActivity): FlowPromise<RETURN> = this.startFlow(activity = activity, args = Unit)

//
// FlowControllerDelegation
//

class FlowControllerDelegation<STATE: FlowState, ARGS, RETURN>
    : FlowDelegation<ARGS, RETURN>()
    , FlowController<STATE, ARGS, RETURN>
    , StateListener<STATE>
    , FiniteStateMachine.StateTransitionListener<STATE>
    , FlowController.TerminateCallback {

    private lateinit var stateMachine: FiniteStateMachine<STATE>
    private var subFlow: FlowController<*, *, *>? = null

    private val stateListeners: MutableMap<STATE, MutableList<(STATE, Any?) -> Unit>> = mutableMapOf()
    private val transitionListeners: MutableMap<FiniteStateMachine<STATE>.Transition, MutableList<(FiniteStateMachine<STATE>.Transition, Any?) -> Unit>> = mutableMapOf()

    override var flowControllerDelegate: FlowController.Delegate<STATE, ARGS>? = null
    override var flowControllerTerminateCallback: FlowController.TerminateCallback? = null

    override fun state(): STATE = this.stateMachine.state

    override fun startFlow(activity: FlowActivity, args: ARGS): FlowPromise<RETURN> {
        // Make sure the delegate is set
        assert(this.flowControllerDelegate != null)

        val builder: Builder<STATE> = Builder()
        this.flowControllerDelegate?.registerStates(states = builder)
        this.stateMachine = builder.build()
        this.stateMachine.listener = this

        this.flowControllerDelegate?.registerEvents(listener = this)

        this.flowControllerDelegate?.onFlowControllerSetup(activity = activity)
        this.flowControllerDelegate?.onFlowControllerStart(args = args)

        return super.start(args)
    }

    override fun pauseFlow() {
        super.pause()

        if (this.subFlow != null) {
            this.subFlow?.pauseFlow()
        } else {
            this.stateMachine.pause()
        }

        this.flowControllerDelegate?.onFlowControllerPause()
        this.flowControllerDelegate?.onFlowControllerTearDown()
    }

    override fun resumeFlow(activity: FlowActivity): FlowPromise<RETURN> {
        this.flowControllerDelegate?.onFlowControllerSetup(activity = activity)

        if (this.subFlow != null) {
            this.subFlow?.resumeFlow(activity = activity)
        } else {
            this.stateMachine.resume()
        }

        this.flowControllerDelegate?.onFlowControllerResume()

        return super.resume()
    }

    override fun cleanup() {
        super.cleanup()

        this.subFlow?.cancel()
        this.subFlow = null

        this.flowControllerDelegate?.onFlowControllerTearDown()
        this.flowControllerDelegate?.onFlowControllerEnd()
    }

    override fun transitionAsync(from: STATE, to: STATE, with: Any?): Promise<Unit>
        = Promise.value(value=to).map { this.transition(from=from, to=to, with=with) }

    override fun transitionAsync(to: STATE, with: Any?): Promise<Unit>
            = Promise.value(value=to).map { this.transition(to=to, with=with) }

    override fun transition(from: STATE, to: STATE, with: Any?) {
        assert(this.stateMachine.state == from)
        this.stateMachine.transition(to=to, with=with)
    }

    override fun transition(to: STATE, with: Any?) {
        this.stateMachine.transition(to=to, with=with)
    }

    override fun stateMachineWillTransition(stateMachine: FiniteStateMachine<STATE>, from: STATE, to: STATE, with: Any?) {
        val trans = stateMachine.Transition(from = from, to = to)
        this.transitionListeners[trans]?.let { it.forEach { it(trans, with) } }
    }

    override fun stateMachineDidFailTransition(stateMachine: FiniteStateMachine<STATE>, from: STATE, to: STATE, with: Any?) {
        assert(false) { "Invalid transition: $from → $to)" }
    }

    override fun stateMachineDidTransition(stateMachine: FiniteStateMachine<STATE>, from: STATE, to: STATE, with: Any?) {
        this.stateListeners[to]?.let { it.forEach { it(to, with) } }
    }

    override fun stateMachineDidEnd(stateMachine: FiniteStateMachine<STATE>, on: STATE, with: Any?) {
    }

    @Suppress("UNCHECKED_CAST")
    override fun <WITH> on(state: STATE, execute: (STATE, WITH) -> Unit): StateListener<STATE> {
        val typeCheckExecute = { state: STATE, with: WITH ->
            try { execute(state, with) } catch (e: Throwable) { if(BuildConfig.DEBUG) { e.printStackTrace() } }
        } as ((STATE, Any?) -> Unit)

        var listener = this.stateListeners[state]
        if (listener == null) {
            listener = arrayListOf(typeCheckExecute)
            this.stateListeners[state] = listener
        } else {
            listener.add(typeCheckExecute)
        }
        return this
    }

    override fun registerSubFlow(flow: FlowController<*, *, *>) {
        this.subFlow = flow
        this.subFlow?.flowControllerTerminateCallback = this
    }

    override fun onFlowControllerTerminate(flow: FlowController<*, *, *>) {
        this.subFlow?.let {
            if (it == flow) this.subFlow = null
        }
    }

}
