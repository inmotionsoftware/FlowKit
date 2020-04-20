package com.inmotionsoftware.flowkit.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inmotionsoftware.flowkit.StateMachine
import com.inmotionsoftware.flowkit.StateMachineDelegate
import com.inmotionsoftware.flowkit.StateMachineHost
import com.inmotionsoftware.promisekt.*

class StateViewModel<S>: ViewModel() {
    val state = MutableLiveData<S>()
}

abstract class FragmentContainerActivity<S,I,O>(): FlowActivity<I,O>(), FragContainer, StateMachine<S,I,O>, NavStateMachine, StateMachineDelegate<S> {
    override val viewId: Int = View.generateViewId()
    override val activity: DispatchActivity get() { return this }

    override var nav: FragContainer
        get() { return this }
        set(value) {}

    lateinit var currentState: StateViewModel<S>

    override fun stateDidChange(from: S, to: S) {
        currentState.state.value = to
    }

    private fun createContent() {
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val layout = LinearLayout(this.applicationContext)
        layout.layoutParams = params
        layout.orientation = LinearLayout.VERTICAL
        setContentView(layout, params)
        layout.id = viewId
    }

    private fun loadViewModel() {
        @Suppress("UNCHECKED_CAST")
        currentState = ViewModelProvider(this).get(StateViewModel::class.java) as StateViewModel<S>
    }

    private fun startFlow() {
        StateMachineHost(stateMachine=this)
            .startFlow(this.input)
            .done { this.resolve(it) }
            .catch { this.reject(it) }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createContent()
        loadViewModel()
        startFlow()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // TODO: save the state data...
    }
}