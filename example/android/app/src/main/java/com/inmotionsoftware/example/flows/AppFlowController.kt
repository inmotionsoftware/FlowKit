package com.inmotionsoftware.example.flows

import com.inmotionsoftware.example.HomeActivity
import com.inmotionsoftware.example.HomeResult
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.example.flows.AppState.FromBegin
import com.inmotionsoftware.example.flows.AppState.FromHome
import com.inmotionsoftware.example.flows.AppState.FromLogin
import com.inmotionsoftware.flowkit.android.StateMachineActivity
import com.inmotionsoftware.promisekt.map


class AppFlowController: StateMachineActivity<AppState, Unit, Unit>(), AppStateMachine {

    override fun onBegin(state: AppState, context: Unit): Promise<AppState.FromBegin> {
        return Promise.value(FromBegin.Home(context=context))
    }

    override fun onHome(state: AppState, context: Unit): Promise<AppState.FromHome> =
        this.subflow(activity=HomeActivity::class.java, context=context)
            .map {
                when (it) {
                    is HomeResult.Login -> FromHome.Login(context=Unit)
                }
            }

    override fun onLogin(state: AppState, context: Unit): Promise<AppState.FromLogin> =
        this.subflow(activity=LoginFlowController::class.java, state = LoginFlowState.Begin(context))
            .map { FromLogin.Home(Unit) }
}