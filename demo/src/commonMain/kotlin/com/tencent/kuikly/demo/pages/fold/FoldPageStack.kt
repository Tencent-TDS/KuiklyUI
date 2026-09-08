/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.demo.pages.fold

import com.tencent.kuikly.core.base.BackPressCallback
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.View

/**
 * 页面内容构建器。
 *
 * [FoldPageStackController] 提供 open / close 操作，[FoldPageStackEntry] 携带当前页面信息和业务数据。
 */
internal typealias FoldPageContent =
    ViewContainer<*, *>.(FoldPageStackController, FoldPageStackEntry) -> Unit

/**
 * 栈中的一个页面。
 *
 * [data] 只负责把轻量业务参数带给页面；真正需要跨页共享的状态仍建议放在 Pager 或 ViewModel 上。
 */
internal class FoldPageStackEntry internal constructor(
    val id: String,
    val title: String,
    val data: Any?,
    internal val content: FoldPageContent,
)

/**
 * 基于 PageList 的通用页内导航栈。
 *
 * 与 RouterModule 不同，它不会创建新的原生 Activity / ViewController，所有页面都属于同一个 Pager。
 * 页面入栈后保持挂载，因此适合设置、文件、邮件这类 3～4 级局部导航；不适合无限增长的信息流。
 */
internal class FoldPageStackController(
    private val pager: Pager,
    rootId: String,
    rootTitle: String,
    rootData: Any? = null,
    rootContent: FoldPageContent,
) {
    internal var entries: ObservableList<FoldPageStackEntry> by pager.observableList()
        private set

    var currentIndex by pager.observable(0)
        private set

    var viewportWidth by pager.observable(
        pager.pageData.pageViewWidth.coerceAtLeast(MIN_VIEWPORT_SIZE),
    )
        private set

    var viewportHeight by pager.observable(
        (pager.pageData.pageViewHeight - pager.pageData.statusBarHeight)
            .coerceAtLeast(MIN_VIEWPORT_SIZE),
    )
        private set

    /** 是否允许用户从左向右侧滑返回。 */
    var swipeBackEnabled by pager.observable(true)

    /** 改宽时暂时关闭 PageList 手势，完成归位后再打开。 */
    internal var pageListSwipeGate by pager.observable(true)
        private set

    /** 页面切换完成时回调，参数分别是当前页面和当前深度。根页面深度为 0。 */
    var onPageChanged: ((FoldPageStackEntry, Int) -> Unit)? = null

    private var pageListRef: ViewRef<PageListView<*, *>>? = null
    private var mutationVersion = 0
    private var resizingViewport = false
    private var resizeGeneration = 0

    private val backCallback = object : BackPressCallback() {
        override fun handleOnBackPressed() {
            close()
        }
    }

    init {
        require(rootId.isNotBlank()) { "FoldPageStack rootId must not be blank" }
        entries.add(FoldPageStackEntry(rootId, rootTitle, rootData, rootContent))
    }

    val depth: Int
        get() = currentIndex

    val canClose: Boolean
        get() = currentIndex > ROOT_INDEX

    fun currentEntry(): FoldPageStackEntry = entries[currentIndex.coerceIn(0, entries.lastIndex)]

    fun pathTitles(): String =
        entries.take(currentIndex + 1).joinToString(" / ") { it.title }

    /**
     * 打开一个新页面。
     *
     * 如果用户刚侧滑回旧页面但旧的前向节点还没来得及卸载，会先裁掉这些节点，再追加新页面。
     */
    fun open(
        id: String,
        title: String,
        data: Any? = null,
        animated: Boolean = true,
        content: FoldPageContent,
    ) {
        require(id.isNotBlank()) { "FoldPageStack page id must not be blank" }
        mutationVersion++
        pruneAfter(currentIndex)
        entries.add(FoldPageStackEntry(id, title, data, content))
        currentIndex = entries.lastIndex
        syncBackInterception()
        scrollWhenMounted(currentIndex, animated)
        notifyPageChanged()
    }

    /** 关闭顶部若干页。返回 false 表示已经在根页面。 */
    fun close(levels: Int = 1, animated: Boolean = true): Boolean {
        if (!canClose || levels <= 0) {
            return false
        }
        mutationVersion++
        val target = (currentIndex - levels).coerceAtLeast(ROOT_INDEX)
        currentIndex = target
        syncBackInterception()
        scrollWhenMounted(target, animated)
        schedulePruneAfter(target, if (animated) POP_FALLBACK_DELAY_MS else 0)
        notifyPageChanged()
        return true
    }

    /** 回到栈里最后一个匹配 [id] 的页面。 */
    fun closeTo(id: String, animated: Boolean = true): Boolean {
        var target = -1
        for (index in currentIndex downTo ROOT_INDEX) {
            if (entries[index].id == id) {
                target = index
                break
            }
        }
        if (target < 0 || target == currentIndex) {
            return false
        }
        return close(currentIndex - target, animated)
    }

    fun closeToRoot(animated: Boolean = true): Boolean = close(currentIndex, animated)

    /** 用新页面替换当前顶页，不增加栈深度。 */
    fun replace(
        id: String,
        title: String,
        data: Any? = null,
        content: FoldPageContent,
    ) {
        require(id.isNotBlank()) { "FoldPageStack page id must not be blank" }
        mutationVersion++
        entries[currentIndex] = FoldPageStackEntry(id, title, data, content)
        notifyPageChanged()
    }

    internal fun attach(ref: ViewRef<PageListView<*, *>>) {
        pageListRef = ref
    }

    internal fun updateViewport(width: Float, height: Float) {
        val nextWidth = width.coerceAtLeast(MIN_VIEWPORT_SIZE)
        val nextHeight = height.coerceAtLeast(MIN_VIEWPORT_SIZE)
        if (viewportWidth == nextWidth && viewportHeight == nextHeight) {
            return
        }
        resizingViewport = true
        pageListSwipeGate = false
        val generation = ++resizeGeneration
        viewportWidth = nextWidth
        viewportHeight = nextHeight
        // PageList 会先拿旧 offset 除以新页宽，期间可能上报一个错误页码。
        // 连续两次归位，并在最后一帧才重新接收页码和手势。
        pager.setTimeout(0) outer@{
            if (generation != resizeGeneration) {
                return@outer
            }
            pageListRef?.view?.scrollToPageIndex(currentIndex, false)
            pager.setTimeout(MOUNT_DELAY_MS) settle@{
                if (generation != resizeGeneration) {
                    return@settle
                }
                pageListRef?.view?.scrollToPageIndex(currentIndex, false)
                pager.setTimeout(0) {
                    if (generation == resizeGeneration) {
                        resizingViewport = false
                        pageListSwipeGate = true
                    }
                }
            }
        }
    }

    internal fun onNativePageChanged(index: Int) {
        if (entries.isEmpty() || resizingViewport) {
            return
        }
        val safeIndex = index.coerceIn(ROOT_INDEX, entries.lastIndex)
        currentIndex = safeIndex
        syncBackInterception()
        if (safeIndex < entries.lastIndex) {
            schedulePruneAfter(safeIndex, PRUNE_AFTER_SWIPE_DELAY_MS)
        }
        notifyPageChanged()
    }

    private fun notifyPageChanged() {
        onPageChanged?.invoke(currentEntry(), currentIndex)
    }

    private fun syncBackInterception() {
        val handler = pager.getBackPressHandler()
        val registered = handler.containsCallback(backCallback)
        if (canClose && !registered) {
            handler.addCallback(backCallback)
        } else if (!canClose && registered) {
            handler.removeCallback(backCallback)
        }
    }

    private fun scrollWhenMounted(
        index: Int,
        animated: Boolean,
        initialDelay: Int = MOUNT_DELAY_MS,
        attempt: Int = 0,
    ) {
        pager.setTimeout(initialDelay) {
            val didScroll = pageListRef?.view?.scrollToPageIndex(index, animated) == true
            if (!didScroll && attempt < MAX_SCROLL_RETRY) {
                scrollWhenMounted(
                    index = index,
                    animated = animated,
                    initialDelay = SCROLL_RETRY_DELAY_MS,
                    attempt = attempt + 1,
                )
            }
        }
    }

    private fun schedulePruneAfter(index: Int, delay: Int) {
        val version = mutationVersion
        pager.setTimeout(delay) {
            if (version == mutationVersion && currentIndex == index) {
                pruneAfter(index)
            }
        }
    }

    private fun pruneAfter(index: Int) {
        while (entries.lastIndex > index) {
            entries.removeAt(entries.lastIndex)
        }
    }

    private companion object {
        const val ROOT_INDEX = 0
        const val MIN_VIEWPORT_SIZE = 1f
        const val MOUNT_DELAY_MS = 50
        const val SCROLL_RETRY_DELAY_MS = 16
        const val MAX_SCROLL_RETRY = 4
        const val PRUNE_AFTER_SWIPE_DELAY_MS = 80
        const val POP_FALLBACK_DELAY_MS = 420
    }
}

