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

package com.tencent.kuikly.core.render.android.performace.launch

import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.expand.module.PageCreateTrace
import com.tencent.kuikly.core.render.android.performace.KRMonitor
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_COUNT
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_CREATE_PAGE_FINISH
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_CREATE_PAGE_START
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_FIRST_FRAME_PAINT
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_INIT
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_NEW_PAGE_FINISH
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_NEW_PAGE_START
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_PAGE_BUILD_FINISH
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_PAGE_BUILD_START
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_PAGE_LAYOUT_FINISH
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_PAGE_LAYOUT_START
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_PAUSE
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_PRELOAD_CLASS
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_CONTEXT_INIT_FINISH
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_CONTEXT_INIT_START
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_INIT_CORE_FINISH
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_INIT_CORE_START
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_CREATE_INSTANCE_FINISH
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData.Companion.EVENT_ON_CREATE_INSTANCE_START

/**
 * 启动性能追踪
 */
class KRLaunchMonitor: KRMonitor<KRLaunchData>() {

    companion object {
        const val MONITOR_NAME = "KRLaunchMonitor"
    }

    private val eventTimestamps = Array(EVENT_COUNT){ 0L }

    private var listeners = mutableListOf<KRLaunchDataListener>()

    private var hasNotifyListener = false

    init {
        eventTimestamps[EVENT_ON_PAUSE] = Long.MAX_VALUE
    }

    override fun name(): String = MONITOR_NAME

    override fun onInit() {
        eventTimestamps[EVENT_ON_INIT] = System.currentTimeMillis()
    }

    override fun onPreloadClassFinish() {
        eventTimestamps[EVENT_ON_PRELOAD_CLASS] = System.currentTimeMillis()
    }

    override fun onInitCoreStart() {
        eventTimestamps[EVENT_ON_INIT_CORE_START] = System.currentTimeMillis()
    }

    override fun onInitCoreFinish() {
        eventTimestamps[EVENT_ON_INIT_CORE_FINISH] = System.currentTimeMillis()
    }

    override fun onInitContextStart() {
        eventTimestamps[EVENT_ON_CONTEXT_INIT_START] = System.currentTimeMillis()
    }

    override fun onInitContextFinish() {
        eventTimestamps[EVENT_ON_CONTEXT_INIT_FINISH] = System.currentTimeMillis()
    }

    override fun onCreateInstanceStart() {
        eventTimestamps[EVENT_ON_CREATE_INSTANCE_START] = System.currentTimeMillis()
    }

    override fun onCreateInstanceFinish() {
        eventTimestamps[EVENT_ON_CREATE_INSTANCE_FINISH] = System.currentTimeMillis()
    }

    /**
     * Kotlin 侧回调
     */
    fun onPageCreateFinish(createTrace: PageCreateTrace?) {
        KuiklyRenderLog.d(MONITOR_NAME, "--onPageCreateFinish--")
        createTrace?.let {
            eventTimestamps[EVENT_ON_CREATE_PAGE_START] = createTrace.createStartTimeMills
            eventTimestamps[EVENT_ON_PAGE_BUILD_START] = createTrace.buildStartTimeMills
            eventTimestamps[EVENT_ON_PAGE_BUILD_FINISH] = createTrace.buildEndTimeMills
            eventTimestamps[EVENT_ON_PAGE_LAYOUT_START] = createTrace.layoutStartTimeMills
            eventTimestamps[EVENT_ON_PAGE_LAYOUT_FINISH] = createTrace.layoutEndTimeMills
            eventTimestamps[EVENT_ON_NEW_PAGE_START] = createTrace.newPageStartTimeMills
            eventTimestamps[EVENT_ON_NEW_PAGE_FINISH] = createTrace.newPageEndTimeMills
            eventTimestamps[EVENT_ON_CREATE_PAGE_FINISH] = createTrace.createEndTimeMills
        }
        // pageCreateFinish 是异步回调，不保证时序性，尝试通知
        tryNotifyListener()
    }

    override fun onFirstFramePaint() {
        eventTimestamps[EVENT_ON_FIRST_FRAME_PAINT] = System.currentTimeMillis()
        // pageCreateFinish 是异步回调，不保证时序性，尝试通知
        tryNotifyListener()
    }

    override fun onPause() {
        // onPause 事件作为哨兵，如果启动期间有 onPause 事件则不上报
        eventTimestamps[EVENT_ON_PAUSE] = System.currentTimeMillis()
    }

    override fun onDestroy() {
        listeners.clear()
    }

    override fun getMonitorData(): KRLaunchData? {
        val isValid = checkIsValidTimestamps()
        if (!isValid) {
            return null
        }
        return KRLaunchData(eventTimestamps.clone())
    }

    /**
     * 判断是否为有效的时间戳
     */
    private fun checkIsValidTimestamps(): Boolean {
        eventTimestamps.forEachIndexed { index, value ->
            if (value <= 0L) {
                KuiklyRenderLog.i(MONITOR_NAME, "timestamp is invalid:[$index] $value")
                return false
            }
        }
        return true
    }

    /**
     * 分发启动数据
     */
    private fun tryNotifyListener() {
        if (hasNotifyListener) {
            return
        }
        getMonitorData()?.let { launchData ->
            hasNotifyListener = true
            listeners.forEach { listener ->
                listener.invoke(launchData)
            }
        }
    }

    /**
     * 设置启动数据回调监听
     */
    fun addListener(listener: KRLaunchDataListener) {
        listeners.add(listener)
    }

    /**
     * 删除启动数据回调监听
     */
    fun removeListener(listener: KRLaunchDataListener) {
        listeners.remove(listener)
    }

}

typealias KRLaunchDataListener = (data: KRLaunchData) -> Unit