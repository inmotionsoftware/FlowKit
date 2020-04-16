package com.inmotionsoftware.flowkit.android

import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.inmotionsoftware.example.Backable
import com.inmotionsoftware.flowkit.Flow
import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.Resolver
import com.inmotionsoftware.promisekt.fulfill
import com.inmotionsoftware.promisekt.reject

class FlowActivity<Input, Output>: AppCompatActivity() {

    lateinit var resolver: Resolver<Output>
    var input: Input? = null

    fun attach(resolver: Resolver<Output>) {
        this.resolver = resolver
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        load()
    }

    fun resolve(value: Output) {
        this.resolver.fulfill(value)
    }

    fun reject(error: Throwable) {
        this.resolver.reject(error)
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
        this.input = intent.getBundleExtra("FLOW_KIT")?.get("context") as Input
    }
}