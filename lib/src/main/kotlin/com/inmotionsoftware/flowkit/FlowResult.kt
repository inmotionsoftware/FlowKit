//
//  FlowResult.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

sealed class FlowResult<T> {

    val isBack: Boolean get() { return this is back }
    val isCancel: Boolean get() { return this is cancel }
    val isComplete: Pair<Boolean, T?>
        get() {
            return when (this) {
                is complete -> Pair(true, this.value)
                else -> Pair(false, null)
            }
        }

    class complete<T>(val value: T): FlowResult<T>()
    class back<T>: FlowResult<T>()
    class cancel<T>: FlowResult<T>()

}
