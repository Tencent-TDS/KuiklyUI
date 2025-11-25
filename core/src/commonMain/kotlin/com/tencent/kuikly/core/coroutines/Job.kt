package com.tencent.kuikly.core.coroutines

import kotlin.coroutines.CoroutineContext

/**
 * this is a simplified implementation for [Coroutines](https://kotlinlang.org/docs/coroutines-guide.html).
 *
 * A background job. Conceptually, a job is a cancellable thing with a life-cycle that
 * culminates in its completion.
 *
 * The most basic instances of `Job` interface are created like this:
 *
 * * **Coroutine job** is created with [launch][CoroutineScope.launch] coroutine builder.
 *   It runs a specified block of code and completes on completion of this block.
 *
 * Conceptually, an execution of a job does not produce a result value. Jobs are launched solely for their
 * side-effects. See [Deferred] interface for a job that produces a result.
 * */
interface Job : CoroutineContext.Element {
    /**
     * Key for [Job] instance in the coroutine context.
     */
    companion object Key : CoroutineContext.Key<Job>

    /**
     * Returns `true` if this Job is currently active (not cancelled or completed).
     */
    val isActive: Boolean

    /**
     * Cancels this Job. This operation is idempotent.
     *
     * Cancelling a Job prevents future resume of pending suspend points
     * and triggers completion handlers with the provided [cause].
     *
     * @param cause optional cancellation/termination cause; `null` for normal cancel.
     */
    fun cancel(cause: Throwable? = null)

}
