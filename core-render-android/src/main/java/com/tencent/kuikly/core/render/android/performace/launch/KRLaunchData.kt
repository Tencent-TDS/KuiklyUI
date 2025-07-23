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

import org.json.JSONObject

class KRLaunchData(private val eventTimestamps: Array<Long>) {

    val initTimestamp get() = getEventTimestamp(EVENT_ON_INIT)

    val preloadClassTimestamp get() = getEventTimestamp(EVENT_ON_PRELOAD_CLASS)

    val renderCoreInitTimestamp get() = getEventTimestamp(EVENT_ON_INIT_CORE_START)

    val renderCoreInitFinishTimestamp get() = getEventTimestamp(EVENT_ON_INIT_CORE_FINISH)

    val renderContextInitStartTimestamp get() = getEventTimestamp(EVENT_ON_CONTEXT_INIT_START)

    val renderContextInitFinishTimestamp get() = getEventTimestamp(EVENT_ON_CONTEXT_INIT_FINISH)

    val createInstanceStartTimestamp get() = getEventTimestamp(EVENT_ON_CREATE_INSTANCE_START)

    val newPageStartTimestamp get() = getEventTimestamp(EVENT_ON_NEW_PAGE_START)

    val newPageFinishTimestamp get() = getEventTimestamp(EVENT_ON_NEW_PAGE_FINISH)

    val pageCreateStartTimestamp get() = getEventTimestamp(EVENT_ON_CREATE_PAGE_START)

    val pageBuildStartTimestamp get() = getEventTimestamp(EVENT_ON_PAGE_BUILD_START)

    val pageBuildFinishTimestamp get() = getEventTimestamp(EVENT_ON_PAGE_BUILD_FINISH)

    val pageLayoutStartTimestamp get() = getEventTimestamp(EVENT_ON_PAGE_LAYOUT_START)

    val pageLayoutFinishTimestamp get() = getEventTimestamp(EVENT_ON_PAGE_LAYOUT_FINISH)

    val pageCreateFinishTimestamp get() = getEventTimestamp(EVENT_ON_CREATE_PAGE_FINISH)

    val createInstanceFinishTimestamp get() = getEventTimestamp(EVENT_ON_CREATE_INSTANCE_FINISH)

    val firstFrameTimestamp get() = getEventTimestamp(EVENT_ON_FIRST_FRAME_PAINT)

