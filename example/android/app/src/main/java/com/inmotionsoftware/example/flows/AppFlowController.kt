package com.inmotionsoftware.example.flows

import android.os.Bundle
import com.inmotionsoftware.example.HomeActivity
import com.inmotionsoftware.example.HomeResult
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.example.flows.AppState.FromBegin
import com.inmotionsoftware.example.flows.AppState.FromHome
import com.inmotionsoftware.example.flows.AppState.FromLogin
import com.inmotionsoftware.example.flows.AppState.FromProfile
import com.inmotionsoftware.example.views.ProfileFragment
import com.inmotionsoftware.flowkit.android.StateMachineActivity
import com.inmotionsoftware.flowkit.back
import com.inmotionsoftware.flowkit.cancel
import com.inmotionsoftware.promisekt.map


class AppFlowController: StateMachineActivity<AppState, Unit, Unit>(), AppStateMachine {

    override fun onBegin(state: AppState, context: Unit): Promise<FromBegin> =
        Promise.value(FromBegin.Home(context=context))

    override fun onHome(state: AppState, context: Unit): Promise<FromHome> =
        this.subflow(activity=HomeActivity::class.java, context=context)
            .map {
                when (it) {
                    is HomeResult.Login -> FromHome.Login(context=Unit)
                    is HomeResult.Profile -> FromHome.Profile(context=Unit)
                }
            }
            .back { FromHome.Home(Unit) }
            .cancel { FromHome.Home(Unit) }

    override fun onLogin(state: AppState, context: Unit): Promise<FromLogin> =
        this.subflow(stateMachine=LoginFlowController::class.java, state=LoginFlowState.Begin(context))
            .map { FromLogin.Home(Unit) as FromLogin }
            .cancel { FromLogin.Home(Unit) }
            .back { FromLogin.Home(Unit) }


    override fun onProfile(state: AppState, context: Unit): Promise<FromProfile> =
        this.subflow2(fragment=ProfileFragment::class.java, context=context)
            .map { FromProfile.Home(Unit) as FromProfile }
            .back { FromProfile.Home(Unit) }
            .cancel { FromProfile.Home(Unit) }
}