/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.render.android.scheduler

import android.os.Handler
import android.os.Looper
import com.tencent.kuikly.core.render.android.IKuiklyRenderViewTreeUpdateListener
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.exception.IKuiklyRenderExceptionListener

/**
 * KTV页面UI线程调度器
 */
class KuiklyRenderCoreUIScheduler(
    private val preRunKuiklyRenderCoreUITask: PreRunKuiklyRenderCoreTask? = null
) : IKuiklyRenderCoreScheduler {
    /**
     *  Context线程上的主线程任务集合
     */
    private var mainThreadTasksOnContextQueue: MutableList<KuiklyRenderCoreTaskExecutor>? = null
    /**
     * 主线程上的任务集合
     */
    private var mainThreadTasks = mutableListOf<KuiklyRenderCoreTaskExecutor>()
    /**
     * 待批量同步主线程任务任务闭包，用于保证一个runLoop中，不管[scheduleTask]调用多少次，最后只会批量调度一次
     */
    private var needSyncMainQueueTasksBlock : ((sync: Boolean) -> Unit)? = null
    /*
     * 需要立即回到主线程执行的同步主线程执行任务闭包
     */
    var mainThreadTaskWaitToSyncBlock : (() -> Unit)? = null
    /*
     *  是否执行主线程任务中
     */
    var isPerformingMainQueueTask = false
     private set
    private val uiHandler by lazy {
        Handler(Looper.getMainLooper())
    }
    /*
     * 首屏视图是否加载完
     */
    private var viewDidLoad = false
    /*
     * 主线程上的任务集合
     */
    private val viewDidLoadMainThreadTasks = mutableListOf<KuiklyRenderCoreTask>()

    /**
     * ViewTree 更新事件监听
     */
    private var viewTreeUpdateListener: IKuiklyRenderViewTreeUpdateListener? = null

    /**
     * 异常监听
     */
    private var exceptionListener: IKuiklyRenderExceptionListener? = null

    override fun scheduleTask(delayMs: Long, task: KuiklyRenderCoreTask) {
        scheduleTask(delayMs, false, task)
    }

    /**
     * 添加 UI 更新任务
     */
    fun scheduleTask(delayMs: Long = 0, isUpdateViewTree: Boolean = false, task: KuiklyRenderCoreTask) {
        addTaskToMainQueue(KuiklyRenderCoreTaskExecutor(task, isUpdateViewTree))
    }

    override fun destroy() {
        uiHandler.removeCallbacksAndMessages(null)
    }

    fun setViewTreeUpdateListener(listener: IKuiklyRenderViewTreeUpdateListener) {
        viewTreeUpdateListener = listener
    }

    fun setRenderExceptionListener(listener: IKuiklyRenderExceptionListener?) {
        exceptionListener = listener
    }

    fun performSyncMainQueueTasksBlockIfNeed(sync: Boolean) {
        if (needSyncMainQueueTasksBlock != null) {
            needSyncMainQueueTasksBlock?.invoke(sync)
            needSyncMainQueueTasksBlock = null
        }
    }

    fun performMainThreadTaskWaitToSyncBlockIfNeed() {
        if (mainThreadTaskWaitToSyncBlock != null) {
            mainThreadTaskWaitToSyncBlock?.invoke()
            mainThreadTaskWaitToSyncBlock = null
        }
    }

    // 首屏完成在执行任务
    fun performWhenViewDidLoad(task: KuiklyRenderCoreTask) {
        assert(isMainThread())
        if (viewDidLoad) {
            task()
        } else {
            viewDidLoadMainThreadTasks.add(task)
        }
    }

    private fun addTaskToMainQueue(task: KuiklyRenderCoreTaskExecutor) {
        assert(!isMainThread())
        val tasks = mainThreadTasksOnContextQueue ?: mutableListOf<KuiklyRenderCoreTaskExecutor>().apply {
            mainThreadTasksOnContextQueue = this
        }
        tasks.add(task)
        if (task.isUpdateViewTree) {
            viewTreeUpdateListener?.onUpdateViewTreeEnqueued()
        }
        setNeedSyncMainQueueTasks()
    }

    private fun setNeedSyncMainQueueTasks() {
        assert(!isMainThread())
        if (needSyncMainQueueTasksBlock != null) {
            return
        }
        needSyncMainQueueTasksBlock = { sync ->
            assert(!isMainThread())
            preRunKuiklyRenderCoreUITask?.invoke()
            val performTasks = mainThreadTasksOnContextQueue
            mainThreadTasksOnContextQueue = null
            synchronized(this) {
                mainThreadTasks.addAll(performTasks?.toList() ?: listOf())
            }
            performOnMainQueueWithTask(sync = sync) {
                var tasks : List<KuiklyRenderCoreTaskExecutor>?
                synchronized(this) {
                    tasks = mainThreadTasks.toList()
                    mainThreadTasks.clear()
                }
                runMainQueueTasks(tasks)
            }
        }
        KuiklyRenderCoreContextScheduler.scheduleTask {
            performSyncMainQueueTasksBlockIfNeed(false)
        }
    }

    fun performOnMainQueueWithTask(sync : Boolean, task: ()-> Unit) {
        if (sync) {
            if (isMainThread()) {
                task()
            } else {
                // 当前子线程等到主线程可能发生死锁，暂用闭包等后面立即回到主线程处理
                mainThreadTaskWaitToSyncBlock = task
            }
        } else {
            uiHandler.post {
                task()
            }
        }
    }

    private fun runMainQueueTasks(tasks: List<KuiklyRenderCoreTaskExecutor>?) {
        assert(isMainThread()) {
            "must call on ui thread"
        }
        try {
            val uiTasks = tasks ?: return
            isPerformingMainQueueTask = true
            for (task in uiTasks) {
                task.execute()
                if (task.isUpdateViewTree) {
                    viewTreeUpdateListener?.onUpdateViewTreeFinish()
                }
            }
            isPerformingMainQueueTask = false
        } catch (e : Exception) {
            exceptionListener?.onRenderException(e, ErrorReason.UPDATE_VIEW_TREE)
        }
        isPerformingMainQueueTask = false
        if(!viewDidLoad) {
            viewDidLoad = true
            performViewDidLoadTasksIfNeed()
        }
    }

    // perform all wait to viewDidLoad tasks
    private fun performViewDidLoadTasksIfNeed() {
        performOnMainQueueWithTask(sync = false) {
            for (task in viewDidLoadMainThreadTasks.toList()) {
                task()
            }
            viewDidLoadMainThreadTasks.clear()
        }
    }
    companion object

}

/**
 * 执行任务包装类，用于区分是否为更新 UI 的任务
 */
class KuiklyRenderCoreTaskExecutor(
    private val task: KuiklyRenderCoreTask,
    val isUpdateViewTree: Boolean) {

    fun execute() {
        task.invoke()
    }

}
