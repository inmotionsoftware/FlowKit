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