/**
 * 渲染导航栈。父容器只需给它一个确定尺寸（通常是 `flex(1f)`）。
 *
 * 每个 vfor 节点只生成一个实体 View，避免动态增删页面时破坏 PageList 的物理索引。
 */
internal fun ViewContainer<*, *>.FoldPageStack(controller: FoldPageStackController) {
    View {
        attr {
            flex(1f)
            overflow(false)
        }
        event {
            layoutFrameDidChange { frame ->
                controller.updateViewport(frame.width, frame.height)
            }
        }
        PageList {
            ref {
                controller.attach(it)
            }
            attr {
                pageDirection(true)
                pageItemWidth(controller.viewportWidth)
                pageItemHeight(controller.viewportHeight)
                defaultPageIndex(controller.currentIndex)
                firstContentLoadMaxIndex(controller.entries.size.coerceAtLeast(1))
                keepItemAlive(true)
                bouncesEnable(false)
                showScrollerIndicator(false)
                overflow(false)
                backgroundColor(Color.TRANSPARENT)
                scrollEnable(
                    controller.pageListSwipeGate &&
                        controller.swipeBackEnabled &&
                        controller.currentIndex > 0,
                )
            }
            event {
                pageIndexDidChanged { params ->
                    controller.onNativePageChanged((params as JSONObject).optInt("index"))
                }
            }
            vfor({ controller.entries }) { entry ->
                View {
                    attr {
                        width(controller.viewportWidth)
                        height(controller.viewportHeight)
                        overflow(false)
                    }
                    entry.content(this, controller, entry)
                }
            }
        }
    }
}
