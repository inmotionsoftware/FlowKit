package com.inmotionsoftware.flowkit

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.inmotionsoftware.flowkit.android.*
import com.inmotionsoftware.promisekt.*

interface StateMachine<State, Input, Output> {
    fun dispatch(state: State): Promise<State>
    fun terminal(state: State): Result<Output>?
    fun firstState(context: Input): State
}

interface StateMachineDelegate<State> {
    fun stateDidChange(from: State, to: State)
}

open class StateMachineHost<State, Input, Output, SM: StateMachine<State, Input, Output>>(val stateMachine: SM) : Flow<Input, Output> {
    var delegate: StateMachineDelegate<State>? = null

    override fun startFlow(context: Input): Promise<Output> {
        val begin = this.stateMachine.firstState(context=context)
        return this
            .nextState(prev=begin, curr=begin)
            .map {
                when (it) {
                    is Result.Success -> it.value
                    is Result.Failure -> throw it.cause
                }
            }
    }

    private fun nextState(prev: State, curr: State): Promise<Result<Output>> {
        this.delegate?.stateDidChange(from=prev, to=curr)
        val result = this.stateMachine.terminal(state=curr)
        return if (result == null) {
            this.stateMachine.dispatch(state=curr).thenMap { this.nextState(prev=curr, curr=it) }
        } else {
            Promise.value(result)
        }
    }
}

fun <S, I, O, S2, I2, O2, SM> StateMachine<S, I, O>.subflow(stateMachine: SM, activity: DispatchActivity, viewId: Int, context: I2): Promise<O2> where SM : StateMachine<S2, I2, O2>, SM: NavStateMachine =
    NavigationStateMachineHost(stateMachine=stateMachine, activity=activity, viewId=viewId).startFlow(context=context)

fun <S, I, O, S2, I2, O2, SM> StateMachine<S, I, O>.subflow(stateMachine: SM, context: I2): Promise<O2> where SM: StateMachine<S2, I2, O2> =
    StateMachineHost(stateMachine=stateMachine).startFlow(context=context)

fun <S, I, O> Bootstrap.Companion.startFlow(stateMachine: StateMachine<S, I, O>, context: I) {
    val rt = StateMachineHost(stateMachine=stateMachine)
    .startFlow(context=context)
        .ensure {
            Log.e(Bootstrap::javaClass.name, "Root flow is being restarted")
            startFlow(stateMachine=stateMachine, context=context)
        }
}


