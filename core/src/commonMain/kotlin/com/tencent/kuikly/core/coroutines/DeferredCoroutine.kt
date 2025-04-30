package com.tencent.kuikly.core.coroutines

import com.tencent.kuikly.core.collection.fastArrayListOf
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class DeferredCoroutine<T>(
    parentContext: CoroutineContext,
) : AbstractCoroutine<T>(parentContext), Deferred<T> {
    private var suspendCoroutineResumeTasks = fastArrayListOf<(T) -> Unit>()
    override suspend fun await(): T = awaitInternal()
    private var didSetResultValue = false
    private var resumeResultValue: T? = null
        set(value) {
            field = value
            didSetResultValue = true
        }

    override fun resumeWith(result: Result<T>) {
        if (result.isSuccess) {
            resumeResultValue = result.getOrNull()
            suspendCoroutineResumeTasks.forEach { callback ->
                @Suppress("UNCHECKED_CAST")
                callback.invoke(resumeResultValue as T)
            }
            suspendCoroutineResumeTasks.clear()
        } else {
            throw RuntimeException("result failure:" + result.exceptionOrNull())
        }
    }

    private suspend fun awaitInternal(): T {
        if (didSetResultValue) {
            @Suppress("UNCHECKED_CAST")
            return resumeResultValue as T
        }
        return awaitSuspend() // slow-path
    }

    private suspend fun awaitSuspend(): T = suspendCoroutine {
        this.suspendCoroutineResumeTasks.add { value ->
            it.resume(value)
        }
    }

}