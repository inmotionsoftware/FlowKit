package com.inmotionsoftware.example
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.inmotionsoftware.flowkit.Flow
import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.promisekt.*
import java.lang.RuntimeException

interface Backable {
    fun onBackPressed()
}

abstract class FlowFragment<Input, Output>: Fragment(), Backable {

    lateinit var resolver: Resolver<Output>
    var input: Input? = null

    fun attach(resolver: Resolver<Output>) {
        this.resolver = resolver
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