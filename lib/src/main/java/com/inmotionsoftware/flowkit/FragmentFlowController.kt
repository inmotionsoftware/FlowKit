//
//  FragmentFlowController.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import java.lang.ref.WeakReference
import java.util.*

typealias FlowId = UUID

abstract class FragmentFlowController<STATE: FlowState, ARGS, RETURN>
    : FlowController<STATE, ARGS, RETURN> by FlowControllerDelegation<STATE, ARGS, RETURN>()
    , FlowController.Delegate<STATE, ARGS> {

    val flowId: FlowId = FlowId.randomUUID()

    private var flowActivity: WeakReference<FlowActivity>? = null
    protected val activity: FlowActivity
        get() {
            val localActivity = this.flowActivity?.get()
            localActivity ?: throw IllegalStateException("FragmentFlowController.activity cannot be null!")
            return localActivity
        }

    private var fragmentManagerWeak: WeakReference<FragmentManager>? = null
    private val fragmentManager: FragmentManager?
        get() = fragmentManagerWeak?.get()

    internal fun initialize(fragmentManager: FragmentManager?) {
        this.fragmentManagerWeak = fragmentManager?.let { WeakReference(it) }

        this.flowControllerDelegate = this
    }

    fun <STATE: FlowState, ARGS, RETURN, T : FragmentFlowController<STATE, ARGS, RETURN>> flow(controllerClass: Class<T>, args: ARGS): FlowPromise<RETURN> {
        return FlowControllerProvider.subflow(controllerClass = controllerClass).let { subFlow ->
            subFlow.startFlow(activity = this.activity, args = args).also { this.registerSubFlow(flow = subFlow) }
        }
    }

    fun <STATE: FlowState, RETURN, T : FragmentFlowController<STATE, Unit, RETURN>> flow(controllerClass: Class<T>): FlowPromise<RETURN>
            = this.flow(controllerClass = controllerClass, args = Unit)

    fun <RETURN, F: FlowFragment<Unit, RETURN>> flow(fragment: F): FlowPromise<RETURN>
            = this.flow<F, Unit, RETURN>(fragment, Unit)

    fun <F, ARGS, RETURN> flow(fragment: F, args: ARGS): FlowPromise<RETURN> where F: FlowFragment<ARGS, RETURN> {
        val manager = this.fragmentManager ?: this.activity.supportFragmentManager
        val fragmentName = "${fragment.javaClass.let { it.canonicalName ?: it.simpleName }}_${fragment.flowFragmentId() ?: ""}"
        val isOnBackStack = this.activity.flowBackStack.contains(fragmentName)

        val ft = manager.beginTransaction()
        val visibleFragment = manager.findFragmentById(this.activity.flowFragmentContainerViewId) as? F
        val isFragmentOnTop = "${(visibleFragment?.javaClass?.let { it.canonicalName ?: it.simpleName } ?: "")}_${visibleFragment?.flowFragmentId() ?: ""}" == fragmentName

        if (isOnBackStack) {
            if (!isFragmentOnTop) {
                val sortedBackStack = this.activity.flowBackStack.reversed()
                for (it in sortedBackStack) {
                    if (fragmentName == it) {
                        break
                    }
                    this.activity.flowBackStack.remove(it)
                }
                this.configureExitTransaction(fragmentTransaction = ft)
            }
        } else {
            this.configureEnterTransaction(fragmentTransaction = ft)
            this.activity.flowBackStack.add(fragmentName)
        }

        val result: FlowPromise<RETURN>
        if (visibleFragment == null || !isFragmentOnTop) {
            result = fragment.start(args)
            visibleFragment?.flowWillEnd()

            ft.replace(this.activity.flowFragmentContainerViewId, fragment)
            ft.runOnCommit {
                if (fragment.isFlowStarted && fragment.isAdded) {
                    fragment.flowWillRun(args = args)
                }
            }
            ft.commitNow()
        } else {
            result = visibleFragment.start(args)
            visibleFragment.flowWillRun(args = args)
        }

        return result
    }

    open fun onCancel(state: STATE, with: Any?) {
        this.cancel()
    }

    open fun onBack(state: STATE, with: Any?) {
        this.back()
    }

    open fun onFail(state: STATE, error: Throwable) {
        this.fail(error = error)
    }

    @CallSuper
    override fun onFlowControllerPause() {}

    @CallSuper
    override fun onFlowControllerResume() {}

    @CallSuper
    override fun onFlowControllerSetup(activity: FlowActivity) {
        this.flowActivity = WeakReference(activity)
    }

    @CallSuper
    override fun onFlowControllerTearDown() {
        this.flowActivity = null
    }

    @CallSuper
    override fun onFlowControllerEnd() {
        this.flowControllerTerminateCallback?.onFlowControllerTerminate(flow = this)
    }

    protected abstract fun configureEnterTransaction(fragmentTransaction: FragmentTransaction)
    protected abstract fun configureExitTransaction(fragmentTransaction: FragmentTransaction)

    protected inline fun <reified T: FlowFragment<*, *>>getFragment(): T {
        return this.activity.flowFragmentManager.getFragment(this.flowId)
    }
}

//
// BasicFragmentFlowController
//

abstract class BasicFragmentFlowController<STATE: FlowState> : FragmentFlowController<STATE, Unit, Unit>()
