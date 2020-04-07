//
//  FlowControllerProvider.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

import androidx.fragment.app.FragmentManager

object FlowControllerProvider : FlowController.TerminateCallback {

    private var flows: HashMap<String, Flow<*, *>> = HashMap()

    fun <T : FlowController<*, *, *>> flow(controllerClass: Class<T>): T
        = flow(key = controllerClass.canonicalName ?: "", controllerClass = controllerClass)

    @Suppress("UNCHECKED_CAST")
    fun <T : FlowController<*, *, *>> flow(key: String, controllerClass: Class<T>): T {
        var instance = flows[key]
        if (instance == null) {
            instance = controllerClass.newInstance()
            instance.flowControllerTerminateCallback = this
            flows[key] = instance
        }
        return instance as T
    }

    fun <T : FragmentFlowController<*, *, *>> flow(controllerClass: Class<T>, fragmentManager: FragmentManager? = null): T
        = flow(key = controllerClass.canonicalName ?: "", controllerClass = controllerClass, fragmentManager = fragmentManager)

    fun <T : FragmentFlowController<*, *, *>> flow(key: String, controllerClass: Class<T>, fragmentManager: FragmentManager?): T {
        val flow = flow(key = key, controllerClass = controllerClass)
        flow.initialize(fragmentManager = fragmentManager)
        return flow
    }

    internal fun <T : FragmentFlowController<*, *, *>> subflow(controllerClass: Class<T>, fragmentManager: FragmentManager? = null): T {
        val subflow = controllerClass.newInstance()
        subflow.initialize(fragmentManager = fragmentManager)
        return subflow
    }

    override fun onFlowControllerTerminate(flow: FlowController<*, *, *>) {
        flows.filter { it.value == flow }.forEach { flows.remove(it.key) }
    }

}
