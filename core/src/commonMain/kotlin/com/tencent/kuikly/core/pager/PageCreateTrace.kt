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

package com.tencent.kuikly.core.pager

import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.PerformanceModule
import com.tencent.kuikly.core.nvi.serialization.serialization

/**
 * 用于记录 Page 创建过程各阶段耗时
 */
class PageCreateTrace {

    companion object {
        private const val EVENT_ON_CREATE_START = "on_create_start"
        private const val EVENT_ON_CREATE_END = "on_create_end"
        private const val EVENT_ON_NEW_PAGE_START = "on_new_page_start"
        private const val EVENT_ON_NEW_PAGE_END = "on_new_page_end"
        private const val EVENT_ON_BUILD_START = "on_build_start"
        private const val EVENT_ON_BUILD_END = "on_build_end"
        private const val EVENT_ON_LAYOUT_START = "on_layout_start"
        private const val EVENT_ON_LAYOUT_END = "on_layout_end"
    }

    private var newPageStartTimeMills = -1L
    private var newPageEndTimeMills = -1L
    private var createStartTimeMills = -1L
    private var buildStartTimeMills = -1L
    private var buildEndTimeMills = -1L
    private var layoutStartTimeMills = -1L
    private var layoutEndTimeMills = -1L
    private var createEndTimeMills = -1L

    fun onCreateStart() {
        createStartTimeMills = DateTime.currentTimestamp()
    }

    /**
     * 创建 Page 实例开始
     */
    fun onNewPageStart() {
        newPageStartTimeMills = DateTime.currentTimestamp()
    }

    /**
     * 创建 Page 实例结束
     */
    fun onNewPageEnd() {
        newPageEndTimeMills = DateTime.currentTimestamp()
    }

    fun onBuildStart() {
        buildStartTimeMills = DateTime.currentTimestamp()
    }

    fun onBuildEnd() {
        buildEndTimeMills = DateTime.currentTimestamp()
    }

    fun onLayoutStart() {
        layoutStartTimeMills = DateTime.currentTimestamp()
    }

    fun onLayoutEnd() {
        layoutEndTimeMills = DateTime.currentTimestamp()
    }

    fun onCreateEnd() {
        createEndTimeMills = DateTime.currentTimestamp()
        PagerManager.getCurrentPager().addNextTickTask { // 首屏的下一帧回调给Native侧，优化首屏速度
            PagerManager.getCurrentPager()
                .acquireModule<PerformanceModule>(PerformanceModule.MODULE_NAME)
                .onPageCreateFinish(this)
        }
    }

    fun dump(): String {
        return hashMapOf<String, Long>().apply {
            put(EVENT_ON_CREATE_START, createStartTimeMills)
            put(EVENT_ON_CREATE_END, createEndTimeMills)
            put(EVENT_ON_BUILD_START, buildStartTimeMills)
            put(EVENT_ON_BUILD_END, buildEndTimeMills)
            put(EVENT_ON_LAYOUT_START, layoutStartTimeMills)
            put(EVENT_ON_LAYOUT_END, layoutEndTimeMills)
            put(EVENT_ON_NEW_PAGE_START, newPageStartTimeMills)
            put(EVENT_ON_NEW_PAGE_END, newPageEndTimeMills)
        }.serialization().toString()
    }

}