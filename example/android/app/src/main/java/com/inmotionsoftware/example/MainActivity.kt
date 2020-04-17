package com.inmotionsoftware.example

import android.os.Bundle
import android.os.PersistableBundle
import com.inmotionsoftware.example.flows.AppFlowController
import com.inmotionsoftware.example.flows.LoginFlowController
import com.inmotionsoftware.flowkit.Bootstrap
import com.inmotionsoftware.flowkit.android.Backable
import com.inmotionsoftware.flowkit.android.DispatchActivity
import com.inmotionsoftware.flowkit.android.startFlow

class MainActivity : DispatchActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Bootstrap.startFlow(AppFlowController(), activity=this, viewId=R.id.fragment_container, context=Unit)
    }
}
