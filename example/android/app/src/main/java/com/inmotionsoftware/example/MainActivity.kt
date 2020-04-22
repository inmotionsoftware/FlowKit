package com.inmotionsoftware.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.inmotionsoftware.example.flows.LoginFlowController
import com.inmotionsoftware.flowkit.Bootstrap
import com.inmotionsoftware.flowkit.android.Backable
import com.inmotionsoftware.flowkit.android.startFlow
import com.inmotionsoftware.flowkit.startFlow
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Bootstrap.startFlow(LoginFlowController(), activity=this, viewId=R.id.fragment_container, context=Unit)
    }

    override fun onBackPressed() {
        (this.supportFragmentManager.fragments.firstOrNull() as? Backable?)?.let {
            it.onBackPressed()
            return
        }
        super.onBackPressed()
    }
}
