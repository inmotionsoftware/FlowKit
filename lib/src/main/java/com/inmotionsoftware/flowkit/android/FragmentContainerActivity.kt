package com.inmotionsoftware.flowkit.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.inmotionsoftware.flowkit.StateMachine
import com.inmotionsoftware.flowkit.StateMachineHost
import com.inmotionsoftware.promisekt.*

abstract class FragmentContainerActivity<S,I,O>(): FlowActivity<I,O>(), FragContainer, StateMachine<S,I,O>, NavStateMachine {
    override val viewId: Int = View.generateViewId()
    override val activity: DispatchActivity get() { return this }

    override var nav: FragContainer
        get() { return this }
        set(value) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val layout = LinearLayout(this.applicationContext)
        layout.layoutParams = params
        layout.orientation = LinearLayout.VERTICAL
        setContentView(layout, params)
        layout.id = viewId

        StateMachineHost(stateMachine=this)
            .startFlow(this.input)
            .done { this.resolve(it) }
            .catch { this.reject(it) }
    }

//    override fun handleBackButton(): Boolean {
//        finish()
//        return true
//    }
}