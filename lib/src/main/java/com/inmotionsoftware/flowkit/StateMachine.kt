package com.inmotionsoftware.flowkit

import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.then
import com.inmotionsoftware.promisekt.thenMap

interface StateMachine<State, Input, Output> {
    fun dispatch(state: State): Promise<State>
    fun terminal(state: State): Result<Output>?
    fun firstState(context: Input): State
}

interface StateMachineDelegate<State> {
    fun stateDidChange(from: State, to: State)
}

class StateMachineHost<State, Input, Output, SM: StateMachine<State, Input, Output>>(val stateMachine: SM) : Flow<Input, Output> {
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
        this.delegate?.let { it.stateDidChange(from=prev, to=curr) }
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
}

//sealed class AppState {
//    interface FromBegin {}
//    interface FromHome {}
//    interface FromLogin {}
//    interface FromEnd {}
//    interface FromFail {}
//
//    class Begin(val context: Unit): AppState()
//    class Home(val context: Unit): AppState(), FromBegin, FromHome, FromLogin
//    class Login(val context: Unit): AppState(), FromHome
//    class End(val context: Unit): AppState(), FromHome
//    class Fail(val context: Throwable): AppState()
//    class Terminate(val context: Result<Unit>): AppState(), FromFail, FromEnd
//}
//
//interface AppStateMachine: StateMachine<AppState, Unit, Unit> {
//    fun onBegin(state: AppState, context: Unit) :  Promise<AppState.FromBegin>
//    fun onHome(state: AppState, context: Unit) :  Promise<AppState.FromHome>
//    fun onLogin(state: AppState, context: Unit) :  Promise<AppState.FromLogin>
//
//    fun onEnd(state: AppState, context: Unit) :  Promise<AppState.FromEnd> = Promise.value(AppState.Terminate(Result.Success(context)))
//    fun onFail(state: AppState, context: Throwable) :  Promise<AppState.FromFail> = Promise.value(AppState.Terminate(Result.Failure(context)))
//    fun onTerminate(state: AppState, context: Result<Unit>) :  Promise<Result<Unit>> = Promise.value(context)
//
//    override fun dispatch(state: AppState): Promise<AppState> {
//        return when (state) {
//            is AppState.Begin -> onBegin(state=state, context=state.context).map { it as AppState }
//            is AppState.Home -> onHome(state=state, context=state.context).map { it as AppState }
//            is AppState.Login -> onLogin(state=state, context=state.context).map { it as AppState }
//            is AppState.End -> onEnd(state=state, context=state.context).map { it as AppState }
//            is AppState.Fail -> onFail(state=state, context=state.context).map { it as AppState }
//            is AppState.Terminate -> onTerminate(state=state, context=state.context).map { it as AppState }
//        }
//    }
//
//    override fun terminal(state: AppState): Result<Unit>? =
//        when (state) {
//            is AppState.Terminate -> state.context
//            else -> null
//        }
//
//    override fun firstState(context: Unit): AppState = AppState.Begin(context=context)
//}
//
//class AppFlow: AppStateMachine {
//    override fun onBegin(state: AppState, context: Unit): Promise<AppState.FromBegin> {
//        return Promise.value(AppState.Home(context=Unit))
//    }
//
//    override fun onHome(state: AppState, context: Unit): Promise<AppState.FromHome> {
//        return Promise.value(AppState.End(context=Unit))
//    }
//
//    override fun onLogin(state: AppState, context: Unit): Promise<AppState.FromLogin> {
//        return Promise.value(AppState.Home(context=Unit))
//    }
//}
//
//fun blah() {
//    val flow = AppFlow()
//    val host = StateMachineHost(stateMachine=flow)
//}

