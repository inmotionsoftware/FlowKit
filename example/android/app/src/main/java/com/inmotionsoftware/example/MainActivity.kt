package com.inmotionsoftware.example

import android.os.Bundle
import com.inmotionsoftware.example.flows.*
import com.inmotionsoftware.flowkit.android.BootstrapActivity
import com.inmotionsoftware.flowkit.android.StateMachineActivity
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map


class MainActivity: BootstrapActivity() {
    override fun onBegin(state: State, context: Unit): Promise<Unit> =
        this.subflow(activity=AppFlowController::class.java, state=AppState.Begin(Unit))
}