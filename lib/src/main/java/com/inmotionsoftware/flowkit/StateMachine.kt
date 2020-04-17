package com.inmotionsoftware.flowkit

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.inmotionsoftware.flowkit.android.FlowActivity
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

    fun <State, Input, Output, SM: StateMachine<State,Input,Output>> subflow(stateMachine: SM, context: Input): Promise<Output> {
        return Promise(error=FlowError.Canceled())
    }

    fun <State, Input, Output> subflow(activity: FlowActivity<Input, Output>, context: Input): Promise<Output> {
        return Promise(error=FlowError.Canceled())
    }
}

fun <State, Input, Output, State2, Input2, Output2> StateMachine<State, Input, Output>.subflow(stateMachine: StateMachine<State2, Input2, Output2>, context: Input2): Promise<Output2> {
    return StateMachineHost(stateMachine=stateMachine)
        .startFlow(context=context)
}

fun <State, Input, Output> Bootstrap.Companion.startFlow(stateMachine: StateMachine<State, Input, Output>, context: Input) {
    val rt = StateMachineHost(stateMachine=stateMachine)
    .startFlow(context=context)
        .ensure {
            Log.e(Bootstrap::javaClass.name, "Root flow is being restarted")
            startFlow(stateMachine=stateMachine, context=context)
        }
}


