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

enum class PageEventKind(val value: Int) {
    NewPageStart(1),
    NewPageEnd(2),
    CreateStart(3),
    CreateEnd(4),
    BuildStart(5),
    BuildEnd(6),
    LayoutStart(7),
    LayoutEnd(8),
    CallModuleStart(9),
    CallModuleEnd(10),
    ModuleCallbackStart(11),
    ModuleCallbackEnd(12),
    ViewCallbackStart(13),
    ViewCallbackEnd(14),
    ViewCallMethodStart(15),
    ViewCallMethodEnd(16),
    FireObserverFnStart(17),
    FireObserverFnEnd(18),
    ViewWillInit(19),
    ViewDidInit(20);

    fun isStartEvent(): Boolean = name.endsWith("Start") || this == ViewWillInit
    fun isEndEvent(): Boolean = name.endsWith("End") || this == ViewDidInit
}

open class PageEvent(val kind: PageEventKind) {
    val timestamp: Long = DateTime.currentTimestamp()

    protected open fun formatExtra(): String = ""

    override fun toString(): String = "timestamp:$timestamp ${kind.name}${formatExtra()}"
}

class BuildEndEvent(val numNodes: Int) : PageEvent(PageEventKind.BuildEnd) {
    override fun formatExtra(): String = " numNodes:$numNodes"
}

class LayoutEndEvent(val numNodes: Int) : PageEvent(PageEventKind.LayoutEnd) {
    override fun formatExtra(): String = " numNodes:$numNodes"
}

open class CallModuleEvent(
    kind: PageEventKind,
    val moduleName: String,
    val method: String,
    val sync: Boolean,
    val callbackRef: Int
) : PageEvent(kind) {
    override fun formatExtra(): String = " moduleName:$moduleName method:$method sync:$sync callbackRef:$callbackRef"
}

class CallModuleStartEvent(moduleName: String, method: String, sync: Boolean, callbackRef: Int)
    : CallModuleEvent(PageEventKind.CallModuleStart, moduleName, method, sync, callbackRef)

class CallModuleEndEvent(moduleName: String, method: String, sync: Boolean, callbackRef: Int)
    : CallModuleEvent(PageEventKind.CallModuleEnd, moduleName, method, sync, callbackRef)

class ModuleCallbackStartEvent(moduleName: String, method: String, sync: Boolean, callbackRef: Int)
    : CallModuleEvent(PageEventKind.ModuleCallbackStart, moduleName, method, sync, callbackRef)

class ModuleCallbackEndEvent(moduleName: String, method: String, sync: Boolean, callbackRef: Int)
    : CallModuleEvent(PageEventKind.ModuleCallbackEnd, moduleName, method, sync, callbackRef)

open class ViewCallbackEvent(
    kind: PageEventKind,
    val viewName: String,
    val viewRef: Int,
    val methodName: String,
    val callbackRef: Int
) : PageEvent(kind) {
    override fun formatExtra(): String = " viewName:$viewName viewRef:$viewRef methodName:$methodName callbackRef:$callbackRef"
}

class ViewCallbackStartEvent(viewName: String, viewRef: Int, methodName: String, callbackRef: Int)
    : ViewCallbackEvent(PageEventKind.ViewCallbackStart, viewName, viewRef, methodName, callbackRef)

class ViewCallbackEndEvent(viewName: String, viewRef: Int, methodName: String, callbackRef: Int)
    : ViewCallbackEvent(PageEventKind.ViewCallbackEnd, viewName, viewRef, methodName, callbackRef)

class ViewCallMethodStartEvent(viewName: String, viewRef: Int, methodName: String, callbackRef: Int)
    : ViewCallbackEvent(PageEventKind.ViewCallMethodStart, viewName, viewRef, methodName, callbackRef)

class ViewCallMethodEndEvent(viewName: String, viewRef: Int, methodName: String, callbackRef: Int)
    : ViewCallbackEvent(PageEventKind.ViewCallMethodEnd, viewName, viewRef, methodName, callbackRef)

open class FireObserverEvent(
    kind: PageEventKind,
    val propertyKey: String,
    val observerCount: Int
) : PageEvent(kind) {
    override fun formatExtra(): String = " propertyKey:$propertyKey observerCount:$observerCount"
}

class FireObserverFnStartEvent(propertyKey: String, observerCount: Int)
    : FireObserverEvent(PageEventKind.FireObserverFnStart, propertyKey, observerCount)

class FireObserverFnEndEvent(propertyKey: String, observerCount: Int)
    : FireObserverEvent(PageEventKind.FireObserverFnEnd, propertyKey, observerCount)

open class ViewInitEvent(
    kind: PageEventKind,
    val viewName: String,
    val viewClassName: String?,
    val ref: Int
) : PageEvent(kind) {
    override fun formatExtra(): String = " viewName:$viewName viewClassName:$viewClassName ref:$ref"
}

class ViewWillInitEvent(viewName: String, viewClassName: String?, ref: Int)
    : ViewInitEvent(PageEventKind.ViewWillInit, viewName, viewClassName, ref)

class ViewDidInitEvent(viewName: String, viewClassName: String?, ref: Int)
    : ViewInitEvent(PageEventKind.ViewDidInit, viewName, viewClassName, ref)

class PageEventTrace {
    companion object {
        private const val REPORT_BEGIN = "--- begin of kuikly page event report ---"
        private const val REPORT_END = "--- end of kuikly page event report ---"
    }

    internal var pageName: String = ""
    internal var pageId: String = ""

    private val events = mutableListOf<PageEvent>()

    private fun addEvent(event: PageEvent) {
        events.add(event)
    }

