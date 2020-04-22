package com.inmotionsoftware.flowkit

import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.recover

fun <T> Promise<T>.back(closure: () -> T ): Promise<T> =
    canceled {
        when (it) {
            is FlowError.Back -> closure()
            is FlowError.Canceled -> throw it
        }
    }


fun <T> Promise<T>.cancel(closure: () -> T ): Promise<T> =
    canceled {
        when (it) {
            is FlowError.Back -> throw it
            is FlowError.Canceled -> closure()
        }
    }

fun <T> Promise<T>.canceled(closure: (FlowError) -> T ): Promise<T> =
    this.recover {
        if (it is FlowError) {
            Promise.value(closure(it))
        } else {
            throw it
        }
    }