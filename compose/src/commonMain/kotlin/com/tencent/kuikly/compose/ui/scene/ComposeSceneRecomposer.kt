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
     * 执行与官方实现不同的 KuiklyUI 特殊销毁逻辑.
     *
     * Kuikly 的每个 Pager 绑定独立 [CoroutineDispatcher]，需在销毁时彻底清空任务队列，
     * 避免因残留任务导致内存泄漏或意外行为。
     *
     * 实现要点：
     * 1. 强制清空调度器队列中的待执行任务
     * 2. 终止关联的协程作用域
     *
     * @see performScheduledTasks()  清空计划任务队列
     * @see performScheduledEffects() 清理副作用调度
     * @receiver UiController 生命周期控制器
     */
    fun cancel() {
        recomposer.cancel()       // 终止 Compose 重组器
        job.cancel()              // 取消关联协程作业
        performScheduledTasks()   // 清空调度器任务
        performScheduledEffects() // 清理副作用
    }
}