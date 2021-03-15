package com.inmotionsoftware.flowkit.android
import android.icu.util.Output
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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


class FlowViewModelFactory<Input, Output>(val input: Input, val resolver: Resolver<Output>): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Object::class.java, Resolver::class.java).newInstance(input, resolver)
    }
}

class FlowViewModel<I, O>(input: I, var resolver: Resolver<O>): ViewModel() {
    val input = MutableLiveData<I>(input)
}

abstract class FlowFragment<Input, Output>: Fragment(), Backable {
    private val viewModel: FlowViewModel<Input,Output> by lazy {
        StateMachineViewModelProvider.of(activity=this.requireActivity(), fragment=this)
    }

    abstract fun onInputAttached(input: Input)

    fun resolve(result: com.inmotionsoftware.promisekt.Result<Output>) {
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

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.viewModel.input.value?.let {
            this.onInputAttached(input = it)
        }

        this.viewModel.input.observe(requireActivity(), Observer {
            this.onInputAttached(input=it)
        })
    }
}
