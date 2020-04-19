package com.inmotionsoftware.flowkit.android
import android.os.Bundle
import android.util.Log
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.promisekt.Resolver
import com.inmotionsoftware.promisekt.fulfill
import com.inmotionsoftware.promisekt.reject

interface Backable {
    fun onBackPressed()
}

abstract class FlowFragment<Input, Output>: Fragment(), Backable {

    lateinit var resolver: Resolver<Output>
    var input: Input? = null

    fun attach(resolver: Resolver<Output>) {
        this.resolver = resolver
    }

    fun resolve(result: Result<Output>) {
        if (!this::resolver.isInitialized) {
            Log.e(FlowFragment::class.java.name, "Resolver has not been initialized")
            return
        }
        this.resolver.resolve(result)
    }

    fun resolve(value: Output) {
        resolve(Result.fulfilled(value))
    }

    fun reject(error: Throwable) {
        resolve(Result.rejected(error))
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
        arguments?.let {
            this.input = this.arguments?.get("context") as? Input
        }
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
}