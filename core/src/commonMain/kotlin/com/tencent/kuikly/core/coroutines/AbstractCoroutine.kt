package com.tencent.kuikly.core.coroutines

import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.global.GlobalFunctions
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

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

    private var active = true
    private val completionHandlers = fastArrayListOf<(Throwable?) -> Unit>()
    private val timeoutRefs = fastArrayListOf<Pair<String, String>>()
    private val cancellables = fastArrayListOf<Continuation<*>>()

    override val isActive: Boolean get() = active
    /**
     * Cancels this coroutine and marks it as inactive.
     *
     * This method is idempotent. It destroys all pending timeout callbacks
     * that were registered via suspend points (e.g. [delay]) and then
     * invokes all completion handlers with the provided [cause].
     *
     * @param cause optional cancellation/termination cause; `null` for normal cancel.
     */
    override fun cancel(cause: Throwable?) {
        if (!active) {
            return
        }
        active = false
        timeoutRefs.forEach { pair ->
            GlobalFunctions.destroyGlobalFunction(pair.first, pair.second)
        }
        timeoutRefs.clear()
        val ex = if (cause is CancellationException) {
            cause
        } else {
            CancellationException("cancelled", cause)
        }
        cancellables.forEach { c ->
            try {
                c.resumeWith(Result.failure(ex))
            } catch (_: Throwable) {
            }
        }
        cancellables.clear()
        completionHandlers.forEach { it.invoke(cause) }
        completionHandlers.clear()
    }

    /**
     * Registers a completion handler to be invoked when this coroutine completes
     * (either normally, with exception, or via [cancel]).
     *
     * @param handler completion callback receiving the termination [Throwable] or `null`.
     */
    internal fun addCompletionHandler(handler: (Throwable?) -> Unit) {
        completionHandlers.add(handler)
    }

    /**
     * Associates a timeout reference created by a suspend point (e.g. [delay])
     * with this Job so that it can be destroyed upon [cancel].
     *
     * @param pagerId owning pager id used by the timeout registration.
     * @param ref timeout function reference returned by setTimeout.
     */
    internal fun registerTimeout(pagerId: String, ref: String) {
        timeoutRefs.add(pagerId to ref)
    }

    /**
     * Removes a previously associated timeout reference from this Job.
     * Should be called by the suspend point once the timeout has fired.
     *
     * @param ref timeout function reference returned by setTimeout.
     */
    internal fun unregisterTimeout(ref: String) {
        val iterator = timeoutRefs.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.second == ref) {
                iterator.remove()
                break
            }
        }
    }

    internal fun registerCancellable(cont: Continuation<*>) {
        cancellables.add(cont)
    }

    internal fun unregisterCancellable(cont: Continuation<*>) {
        cancellables.remove(cont)
    }

    /**
     * Starts this coroutine with the given code [block] and [start] strategy.
     * This function shall be invoked at most once on this coroutine.
     */
    open fun <R> start(start: CoroutineStart, receiver: R, block: suspend R.() -> T) {
        start(block, receiver, this)
    }

    /**
     * Notifies all registered completion handlers.
     * Internal use for both normal and exceptional completion paths.
     *
     * @param cause termination cause or `null` for normal completion.
     */
    protected fun complete(cause: Throwable?) {
        active = false
        completionHandlers.forEach { it.invoke(cause) }
        completionHandlers.clear()
    }
}
