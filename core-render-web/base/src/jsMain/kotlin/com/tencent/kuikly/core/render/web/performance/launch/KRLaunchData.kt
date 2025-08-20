package com.tencent.kuikly.core.render.web.performance.launch

import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject

class KRLaunchData(private val eventTimestamps: Array<Long>) {
    val initTimestamp get() = getEventTimestamp(EVENT_ON_INIT)

    val preloadDexClassTimestamp get() = getEventTimestamp(EVENT_ON_PRELOAD_DEX_CLASS)

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

    val preloadDexClassCost: Long
        get() {
            if (eventTimestamps.size < EVENT_COUNT) {
                return 0L
            }
            return eventTimestamps[EVENT_ON_PRELOAD_DEX_CLASS] - eventTimestamps[EVENT_ON_INIT]
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
     * Get event timestamp
     */
    fun getEventTimestamp(event: Int): Long {
        if (event >= eventTimestamps.size) {
            return 0L
        }
        return eventTimestamps[event]
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_FIRST_PAINT_COST, firstFramePaintCost)
            put(KEY_INIT_VIEW_COST, initRenderViewCost)
            put(KEY_PRELOAD_DEX_CLASS_COST, preloadDexClassCost)
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
            "       -- preloadDexClassCost: $preloadDexClassCost \n" +
            "   -- initRenderCoreCost: $initRenderCoreCost \n" +
            "   -- initRenderContextCost: $initRenderContextCost \n" +
            "   -- createInstanceCost: $createInstanceCost \n" +
            "       -- newPageCost: $newPageCost \n" +
            "       -- onPageCreateCost: $pageCreateCost \n" +
            "           -- pageBuildCost: $pageBuildCost \n" +
            "           -- pageLayoutCost: $pageLayoutCost \n" +
            "   -- renderCost: $renderCost \n"

    companion object {
        private const val KEY_FIRST_PAINT_COST = "firstPaintCost"
        private const val KEY_INIT_VIEW_COST = "initViewCost"
        private const val KEY_PRELOAD_DEX_CLASS_COST = "preloadDexClassCost"
        private const val KEY_FETCH_CONTEXT_CODE_COST = "fetchContextCodeCost"
        private const val KEY_INIT_RENDER_CONTEXT_COST = "initRenderContextCost"
        private const val KEY_INIT_RENDER_CORE_COST = "initRenderCoreCost"
        private const val KEY_NEW_PAGE_COST = "newPageCost"
        private const val KEY_PAGE_BUILD_COST = "pageBuildCost"
        private const val KEY_PAGE_LAYOUT_COST = "pageLayoutCost"
        private const val KEY_ON_CREATE_PAGE_COST = "createPageCost"
        private const val KEY_ON_CREATE_INSTANCE_COST = "createInstanceCost"
        private const val KEY_ON_RENDER_COST = "renderCost"

        // Events
        const val EVENT_ON_INIT = 0
        const val EVENT_ON_PRELOAD_DEX_CLASS = EVENT_ON_INIT + 1
        const val EVENT_ON_INIT_CORE_START = EVENT_ON_PRELOAD_DEX_CLASS + 1
        const val EVENT_ON_INIT_CORE_FINISH = EVENT_ON_INIT_CORE_START + 1
        const val EVENT_ON_CONTEXT_INIT_START = EVENT_ON_INIT_CORE_FINISH + 1
        const val EVENT_ON_CONTEXT_INIT_FINISH = EVENT_ON_CONTEXT_INIT_START + 1
        const val EVENT_ON_CREATE_INSTANCE_START = EVENT_ON_CONTEXT_INIT_FINISH + 1
        const val EVENT_ON_NEW_PAGE_START = EVENT_ON_CREATE_INSTANCE_START + 1
        const val EVENT_ON_NEW_PAGE_FINISH = EVENT_ON_NEW_PAGE_START + 1
        const val EVENT_ON_CREATE_PAGE_START = EVENT_ON_NEW_PAGE_FINISH + 1
        const val EVENT_ON_PAGE_BUILD_START = EVENT_ON_CREATE_PAGE_START + 1
        const val EVENT_ON_PAGE_BUILD_FINISH = EVENT_ON_PAGE_BUILD_START + 1
        const val EVENT_ON_PAGE_LAYOUT_START = EVENT_ON_PAGE_BUILD_FINISH + 1
        const val EVENT_ON_PAGE_LAYOUT_FINISH = EVENT_ON_PAGE_LAYOUT_START + 1
        const val EVENT_ON_CREATE_PAGE_FINISH = EVENT_ON_PAGE_LAYOUT_FINISH + 1
        const val EVENT_ON_CREATE_INSTANCE_FINISH = EVENT_ON_CREATE_PAGE_FINISH + 1
        const val EVENT_ON_FIRST_FRAME_PAINT = EVENT_ON_CREATE_INSTANCE_FINISH + 1
        const val EVENT_ON_PAUSE = EVENT_ON_FIRST_FRAME_PAINT + 1
        const val EVENT_COUNT = EVENT_ON_PAUSE + 1
    }
}
