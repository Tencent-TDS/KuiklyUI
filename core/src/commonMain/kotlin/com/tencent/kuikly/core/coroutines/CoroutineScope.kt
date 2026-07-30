package com.tencent.kuikly.core.coroutines

import kotlin.coroutines.CoroutineContext

/**
 * this is a simplified implementation for [Coroutines](https://kotlinlang.org/docs/coroutines-guide.html).
 *
 * Defines a scope for new coroutines. Every **coroutine builder** (like [launch], [async], etc.)
 * is an extension on [CoroutineScope] and inherits its [coroutineContext][CoroutineScope.coroutineContext]
 * to automatically propagate all its elements and cancellation.
 */
interface CoroutineScope {
    /**
     * The context of this scope.
     * Context is encapsulated by the scope and used for implementation of coroutine builders that are extensions on the scope.
     * Accessing this property in general code is not recommended for any purposes except accessing the [Job] instance for advanced usages.
     *
     * By convention, should contain an instance of a [job][Job] to enforce structured concurrency.
     */
    val coroutineContext: CoroutineContext
}