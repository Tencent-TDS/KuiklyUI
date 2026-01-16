package com.tencent.kuikly.core.coroutines

import com.tencent.kuikly.core.collection.fastArrayListOf
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

internal class DeferredCoroutine<T>(
    parentContext: CoroutineContext
) : AbstractCoroutine<T>(parentContext), Deferred<T> {
    private var awaiters = fastArrayListOf<Continuation<T>>()
    override suspend fun await(): T = awaitInternal()
    private var didSetResultValue = false
    private var resumeResultValue: T? = null
        set(value) {
            field = value
            didSetResultValue = true
        }
    private var resumeResultError: Throwable? = null

    override fun resumeWith(result: Result<T>) {
        if (result.isSuccess) {
            resumeResultValue = result.getOrNull()
            awaiters.forEach { c ->
                val j = c.context[Job]
                (c.context[Job] as? AbstractCoroutine<*>)?.unregisterCancellable(c)
                if (j == null || j.isActive) {
                    @Suppress("UNCHECKED_CAST")
                    c.resumeWith(Result.success(resumeResultValue as T))
                }
            }
            awaiters.clear()
            complete(null)
        } else {
            resumeResultError = result.exceptionOrNull()
            awaiters.forEach { c ->
                val j = c.context[Job]
                (c.context[Job] as? AbstractCoroutine<*>)?.unregisterCancellable(c)
                if (j == null || j.isActive) {
                    c.resumeWith(Result.failure(resumeResultError!!))
                }
            }
            awaiters.clear()
            complete(resumeResultError)
            throw RuntimeException("result failure:" + resumeResultError)
        }
    }

    private suspend fun awaitInternal(): T {
        if (didSetResultValue) {
            @Suppress("UNCHECKED_CAST")
            return resumeResultValue as T
        }
        val e = resumeResultError
        if (e != null) {
            throw e
        }
        return awaitSuspend() // slow-path
    }

    private suspend fun awaitSuspend(): T = suspendCoroutine { cont ->
        val aj = cont.context[Job] as? AbstractCoroutine<*>
        aj?.registerCancellable(cont)
        awaiters.add(cont)
    }

}