    fun onCreateStart() {
        addEvent(PageEvent(PageEventKind.CreateStart))
    }

    fun onNewPageStart() {
        addEvent(PageEvent(PageEventKind.NewPageStart))
    }

    fun onNewPageEnd() {
        addEvent(PageEvent(PageEventKind.NewPageEnd))
    }

    fun onBuildStart() {
        addEvent(PageEvent(PageEventKind.BuildStart))
    }

    fun onBuildEnd(nodeCount: Int) {
        addEvent(BuildEndEvent(nodeCount))
    }

    fun onCreateEnd() {
        addEvent(PageEvent(PageEventKind.CreateEnd))
    }

    fun onLayoutStart() {
        addEvent(PageEvent(PageEventKind.LayoutStart))
    }

    fun onLayoutEnd(nodeCount: Int) {
        addEvent(LayoutEndEvent(nodeCount))
    }

    fun onCallModuleStart(moduleName: String, method: String, sync: Boolean, callbackRef: Int) {
        addEvent(CallModuleStartEvent(moduleName, method, sync, callbackRef))
    }

    fun onCallModuleEnd(moduleName: String, method: String, sync: Boolean, callbackRef: Int) {
        addEvent(CallModuleEndEvent(moduleName, method, sync, callbackRef))
    }

    fun onModuleCallbackStart(moduleName: String, method: String, sync: Boolean, callbackRef: Int) {
        addEvent(ModuleCallbackStartEvent(moduleName, method, sync, callbackRef))
    }

    fun onModuleCallbackEnd(moduleName: String, method: String, sync: Boolean, callbackRef: Int) {
        addEvent(ModuleCallbackEndEvent(moduleName, method, sync, callbackRef))
    }

    fun onViewCallbackStart(viewName: String, viewRef: Int, methodName: String, callbackRef: Int) {
        addEvent(ViewCallbackStartEvent(viewName, viewRef, methodName, callbackRef))
    }

    fun onViewCallbackEnd(viewName: String, viewRef: Int, methodName: String, callbackRef: Int) {
        addEvent(ViewCallbackEndEvent(viewName, viewRef, methodName, callbackRef))
    }

    fun onViewCallMethodStart(viewName: String, viewRef: Int, methodName: String, callbackRef: Int) {
        addEvent(ViewCallMethodStartEvent(viewName, viewRef, methodName, callbackRef))
    }

    fun onViewCallMethodEnd(viewName: String, viewRef: Int, methodName: String, callbackRef: Int) {
        addEvent(ViewCallMethodEndEvent(viewName, viewRef, methodName, callbackRef))
    }

    fun onFireObserverFnStart(propertyKey: String, observerCount: Int) {
        addEvent(FireObserverFnStartEvent(propertyKey, observerCount))
    }

    fun onFireObserverFnEnd(propertyKey: String, observerCount: Int) {
        addEvent(FireObserverFnEndEvent(propertyKey, observerCount))
    }

    fun onViewWillInit(viewName: String, viewClassName: String?, ref: Int) {
        addEvent(ViewWillInitEvent(viewName, viewClassName, ref))
    }

    fun onViewDidInit(viewName: String, viewClassName: String?, ref: Int) {
        addEvent(ViewDidInitEvent(viewName, viewClassName, ref))
    }

    private fun formatEventLine(event: PageEvent, indentStr: String): String =
        indentStr + event.toString()

    /**
     * 将缓存的事件格式化为字符串输出。
     * 开头统一输出 pageName、pageId，并用 begin/end of report 标记报告范围。
     * @param pretty 为 true 时按嵌套关系缩进输出，为 false 时紧凑输出
     */
    fun dump(pretty: Boolean): String {
        val header = "pageName:$pageName pageId:$pageId"
        val body = if (events.isEmpty()) {
            "(no events)"
        } else if (!pretty) {
            events.joinToString("\n") { formatEventLine(it, "") }
        } else {
            var indent = 0
            val indentUnit = "    "
            buildString {
                for (event in events) {
                    val kind = event.kind
                    if (kind.isEndEvent()) indent = (indent - 1).coerceAtLeast(0)
                    append(formatEventLine(event, indentUnit.repeat(indent))).append('\n')
                    if (kind.isStartEvent()) indent++
                }
            }.trimEnd()
        }
        return buildString {
            append(REPORT_BEGIN).append('\n')
            append(header).append('\n')
            append(body).append('\n')
            append(REPORT_END).append("\n\n")
        }
    }
}

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

    internal var pageId = ""
    internal var pageName = ""
    var pageEventTrace: PageEventTrace? = null

    internal fun createPageEventTraceIfNeeded(){
        if (pageEventTrace == null){
            pageEventTrace = PageEventTrace()
            pageEventTrace?.pageId = pageId
            pageEventTrace?.pageName = pageName
        }
    }

    fun onCreateStart() {
        createStartTimeMills = DateTime.currentTimestamp()
        pageEventTrace?.onCreateStart()
    }

    fun onNewPageStart() {
        newPageStartTimeMills = DateTime.currentTimestamp()
        pageEventTrace?.onNewPageStart()
    }

    fun onNewPageEnd() {
        newPageEndTimeMills = DateTime.currentTimestamp()
        pageEventTrace?.onNewPageEnd()
    }

    fun onBuildStart() {
        buildStartTimeMills = DateTime.currentTimestamp()
        pageEventTrace?.onBuildStart()
    }

    fun onBuildEnd(nodeCount: Int) {
        buildEndTimeMills = DateTime.currentTimestamp()
        pageEventTrace?.onBuildEnd(nodeCount)
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
        pageEventTrace?.onCreateEnd()
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