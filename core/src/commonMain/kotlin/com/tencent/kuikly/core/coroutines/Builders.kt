package com.tencent.kuikly.core.coroutines

import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.timer.setTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Launches a new coroutine without blocking the current thread and returns a reference to the coroutine as a [Job].
 *
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.ATOMIC].
 * @param block the coroutine code which will be invoked in the context of the provided scope.
 **/
fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.ATOMIC,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val job = StandaloneCoroutine(context)
    job.start(start, this) {
        try {
            block.invoke(this)
        } catch (e: Throwable) {
            throwCoroutineScopeException(e)
        }
    }
    return job
}

fun <T> CoroutineScope.async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.ATOMIC,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    val job = DeferredCoroutine<T>(context)
    job.start(start, this) {
        try {
            block.invoke(this)
        } catch (e: Throwable) {
            throwCoroutineScopeException(e)
            val res: T? = null
            res!!// 解决编译返回，不影响报错结果
        }
    }
    return job
}

suspend fun CoroutineScope.delay(timeMs: Int) {
    val pagerId = if (this is LifecycleScope) {
        pagerScope.pagerId
    } else {
        @Suppress("DEPRECATION")
        BridgeManager.currentPageId.ifEmpty { return }
    }
    suspendCoroutine {
        setTimeout(pagerId, timeMs) {
            try {
                it.resume(Unit)
            } catch (e: Throwable) {
                throwCoroutineScopeException(e)
            }
        }
    }
}

/**
 * 协程内的异常统一使用该接口抛出，否则在协程内异常抛出无效。
 */
private fun CoroutineScope.throwCoroutineScopeException(e: Throwable) {
    val pagerId = if (this is LifecycleScope) {
        pagerScope.pagerId
    } else {
        @Suppress("DEPRECATION")
        BridgeManager.currentPageId.ifEmpty { return }
    }
    setTimeout(pagerId) { // 解决协程作用域内异常throw无效，故下一帧非协程内抛出
        throw e
    }
}

object GlobalScope : CoroutineScope {
    /**
     * Returns [EmptyCoroutineContext].
     */
    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext
}

class LifecycleScope(internal val pagerScope: PagerScope) : CoroutineScope {
    /**
     * Returns [EmptyCoroutineContext].
     */
    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext
}
