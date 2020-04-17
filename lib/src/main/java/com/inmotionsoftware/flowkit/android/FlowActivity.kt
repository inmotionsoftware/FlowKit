package com.inmotionsoftware.flowkit.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.promisekt.Resolver
import com.inmotionsoftware.promisekt.fulfill
import com.inmotionsoftware.promisekt.reject

val FLOW_KIT_ACTIVITY_RESULT: String = "FLOWKIT"

open class FlowActivity<Input, Output>: ComponentActivity() {

    var input: Input? = null

    @CallSuper
    override fun onResume() {
        super.onResume()
        load()
    }

    fun resolve(value: Output) {
        val intent = Intent()
        val bundle = Bundle()
        bundle.put(FLOW_KIT_ACTIVITY_RESULT, value)
        intent.putExtra("result", bundle)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    fun reject(error: Throwable) {
        val bundle = Bundle()
        bundle.put("failure", error)

        val intent = Intent()
        intent.putExtra(FLOW_KIT_ACTIVITY_RESULT, bundle)
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    fun cancel() {
        this.reject(FlowError.Canceled())
    }

    fun back() {
        this.reject(FlowError.Back())
    }

    override fun onBackPressed() {
        back()
    }

    private fun load() {
        this.input = intent.getBundleExtra(FLOW_KIT_ACTIVITY_RESULT)?.get("context") as Input
    }
}
