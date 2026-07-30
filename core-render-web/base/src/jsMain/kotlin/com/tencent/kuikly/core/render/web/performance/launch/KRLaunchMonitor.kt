package com.tencent.kuikly.core.render.web.performance.launch

import com.tencent.kuikly.core.render.web.expand.module.PageCreateTrace
import com.tencent.kuikly.core.render.web.performance.KRMonitor
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_COUNT
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_CONTEXT_INIT_FINISH
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_CONTEXT_INIT_START
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_CREATE_INSTANCE_FINISH
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_CREATE_INSTANCE_START
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_CREATE_PAGE_FINISH
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_CREATE_PAGE_START
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_FIRST_FRAME_PAINT
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_INIT
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_INIT_CORE_FINISH
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_INIT_CORE_START
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_NEW_PAGE_FINISH
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_NEW_PAGE_START
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_PAGE_BUILD_FINISH
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_PAGE_BUILD_START
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_PAGE_LAYOUT_FINISH
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_PAGE_LAYOUT_START
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_PAUSE
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData.Companion.EVENT_ON_PRELOAD_DEX_CLASS
import com.tencent.kuikly.core.render.web.utils.Log
import kotlin.js.Date

/**
 * Launch performance monitoring
 */
class KRLaunchMonitor : KRMonitor<KRLaunchData>() {
    private val eventTimestamps = Array(EVENT_COUNT) { 0L }

    private var listeners = mutableListOf<KRLaunchDataListener>()

    private var hasNotifyListener = false

    init {
        eventTimestamps[EVENT_ON_PAUSE] = Long.MAX_VALUE
    }

    override fun name(): String = MONITOR_NAME

    override fun onInit() {
        eventTimestamps[EVENT_ON_INIT] = Date.now().toLong()
    }

    override fun onPreloadDexClassFinish() {
        eventTimestamps[EVENT_ON_PRELOAD_DEX_CLASS] = Date.now().toLong()
    }

    override fun onInitCoreStart() {
        eventTimestamps[EVENT_ON_INIT_CORE_START] = Date.now().toLong()
    }

    override fun onInitCoreFinish() {
        eventTimestamps[EVENT_ON_INIT_CORE_FINISH] = Date.now().toLong()
    }

    override fun onInitContextStart() {
        eventTimestamps[EVENT_ON_CONTEXT_INIT_START] = Date.now().toLong()
    }

    override fun onInitContextFinish() {
        eventTimestamps[EVENT_ON_CONTEXT_INIT_FINISH] = Date.now().toLong()
    }

    override fun onCreateInstanceStart() {
        eventTimestamps[EVENT_ON_CREATE_INSTANCE_START] = Date.now().toLong()
    }

    override fun onCreateInstanceFinish() {
        eventTimestamps[EVENT_ON_CREATE_INSTANCE_FINISH] = Date.now().toLong()
    }

    /**
     * Kotlin side callback
     */
    fun onPageCreateFinish(createTrace: PageCreateTrace?) {
        Log.log(MONITOR_NAME, "--onPageCreateFinish--")
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
        // pageCreateFinish is async callback, timing not guaranteed, try to notify
        tryNotifyListener()
    }

    override fun onFirstFramePaint() {
        if (eventTimestamps[EVENT_ON_FIRST_FRAME_PAINT] == 0L) {
            // First frame event may trigger multiple times, only record first occurrence
            eventTimestamps[EVENT_ON_FIRST_FRAME_PAINT] = Date.now().toLong()
        }
    }

    override fun onPause() {
        // onPause event as sentinel, if onPause occurs during launch, do not report
        eventTimestamps[EVENT_ON_PAUSE] = Date.now().toLong()
    }

    override fun onDestroy() {
        listeners.clear()
    }

    override fun getMonitorData(): KRLaunchData? {
        val isValid = checkIsValidTimestamps()
        if (!isValid) {
            return null
        }
        return KRLaunchData(eventTimestamps.copyOf())
    }

    /**
     * Check if timestamps are valid
     */
    private fun checkIsValidTimestamps(): Boolean {
        for (i in 1 until eventTimestamps.size - 1) {
            if (eventTimestamps[i] < eventTimestamps[i - 1]) {
                Log.log(
                    MONITOR_NAME,
                    "timestamp is invalid:[$i] ${eventTimestamps[i]} < [${i - 1}] ${eventTimestamps[i - 1]}"
                )
                return false
            }
        }
        return true
    }

    /**
     * Distribute launch data
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
     * Add launch data callback listener
     */
    fun addListener(listener: KRLaunchDataListener) {
        listeners.add(listener)
    }

    /**
     * Remove launch data callback listener
     */
    fun removeListener(listener: KRLaunchDataListener) {
        listeners.remove(listener)
    }

    companion object {
        const val MONITOR_NAME = "KRLaunchMonitor"
    }
}

typealias KRLaunchDataListener = (data: KRLaunchData) -> Unit
