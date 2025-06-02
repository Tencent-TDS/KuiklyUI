package com.tencent.kuikly.core.coroutines

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

/**
 * this is a simplified implementation for [Coroutines](https://kotlinlang.org/docs/coroutines-guide.html).
 *
 * @suppress **This an internal API and should not be used from general code.**
 */
internal abstract class AbstractCoroutine<in T>(
    parentContext: CoroutineContext
) : Job, Continuation<T>, CoroutineScope {

    @Suppress("LeakingThis")
    final override val context: CoroutineContext = parentContext + this
    final override val key: CoroutineContext.Key<*> get() = Job

    /**
     * The context of this scope which is the same as the [context] of this coroutine.
     */
    override val coroutineContext: CoroutineContext get() = context

    /**
     * Starts this coroutine with the given code [block] and [start] strategy.
     * This function shall be invoked at most once on this coroutine.
     */
    open fun <R> start(start: CoroutineStart, receiver: R, block: suspend R.() -> T) {
        start(block, receiver, this)
    }
}
