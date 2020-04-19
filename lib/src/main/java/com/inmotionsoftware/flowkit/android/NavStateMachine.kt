package com.inmotionsoftware.flowkit.android

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.inmotionsoftware.flowkit.Bootstrap
import com.inmotionsoftware.flowkit.StateMachine
import com.inmotionsoftware.flowkit.StateMachineHost
import com.inmotionsoftware.example.FlowFragment
import com.inmotionsoftware.flowkit.*
import com.inmotionsoftware.promisekt.*
import java.lang.IllegalStateException
import kotlin.Result
import com.inmotionsoftware.flowkit.startFlow
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.ensure

interface Navigable {
    val fragmentManager: FragmentManager
}

interface FragContainer: Navigable {
    val activity: DispatchActivity
    val viewId: Int

    override val fragmentManager: FragmentManager
        get() { return activity.supportFragmentManager }
}


interface NavStateMachine {
    var nav: FragContainer
}

inline fun <I, reified O, F: FlowFragment<I, O>> NavStateMachine.subflow2(fragment: Class<F>, context: I): Promise<O> =
    this.subflow(fragment=fragment.newInstance(), context = context)

inline fun <I, reified O, F: FlowFragment<I, O>> NavStateMachine.subflow(fragment: F, context: I): Promise<O> {
    val args = Bundle()
    context?.let { args.put("context", it) }
    fragment.arguments = args

    if (this.nav.activity.isDestroyed) {
        return Promise(error=IllegalStateException("Trying to add fragment to destroyed Activity"))
    }

    val pending = Promise.pending<O>()
    fragment.attach(pending.second)
    this.nav.fragmentManager.beginTransaction()
        .replace(this.nav.viewId, fragment)
        .addToBackStack(null)
        .commitAllowingStateLoss()

    return pending.first
}

inline fun <I, reified O, A: FlowActivity<I, O>> NavStateMachine.subflow(activity: Class<A>, context: I): Promise<O> =
    this.nav.activity.subflow(activity=activity, context=context)

fun <S, I, O, SM, S2, I2, O2, SM2> SM.subflow(stateMachine: SM2, context: I2): Promise<O2>
            where SM2: StateMachine<S2, I2, O2>, SM2: NavStateMachine, SM: StateMachine<S, I, O>, SM: NavStateMachine =
    NavigationStateMachineHost(stateMachine=stateMachine, activity=this.nav.activity, viewId=nav.viewId)
        .startFlow(context=context)

fun <S, I, O, SM> Bootstrap.Companion.startFlow(stateMachine: SM, activity: DispatchActivity, viewId: Int, context: I): Unit where SM: StateMachine<S, I, O>, SM: NavStateMachine =
    NavigationStateMachineHost(stateMachine=stateMachine, activity=activity, viewId=viewId)
        .startFlow(context=context)
        .done {
            Log.e(Bootstrap::javaClass.name, "Root flow is being restarted")
        }
        .catch {
            Log.e(Bootstrap::javaClass.name, "Root flow is being restarted", it)
        }
        .finally {
            startFlow<S,I,O,SM>(stateMachine=stateMachine, activity=activity, viewId=viewId, context=context)
        }

fun <S, I, O, SM> Bootstrap.Companion.startFlow(stateMachine: SM, container: FragContainer, context: I): Unit where SM: StateMachine<S, I, O>, SM: NavStateMachine =
    this.startFlow(stateMachine=stateMachine, activity=container.activity, viewId=container.viewId, context=context)

class NavigationStateMachineHost<S, I, O, SM> (stateMachine: SM, override var activity: DispatchActivity, override val viewId: Int)
    : StateMachineHost<S, I, O, SM>(stateMachine), FragContainer where SM: StateMachine<S, I, O>, SM: NavStateMachine {

    constructor(stateMachine: SM, container: FragContainer):
            this(stateMachine=stateMachine, activity=container.activity, viewId=container.viewId)

    override fun startFlow(context: I): Promise<O> {
        stateMachine.nav = this // dependency injection
        return super.startFlow(context)
    }
}
