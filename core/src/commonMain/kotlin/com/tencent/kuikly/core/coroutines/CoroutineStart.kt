package com.tencent.kuikly.core.coroutines

import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine

/**
 * this is a simplified implementation for [Coroutines](https://kotlinlang.org/docs/coroutines-guide.html).
 *
 * Defines start options for coroutines builders.
 * It is used in `start` parameter of [launch][CoroutineScope.launch], [async][CoroutineScope.async], and other coroutine builder functions.
 */
enum class CoroutineStart {
    /**
     * Default -- immediately schedules the coroutine for execution according to its context.
     */
    ATOMIC;

    operator fun <R, T> invoke(
        block: suspend R.() -> T,
        receiver: R,
        completion: Continuation<T>
    ): Unit =
        when (this) {
            ATOMIC -> block.startCoroutine(receiver, completion)
        }
}