// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a 
// copy of this software and associated documentation files (the "Software"), 
// to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, 
// and/or sell copies of the Software, and to permit persons to whom the 
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.
//
//  FlowDispatcher.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
package com.inmotionsoftware.flowkit.android

import android.content.Intent
import com.inmotionsoftware.flowkit.Result
import java.lang.NullPointerException
import java.util.*
import kotlin.random.Random

typealias ActivitResultDelegate = (resultCode: Int, data: Intent?) -> Unit

fun <O2> getResult(intent: Intent): Result<O2> {
    val r = intent.getBundleExtra(FLOW_KIT_ACTIVITY_RESULT)?.get("result")

    // FIXME: what if O2 is a throwable??
    if (r is Throwable) {
        return Result.Failure<O2>(r)
    }

    val v: O2? = r as? O2
    return if (v == null) {
        val e = r as? Throwable
        if (e != null) {
            Result.Failure<O2>(e)
        } else {
            Result.Failure<O2>(NullPointerException())
        }
    } else {
        Result.Success(v)
    }
}

object FlowDispatcher {
    private val registry = mutableMapOf<Int,ActivitResultDelegate>()
    private val rand = Random(Date().time)
    private var counter = 0

    private fun nextRequestCode(): Int {
        return (rand.nextInt() and 0x0000FF00) or (++counter)
    }

    fun register(delegate: ActivitResultDelegate): Int {
        val code = nextRequestCode()
        registry.put(code, delegate)
        return code
    }

    fun dispatch(requestCode: Int, resultCode: Int, data: Intent?) {
        registry.remove(requestCode)?.invoke(resultCode, data)
    }
}
