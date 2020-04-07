//
//  Flow.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

import com.inmotionsoftware.promisekt.*

interface Flow<ARGS, RETURN> {

    val isFlowStarted: Boolean
    val isFlowPaused: Boolean

    fun args(): ARGS
    fun start(args: ARGS): FlowPromise<RETURN>
    fun pause()
    fun resume(): FlowPromise<RETURN>

    fun finish(result: RETURN)
    fun back()
    fun cancel()
    fun fail(error: Throwable)

}

fun <RETURN> Flow<Unit, RETURN>.start(): FlowPromise<RETURN> = this.start(args = Unit)
fun <ARGS> Flow<ARGS, Unit>.finish() = this.finish(result = Unit)

//
// FlowDelegation
//

open class FlowDelegation<ARGS, RETURN>: Flow<ARGS, RETURN> {

    private data class Args<out IN>(val value: IN)
    private var flowArgs: Args<ARGS>? = null
    private var deferredPromise: DeferredPromise<FlowResult<RETURN>>? = null

    override var isFlowStarted: Boolean = false
    override var isFlowPaused: Boolean = false
    override fun args(): ARGS = if (this.flowArgs != null) this.flowArgs!!.value else throw IllegalStateException()

    override fun start(args: ARGS): FlowPromise<RETURN> {
        this.flowArgs = Args(value = args)
        this.isFlowStarted = true
        val deferred = DeferredPromise<FlowResult<RETURN>>()
        this.deferredPromise = deferred
        return deferred.promise
    }

    override fun pause() {
        this.isFlowPaused = true
        this.isFlowStarted = false
    }

    override fun resume(): FlowPromise<RETURN> {
        this.isFlowPaused = false
        this.isFlowStarted = true
        return this.deferredPromise?.promise ?: throw IllegalStateException()
    }

    override fun finish(result: RETURN) {
        this.deferredPromise?.resolve(value = FlowResult.complete(value = result))
        this.cleanup()
    }

    override fun back() {
        this.deferredPromise?.resolve(value = FlowResult.back())
        this.cleanup()
    }

    override fun cancel() {
        this.deferredPromise?.resolve(value = FlowResult.cancel())
        this.cleanup()
    }
    
    override fun fail(error: Throwable) {
        this.deferredPromise?.reject(error = error)
        this.cleanup()
    }

    protected open fun cleanup() {}
}
