package com.inmotionsoftware.flowkit

private typealias FFragment = FlowFragment<Any?, Any?>

class FlowFragmentManager {

    val map = mutableMapOf<FlowId, MutableMap<Class<FlowFragment<Any?, Any?>>, FlowFragment<Any?, Any?>>>()

    inline fun <reified T : FlowFragment<*, *>> getFragment(flowId: FlowId): T {
        map[flowId]
            ?.let { (it[T::class.java as? Class<FFragment>] as? T) ?: it.add<T>() }
            ?: let {
                val newFlow = mutableMapOf<Class<FFragment>, FFragment>()
                    .apply { add<T>() }

                map.put(flowId, newFlow)
            }

        return map[flowId]?.get(T::class.java as Class<FFragment>) as T
    }

    inline fun <reified T : FlowFragment<*, *>> MutableMap<Class<FFragment>, FFragment>.add() {
        this[T::class.java as Class<FFragment>] = T::class.java.newInstance() as FFragment
    }

}
