//
//  FlowPromise.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map

typealias FlowPromise<R> = Promise<FlowResult<R>>

fun <T, R> Promise<T>.back(execute: () -> Unit): Promise<T> where T: FlowResult<R> {
    return this.map { result ->
        if (result.isBack) execute()
        return@map result
    }
}

fun <T, R> Promise<T>.cancel(execute: () -> Unit): Promise<T> where T: FlowResult<R> {
    return this.map { result ->
        if (result.isCancel) execute()
        return@map result
    }
}

fun <T, R> Promise<T>.complete(execute: (R) -> Unit): Promise<T> where T: FlowResult<R> {
    return this.map { result ->
        val rt = result.isComplete
        if (rt.first) execute(rt.second!!)
        return@map result
    }
}
