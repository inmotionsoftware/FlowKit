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


class FlowViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor().newInstance()
    }
}

class FlowViewModel<I, O>: ViewModel() {
    private var _resolver: Resolver<O>? = null
    val resolver: Resolver<O>
        get() = _resolver!!

    val input = MutableLiveData<I>()

    fun init(inputValue: I, resolver: Resolver<O>) {
        input.value = inputValue
        _resolver = resolver
    }
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.viewModel.input.value?.let {
            this.onInputAttached(input = it)
        }

        this.viewModel.input.observe(requireActivity(), Observer {
            this.onInputAttached(input=it)
        })
    }
}
