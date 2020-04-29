package com.inmotionsoftware.flowkit.android
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inmotionsoftware.flowkit.Flow
import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.promisekt.Resolver
import com.inmotionsoftware.promisekt.fulfill
import com.inmotionsoftware.promisekt.reject
import com.inmotionsoftware.promisekt.resolve

interface Backable {
    fun onBackPressed()
}

class FlowViewModel<I, O>: ViewModel() {
    val input = MutableLiveData<I>()
    lateinit var resolver: Resolver<O>
}

abstract class FlowFragment<Input, Output>: Fragment(), Backable {
    lateinit var viewModel: FlowViewModel<Input, Output>
    val input: Input? get() { return viewModel.input.value }

    fun resolve(result: com.inmotionsoftware.promisekt.Result<Output>) {
        if (!this::viewModel.isInitialized) {
            Log.e(FlowFragment::class.java.name, "Resolver has not been initialized")
            return
        }

        viewModel.resolver.resolve(result)
    }

    fun resolve(value: Output) {
        resolve(com.inmotionsoftware.promisekt.Result.fulfilled(value))
    }

    fun reject(error: Throwable) {
        resolve(com.inmotionsoftware.promisekt.Result.rejected(error))
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

    private fun loadViewModel() {
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(requireActivity()).get(FlowViewModel::class.java) as FlowViewModel<Input,Output>
        Log.d(this.javaClass.name, "input: ${viewModel.input.value}")
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadViewModel()
        // TODO: Load bundle
    }
}
