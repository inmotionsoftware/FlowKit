//
//  FlowFragment.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

import androidx.fragment.app.Fragment

interface FlowBackButtonDelegate {
    fun onBackButtonPressed(): Boolean
}

abstract class FlowFragment<ARGS, RETURN>
    : Fragment()
    , Flow<ARGS, RETURN> by FlowDelegation<ARGS, RETURN>()
    , FlowBackButtonDelegate {

    override fun onBackButtonPressed(): Boolean {
        this.back()
        return true
    }

    abstract fun flowWillRun(args: ARGS)

    abstract fun flowWillEnd()

    abstract fun flowFragmentId(): String?

}
