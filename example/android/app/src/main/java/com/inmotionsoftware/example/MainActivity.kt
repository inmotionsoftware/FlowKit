package com.inmotionsoftware.example

import android.os.Bundle
import android.os.PersistableBundle
import com.inmotionsoftware.example.flows.*
import com.inmotionsoftware.flowkit.Bootstrap
import com.inmotionsoftware.flowkit.Result
import com.inmotionsoftware.flowkit.StateMachine
import com.inmotionsoftware.flowkit.android.Backable
import com.inmotionsoftware.flowkit.android.DispatchActivity
import com.inmotionsoftware.flowkit.android.startFlow
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.map


class MainActivity : BlahActivity<AppState, Unit, Unit>(), AppStateMachine {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onBegin(state: AppState, context: Unit): Promise<AppState.FromBegin> {
        return Promise.value(AppState.FromBegin.Home(context=context))
    }

    override fun onHome(state: AppState, context: Unit): Promise<AppState.FromHome> =
        this.subflow2(activity = HomeActivity::class.java, context=context)
            .map {
                when (it) {
                    is HomeResult.Login -> AppState.FromHome.Login(context=Unit)
                }
            }

    override fun onLogin(state: AppState, context: Unit): Promise<AppState.FromLogin> =
        this.subflow(activity=LoginFlowController::class.java, context = context)
            .map { AppState.FromLogin.Home(Unit) }


    override fun getInput(): Unit { return Unit }
}
