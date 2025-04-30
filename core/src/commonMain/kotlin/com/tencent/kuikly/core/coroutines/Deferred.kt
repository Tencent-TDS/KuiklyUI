package com.tencent.kuikly.core.coroutines

/**
 * this is a simplified implementation for [Coroutines](https://kotlinlang.org/docs/coroutines-guide.html).
 *
 * Deferred value is a non-blocking cancellable future &mdash; it is a [Job] with a result.
 */
interface Deferred<out T> : Job {

    suspend fun await(): T

}
