/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.ui.scene

import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import com.tencent.kuikly.compose.ui.platform.FlushCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * The scheduler for performing recomposition and applying updates to one or more [Composition]s.
 *
 * The main difference from [Recomposer] is separate dispatchers for LaunchEffect and other
 * recompositions that allows more precise status checking.
 *
 * @param coroutineContext The coroutine context to use for the compositor.
 * @param elements Additional coroutine context elements to include in context.
 */
internal class ComposeSceneRecomposer(
    coroutineContext: CoroutineContext,
    vararg elements: CoroutineContext.Element
) {
    private val job = Job()
    private val coroutineScope = CoroutineScope(coroutineContext + job)

    /**
     * We use [FlushCoroutineDispatcher] not because we need [flush] for
     * LaunchEffect tasks, but because we need to know if it is idle (hasn't scheduled tasks)
     */
    private val effectDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val recomposeDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val recomposer = Recomposer(coroutineContext + job + effectDispatcher)

    /**
     * `true` if there is any pending work scheduled, regardless of whether it is currently running.
     */
    val hasPendingWork: Boolean
        get() = recomposer.hasPendingWork ||
                effectDispatcher.hasTasks() ||
                recomposeDispatcher.hasTasks()

    val compositionContext: CompositionContext
        get() = recomposer

    init {
        var context: CoroutineContext = recomposeDispatcher
        for (element in elements) {
            context += element
        }
        coroutineScope.launch(
            context,
            start = CoroutineStart.UNDISPATCHED
        ) {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    /**
     * Perform all scheduled tasks and wait for the tasks which are already
     * performing in the recomposition scope.
     */
    fun performScheduledTasks() {
        recomposeDispatcher.flush()
    }

    /**
     * Perform all scheduled effects.
     */
    fun performScheduledEffects() {
        effectDispatcher.flush()
    }

    /**
     * Permanently shut down this [ComposeSceneRecomposer].
     *
     * Kuikly 的每个 Pager 绑定独立的 [kotlinx.coroutines.CoroutineDispatcher]，
     * Pager 销毁后该 dispatcher 不再调度新任务，因此必须同步清空队列，
     * 否则残留的 [Runnable] 引用将无法被 GC 回收，导致内存泄漏。
     *
     * 使用 [FlushCoroutineDispatcher.drainSafely] 而非 [FlushCoroutineDispatcher.flush]：
     * [BaseComposeScene.close] 在调用 cancel() 之前已执行 composition.dispose()，
     * 该操作会取消 LaunchedEffect 协程，产生已取消的 DispatchedTask 进入队列。
     * flush() 同步执行这些 task 时，DispatchedTask.run() 内的 callOnCancellation
     * 会抛出 CancellationException 导致 crash。drainSafely() 对每个 task 单独
     * 捕获 CancellationException，确保队列被完整清空且不会崩溃。
     */
    fun cancel() {
        recomposer.cancel()
        job.cancel()
        recomposeDispatcher.drainSafely()
        effectDispatcher.drainSafely()
    }
}

