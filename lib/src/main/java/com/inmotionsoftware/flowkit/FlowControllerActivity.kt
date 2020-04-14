//
//  FlowControllerActivity.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

import android.content.Intent
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.inmotionsoftware.promisekt.DeferredPromise
import com.inmotionsoftware.promisekt.Promise

interface FlowFragmentContainer {
    val flowFragmentContainerViewId: Int
}

//
// FlowActivity
//

abstract class FlowActivity : AppCompatActivity(), FlowFragmentContainer {

    data class ActivityResult(val requestCode: Int, val resultCode: Int, val data: Intent?)

    val flowFragmentManager by lazy { FlowFragmentManager() }

    internal val flowBackStack = ArrayList<String>()

    private var deferredPromise: DeferredPromise<ActivityResult>? = null

    fun startActivity(intent: Intent, requestCode: Int): Promise<ActivityResult> {
        val deferredPromise = DeferredPromise<ActivityResult>()
        this.deferredPromise = deferredPromise

        this.startActivityForResult(intent, requestCode)
        return deferredPromise.promise
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        this.deferredPromise?.resolve(value = ActivityResult(requestCode, resultCode, data))
        this.deferredPromise = null
    }

}

//
// FlowControllerActivity
//

abstract class FlowControllerActivity<ARGS, RETURN> : FlowActivity(), FlowBackButtonDelegate {

    private var flowController: FragmentFlowController<*, ARGS, RETURN>? = null

    abstract fun flowControllerClass(): Class<FragmentFlowController<*, ARGS, RETURN>>
    abstract fun args(): ARGS
    abstract fun onFlowControllerResult(result: FlowPromise<RETURN>)

    fun cancelFlow() {
        this.flowController?.cancel()
    }

    override fun onStart() {
        super.onStart()
        if (this.flowController == null) {
            this.flowController = FlowControllerProvider.flow(controllerClass = this.flowControllerClass())
        }
    }

    override fun onPause() {
        super.onPause()
        this.flowController?.pauseFlow()
    }

    override fun onResume() {
        super.onResume()
        this.flowController?.let {
            val result = if (it.isFlowPaused) it.resumeFlow(activity = this) else it.startFlow(activity = this, args = this.args())
            this.onFlowControllerResult(result = result)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        this.flowController = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle UP button as BACK
                if (this.onBackButtonPressed()) return true
                return super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (this.onBackButtonPressed()) return
        super.onBackPressed()
    }

    override fun onBackButtonPressed(): Boolean {
        val manager = this.supportFragmentManager
        val fragments = manager.fragments.filterNotNull()
        val backDelegate = fragments.findLast { it is FlowBackButtonDelegate } as? FlowBackButtonDelegate
        return backDelegate?.onBackButtonPressed() ?: false
    }

}
