// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a 
// copy of this software and associated documentation files (the "Software"), 
// to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, 
// and/or sell copies of the Software, and to permit persons to whom the 
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.
//
//  Flow.swift
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

import android.os.Parcelable
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.inmotionsoftware.flowkit.android.*
import com.inmotionsoftware.promisekt.*

interface FlowState: Parcelable {}

interface StateMachineDelegate<State> {
    fun stateDidChange(from: State, to: State)
}

interface StateFactory<State: FlowState, Input> {
    fun createState(context: Input): State
    fun createState(error: Throwable): State
}

interface StateMachine<State: FlowState, Input, Output>: StateMachineDelegate<State>, StateFactory<State, Input> {
    fun dispatch(prev: State, state: State): Promise<State>

    fun getResult(state: State): Result<Output>?
    fun onTerminate(state: State, context: Result<Output>) :  Promise<Output> =
        when (context) {
            is Result.Success -> Promise.value(context.value)
            is Result.Failure -> Promise(error=context.cause)
        }

    override fun stateDidChange(from: State, to: State) {}
}

fun <S: FlowState, I, O, S2: FlowState, I2, O2, SM2: StateMachine<S2,I2,O2>> StateMachine<S,I,O>.subflow(stateMachine: SM2, context: I2): Promise<O2> =
    StateMachineHost<S2,I2,O2, SM2>(stateMachine=stateMachine)
        .startFlow(context=context)

open class StateMachineHost<State: FlowState, Input, Output, SM: StateMachine<State, Input, Output>>(val stateMachine: SM) : Flow<Input, Output>, StateMachineDelegate<State> {
    var delegate: StateMachineDelegate<State>? = null

    override fun startFlow(context: Input): Promise<Output> {
        val begin = this.stateMachine.createState(context=context)
        return this.jumpToState(state=begin)
    }

    override fun stateDidChange(from: State, to: State) {
        this.delegate?.stateDidChange(from = from, to = to)
        this.stateMachine.stateDidChange(from=from, to=to)
    }

    private fun jumpToState(state: State): Promise<Output> =
        nextState(prev=state, curr=state)
            .map { when(it) {
                is Result.Success -> it.value
                is Result.Failure -> throw it.cause
            }}

    private fun nextState(prev: State, curr: State): Promise<Result<Output>> {
        this.stateDidChange(from=prev, to=curr)
        this.stateMachine.getResult(state=curr)?.let {
            return this.stateMachine.onTerminate(state=curr, context=it)
                .map { Result.Success(it) as Result<Output> }
                .recover { Promise.value(Result.Failure<Output>(it)) }
        }
        return stateMachine.dispatch(prev=prev, state=curr)
            .thenMap { nextState(prev=curr, curr=it) }
            .recover { nextState(prev=curr, curr=stateMachine.createState(error=it)) }
    }
}

fun <S: FlowState, I, O, SM: StateMachine<S, I, O>> Bootstrap.Companion.startFlow(stateMachine: SM, context: I): Unit =
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


