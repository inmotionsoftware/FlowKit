package com.inmotionsoftware.flowkit.android

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.inmotionsoftware.example.FlowFragment
import com.inmotionsoftware.flowkit.*
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.thenMap
import kotlin.Result

interface Navigable {
    val fragmentManager: FragmentManager
}

interface FragContainer: Navigable {
    val activity: AppCompatActivity
    val viewId: Int

    override val fragmentManager: FragmentManager
        get() { return activity.supportFragmentManager }
}

fun <Input, Output, Frag: FlowFragment<Input, Output>> FragContainer.subflow(to: Class<Frag>, context: Input): Promise<Output> {
    val frag = to.newInstance()

    val args = Bundle()
    context?.let { args.put("context", it) }
    frag.arguments = args

    val pending = Promise.pending<Output>()
    frag.attach(pending.second)

    this.fragmentManager.beginTransaction()
        .replace(viewId, frag)
        .addToBackStack(null)
        .commit()

    return pending.first
}

interface NavStateMachine {
    var nav: FragContainer

    fun <Input, Output, Frag: FlowFragment<Input, Output>> subflow(to: Class<Frag>, context: Input): Promise<Output> = nav.subflow(to=to, context=context)
}

fun <State, Input, Output> Bootstrap.Companion.startFlow(stateMachine: StateMachine<State, Input, Output>, activity: AppCompatActivity, viewId: Int, context: Input) {
    val rt = NavigationStateMachineHost(stateMachine=stateMachine, activity=activity, viewId=viewId)
        .startFlow(context=context)
        .ensure {
            Log.e(Bootstrap::javaClass.name, "Root flow is being restarted")
            startFlow(stateMachine=stateMachine, context=context)
        }
}

class NavigationStateMachineHost<State, Input, Output, SM: StateMachine<State, Input, Output>> (
        stateMachine: SM,
        override val activity: AppCompatActivity,
        override val viewId: Int): StateMachineHost<State, Input, Output, SM>(stateMachine), FragContainer {

    override fun startFlow(context: Input): Promise<Output> {
        // inject ourselves
        if (stateMachine is NavStateMachine) {
            stateMachine.nav = this
        }

        return super.startFlow(context)
    }
}