    val preloadClassCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_PRELOAD_CLASS] - eventTimestamps[EVENT_ON_INIT]
        }

    val initRenderViewCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_INIT_CORE_START] - eventTimestamps[EVENT_ON_INIT]
        }

    val initRenderCoreCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_INIT_CORE_FINISH] - eventTimestamps[EVENT_ON_INIT_CORE_START]
        }

    val createInstanceCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_CREATE_INSTANCE_FINISH] - eventTimestamps[EVENT_ON_CREATE_INSTANCE_START]
        }

    val initRenderContextCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_CONTEXT_INIT_FINISH] - eventTimestamps[EVENT_ON_CONTEXT_INIT_START]
        }

    val newPageCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_NEW_PAGE_FINISH] - eventTimestamps[EVENT_ON_NEW_PAGE_START]
        }

    val pageBuildCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_PAGE_BUILD_FINISH] - eventTimestamps[EVENT_ON_PAGE_BUILD_START]
        }

    val pageLayoutCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_PAGE_LAYOUT_FINISH] - eventTimestamps[EVENT_ON_PAGE_LAYOUT_START]
        }

    val pageCreateCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_CREATE_PAGE_FINISH] - eventTimestamps[EVENT_ON_CREATE_PAGE_START]
        }

    val renderCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_FIRST_FRAME_PAINT] - eventTimestamps[EVENT_ON_CREATE_PAGE_FINISH]
        }

    val firstFramePaintCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_FIRST_FRAME_PAINT] - eventTimestamps[EVENT_ON_INIT]
        }

    /**
     * 获取事件时间戳
     */
    fun getEventTimestamp(event: Int): Long {
        if (event >= eventTimestamps.size) {
            return 0L
        }
        return eventTimestamps[event]
    }

    companion object {
        private const val KEY_FIRST_PAINT_COST = "firstPaintCost"
        private const val KEY_INIT_VIEW_COST = "initViewCost"
        private const val KEY_PRELOAD_CLASS_COST = "preloadClassCost"
        private const val KEY_FETCH_CONTEXT_CODE_COST = "fetchContextCodeCost"
        private const val KEY_INIT_RENDER_CONTEXT_COST = "initRenderContextCost"
        private const val KEY_INIT_RENDER_CORE_COST = "initRenderCoreCost"
        private const val KEY_NEW_PAGE_COST = "newPageCost"
        private const val KEY_PAGE_BUILD_COST = "pageBuildCost"
        private const val KEY_PAGE_LAYOUT_COST = "pageLayoutCost"
        private const val KEY_ON_CREATE_PAGE_COST = "createPageCost"
        private const val KEY_ON_CREATE_INSTANCE_COST = "createInstanceCost"
        private const val KEY_ON_RENDER_COST = "renderCost"

        // 事件
        const val EVENT_ON_INIT = 0
        const val EVENT_ON_PRELOAD_CLASS = EVENT_ON_INIT + 1
        const val EVENT_ON_INIT_CORE_START = EVENT_ON_PRELOAD_CLASS + 1
        const val EVENT_ON_INIT_CORE_FINISH = EVENT_ON_INIT_CORE_START + 1
        const val EVENT_ON_CONTEXT_INIT_START = EVENT_ON_INIT_CORE_FINISH + 1
        const val EVENT_ON_CONTEXT_INIT_FINISH = EVENT_ON_CONTEXT_INIT_START + 1
        const val EVENT_ON_CREATE_INSTANCE_START = EVENT_ON_CONTEXT_INIT_FINISH + 1
        const val EVENT_ON_NEW_PAGE_START = EVENT_ON_CREATE_INSTANCE_START + 1
        const val EVENT_ON_NEW_PAGE_FINISH = EVENT_ON_NEW_PAGE_START + 1
        const val EVENT_ON_CREATE_PAGE_START = EVENT_ON_NEW_PAGE_FINISH + 1
        const val EVENT_ON_PAGE_BUILD_START = EVENT_ON_NEW_PAGE_FINISH + 1
        const val EVENT_ON_PAGE_BUILD_FINISH = EVENT_ON_PAGE_BUILD_START + 1
        const val EVENT_ON_PAGE_LAYOUT_START = EVENT_ON_PAGE_BUILD_FINISH + 1
        const val EVENT_ON_PAGE_LAYOUT_FINISH = EVENT_ON_PAGE_LAYOUT_START + 1
        const val EVENT_ON_CREATE_PAGE_FINISH = EVENT_ON_PAGE_LAYOUT_FINISH + 1
        const val EVENT_ON_CREATE_INSTANCE_FINISH = EVENT_ON_CREATE_PAGE_FINISH + 1
        const val EVENT_ON_FIRST_FRAME_PAINT = EVENT_ON_CREATE_INSTANCE_FINISH + 1
        const val EVENT_ON_PAUSE = EVENT_ON_FIRST_FRAME_PAINT + 1
        const val EVENT_COUNT = EVENT_ON_PAUSE + 1
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_FIRST_PAINT_COST, firstFramePaintCost)
            put(KEY_INIT_VIEW_COST, initRenderViewCost)
            put(KEY_PRELOAD_CLASS_COST, preloadClassCost)
            put(KEY_FETCH_CONTEXT_CODE_COST, 0)
            put(KEY_INIT_RENDER_CONTEXT_COST, initRenderContextCost)
            put(KEY_INIT_RENDER_CORE_COST, initRenderCoreCost)
            put(KEY_NEW_PAGE_COST, newPageCost)
            put(KEY_PAGE_BUILD_COST, pageBuildCost)
            put(KEY_PAGE_LAYOUT_COST, pageLayoutCost)
            put(KEY_ON_CREATE_PAGE_COST, pageCreateCost)
            put(KEY_ON_CREATE_INSTANCE_COST, createInstanceCost)
            put(KEY_ON_RENDER_COST, renderCost)

        }
    }

    override fun toString() = "[KRLaunchMeta] \n" +
            "firstFramePaintCost: $firstFramePaintCost \n" +
            "   -- initRenderViewCost: $initRenderViewCost \n" +
            "       -- preloadClassCost: $preloadClassCost \n" +
            "   -- initRenderCoreCost: $initRenderCoreCost \n" +
            "   -- initRenderContextCost: $initRenderContextCost \n" +
            "   -- createInstanceCost: $createInstanceCost \n" +
            "       -- newPageCost: $newPageCost \n" +
            "       -- onPageCreateCost: $pageCreateCost \n" +
            "           -- pageBuildCost: $pageBuildCost \n" +
            "           -- pageLayoutCost: $pageLayoutCost \n" +
            "   -- renderCost: $renderCost \n"

}