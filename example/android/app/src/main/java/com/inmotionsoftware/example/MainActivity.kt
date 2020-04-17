package com.inmotionsoftware.example

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.inmotionsoftware.example.flows.LoginFlowController
import com.inmotionsoftware.flowkit.Bootstrap
import com.inmotionsoftware.flowkit.android.Backable
import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.flowkit.android.FLOW_KIT_ACTIVITY_RESULT
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.flowkit.android.startFlow
import com.inmotionsoftware.flowkit.startFlow
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bundle = Bundle()
        bundle.put("context", "Input String")

        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("FLOW_KIT", bundle)
        startActivityForResult(intent, 0)
//        Bootstrap.startFlow(LoginFlowController(), activity=this, viewId=R.id.fragment_container, context=Unit)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        assert(requestCode == 0)

        when (resultCode) {
            Activity.RESULT_OK -> {
                val result = data?.getBundleExtra(FLOW_KIT_ACTIVITY_RESULT)?.get("result") as? String
                print("result: ${result}")
            }
            Activity.RESULT_CANCELED -> {
                val error = data?.getBundleExtra(FLOW_KIT_ACTIVITY_RESULT)?.get("failure") as? Throwable
                if (error == null) {
                    print("null error")
                } else {
                    when (error) {
                        is FlowError.Canceled -> { print("canceled") }
                        is FlowError.Back -> { print("canceled") }
                        else -> { print("error!: ${error.localizedMessage}") }
                    }
                }
            }
            else -> {}
        }
    }

    override fun onBackPressed() {
        (this.supportFragmentManager.fragments.firstOrNull() as? Backable?)?.let {
            it.onBackPressed()
            return
        }
        super.onBackPressed()
    }
}
