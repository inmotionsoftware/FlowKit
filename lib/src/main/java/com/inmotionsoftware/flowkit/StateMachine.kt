package com.inmotionsoftware.flowkit

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.inmotionsoftware.flowkit.android.*
import com.inmotionsoftware.promisekt.*

interface StateMachineDelegate<State> {
    fun stateDidChange(from: State, to: State)
}

interface StateMachine<State, Input, Output>: StateMachineDelegate<State> {
    fun dispatch(state: State): Promise<State>
    fun firstState(context: Input): State
    fun getResult(state: State): Result<Output>?
    fun onTerminate(state: State, context: Result<Output>) :  Promise<Result<Output>> = Promise.value(context)

    override fun stateDidChange(from: State, to: State) {}
}

//interface IStateMachineHost<State, Input, Output, SM: StateMachine<State, Input, Output>> : Flow<Input, Output>, StateMachineDelegate<State> {
//    val stateMachine: SM
//
//    fun jumpToState(state: State) =
//        nextState(prev=state, curr=state)
//            .map {
//                when (it) {
//                    is Result.Success -> it.value
//                    is Result.Failure -> throw it.cause
//                }
//            }
//
//    private fun nextState(prev: State, curr: State): Promise<Result<Output>> {
//        this.stateDidChange(from=prev, to=curr)
//        this.stateMachine.getResult(state=curr)?.let {
//            return this.stateMachine.onTerminate(state=curr, context=it)
//        }
//        return stateMachine.dispatch(state=curr).thenMap { nextState(prev=curr, curr=it) }
//    }
//}

open class StateMachineHost<State, Input, Output, SM: StateMachine<State, Input, Output>>(val stateMachine: SM) : Flow<Input, Output>, StateMachineDelegate<State> {
    var delegate: StateMachineDelegate<State>? = null

    override fun startFlow(context: Input): Promise<Output> {
        val begin = this.stateMachine.firstState(context=context)
        return this.jumpToState(state=begin)
    }

    override fun stateDidChange(from: State, to: State) {
        this.delegate?.stateDidChange(from = from, to = to)
        this.stateMachine.stateDidChange(from=from, to=to)
    }

    private fun jumpToState(state: State): Promise<Output> =
        nextState(prev=state, curr=state)
            .map {
                when (it) {
                    is Result.Success -> it.value
                    is Result.Failure -> throw it.cause
                }
            }

    private fun nextState(prev: State, curr: State): Promise<Result<Output>> {
        this.stateDidChange(from=prev, to=curr)
        this.stateMachine.getResult(state=curr)?.let {
            return this.stateMachine.onTerminate(state=curr, context=it)
        }
        return stateMachine.dispatch(state=curr).thenMap { nextState(prev=curr, curr=it) }
    }
}

fun <S, I, O, S2, I2, O2, SM> StateMachine<S, I, O>.subflow(stateMachine: SM, activity: DispatchActivity, viewId: Int, context: I2): Promise<O2> where SM : StateMachine<S2, I2, O2>, SM: NavStateMachine =
    NavigationStateMachineHost(stateMachine=stateMachine, activity=activity, viewId=viewId).startFlow(context=context)

fun <S, I, O, S2, I2, O2, SM> StateMachine<S, I, O>.subflow(stateMachine: SM, context: I2): Promise<O2> where SM: StateMachine<S2, I2, O2> =
    StateMachineHost(stateMachine=stateMachine).startFlow(context=context)

fun <S, I, O, SM: StateMachine<S, I, O>> Bootstrap.Companion.startFlow(stateMachine: SM, context: I): Unit =
    StateMachineHost(stateMachine=stateMachine)
    .startFlow(context=context)
        .done {
            Log.e(Bootstrap::javaClass.name, "Root flow is being restarted")
        }
        .catch {
            Log.e(Bootstrap::javaClass.name, "Root flow is being restarted", it)
        }
        .finally {
            startFlow<S,I,O,SM>(stateMachine=stateMachine, context=context)
        }


