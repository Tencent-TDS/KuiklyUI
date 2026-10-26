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
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.pager.PageData
import com.tencent.kuikly.core.pager.HingeStatus
import com.tencent.kuikly.core.pager.ReservedRegion
import com.tencent.kuikly.core.pager.ReservedRegionKind
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import kotlin.math.abs
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.ios.UIBarAxis
import com.tencent.kuikly.core.views.ios.UIBarButtonItemPlacement
import com.tencent.kuikly.core.views.ios.UINavigationBar
import com.tencent.kuikly.core.views.ios.UIToolbar
import com.tencent.kuikly.core.views.ios.UIToolbarItemKind
import com.tencent.kuikly.core.views.ios.toolbarItem
import kotlin.math.max

/** 窗口内容构建器，只拿到控制器，视觉与交互全部由适用方决定。 */
internal typealias FoldSplitWindowContent =
    ViewContainer<*, *>.(FoldSplitStackController) -> Unit

/** 二级及以上页面的内容构建器。 */
internal typealias FoldSplitPageContent =
    ViewContainer<*, *>.(FoldSplitStackController, FoldSplitStackPage) -> Unit

/**
 * 压在一级详情之上的一个页面。
 *
 * [data] 只用来把轻量参数带给页面；跨页共享状态仍建议挂在 Pager 上。
 */
internal class FoldSplitStackPage internal constructor(
    val id: String,
    val title: String,
    val data: Any?,
    internal val content: FoldSplitPageContent,
)

/**
 * 窗口尺寸参数。
 *
 * 这里只描述 frame：主窗宽度区间、浮层边距、详情最小可读宽度。
 * 背景、圆角、玻璃、阴影、蒙版等视觉全部不在组件职责内。
 */
internal class FoldSplitStackLayout(
    /** 主窗初始宽度。 */
    val masterWidth: Float = 248f,
    val masterMinWidth: Float = 196f,
    val masterMaxWidth: Float = 340f,
    /** 主窗浮层到舞台边缘的距离。 */
    val masterInset: Float = 10f,
    /** 详情可读区最低宽度，和 [masterMinWidth] 一起决定大小屏断点。 */
    val detailMinWidth: Float = 430f,
    /** 是否允许拖主窗右边缘改宽。 */
    val masterResizable: Boolean = true,
    /** 详情内容相对主窗右边缘再让开的距离，用于算画廊后退量。 */
    val contentPad: Float = 26f,
    /** 主窗内首列文字的左边缘，被覆盖的一级详情要收到这个位置。 */
    val masterTextLeft: Float = masterInset + 56f,
)

/**
 * 系统导航栏配置。传给 [FoldSplitStackController] 后由 [FoldSplitStack] 自己挂栏和返回键；
 * 返回键何时显示由栈状态决定，适用方不用判断 compact / 压栈。
 *
 * 非 iOS 会忽略这一份配置，页面仍可用 [FoldSplitStackController.usesDrawnBack] 画自己的返回。
 */
internal class FoldSplitSystemChrome(
    val tintColor: Color = Color.BLACK,
    val backSymbol: String = "chevron.backward",
    val backTitle: String = "",
    val backAxis: UIBarAxis = UIBarAxis.Automatic,
    val items: List<FoldSplitSystemNavItem> = emptyList(),
    /** 底部系统工具栏。不传则不显示。 */
    val toolbar: FoldSplitSystemToolbar? = null,
)

/**
 * 底部系统工具栏。iOS 一律走系统 UIToolbar。
 * 分栏不是 UISplitViewController，栏默认会铺满整屏、盖住左边列表。
 * [leadingInset] 传左列表右边缘，[trailingInset] 传右侧留白。
 * 栏的可用区域从左列表右边开始，1 颗还是多颗按钮都排在这块区域里。
 */
internal class FoldSplitSystemToolbar(
    val items: List<FoldSplitSystemToolbarItem> = emptyList(),
    val leadingInset: (FoldSplitStackController) -> Float = { 0f },
    val trailingInset: (FoldSplitStackController) -> Float = { 0f },
) {
    /** 配置里是否声明为底栏横排（无 VerticalPreferred 的 action item）。 */
    fun layoutsHorizontally(): Boolean =
        items.none { it.kind == "item" && it.axis == UIBarAxis.VerticalPreferred }

    /** 实际排版：iPad 分栏始终走底栏横排 + inset/shift；折叠机用配置里的 axis。 */
    fun layoutsHorizontallyOn(controller: FoldSplitStackController): Boolean {
        if (controller.isIPadLikeStage()) {
            return true
        }
        return layoutsHorizontally()
    }
}

/** 工具栏上一颗按钮，或一段 flexibleSpace / fixedSpace。 */
internal class FoldSplitSystemToolbarItem(
    val symbol: String = "",
    val title: String = "",
    val index: Int = 0,
    val kind: String = "item",
    val axis: UIBarAxis = UIBarAxis.Automatic,
    val onClick: (() -> Unit)? = null,
)

/** 系统导航栏左上 / 右上的额外按钮，对应 [SystemNavItem]。 */
internal class FoldSplitSystemNavItem(
    val placement: String,
    val symbol: String,
    val title: String = "",
    val index: Int = 0,
    val itemGroup: Int = 0,
    val axis: UIBarAxis = UIBarAxis.Automatic,
    val onClick: (() -> Unit)? = null,
)

/**
 * 「主窗 + 详情栈」自适应导航控制器。
 *
 * 舞台上 master / 一级详情都只挂一份，靠 zIndex 和位移换层，不走 vif 双挂载：
 * 1. master：小屏在 PageList 下（靠 0 号透明页透出），大屏抬到 PageList 上成为浮窗
 * 2. 一级详情：大屏始终钉在 PageList 下做画廊。小屏没有压栈页时挂进 1 号槽（对齐原版
 *    settings：整页滑回 0 号透出 master）；被更深页盖住时再落到 PageList 下面，避免并排接缝
 * 3. PageList：0 号透明占位；1 号小屏挂一级详情、大屏保持透明；2 号起只有**当前顶页**挂内容。
 *    被盖住的压栈页钉在 PageList 下面，滑动时底页留在原位、顶页盖上去
 *
 * 这接近 iOS：`UISplitViewController` 的 primary/secondary 是两列容器，compact 时把同一个
 * VC 收到一条 `UINavigationController` 里，而不是复制一份 view。Interactive pop 是边缘手势，
 * 不是全屏 PageViewController。
 */
internal class FoldSplitStackController(
    private val pager: Pager,
    val layout: FoldSplitStackLayout = FoldSplitStackLayout(),
    /** 非空则由 [FoldSplitStack] 在 iOS 上挂系统导航栏和返回键。 */
    val chrome: FoldSplitSystemChrome? = null,
    /**
     * master 窗口内容。只创建一次：小屏铺满整屏并压在 PageList 下，大屏变成左侧浮窗抬到 PageList 上。
     *
     * 自身尺寸从 [masterContentWidth] / [masterContentHeight] 拿。视觉要分叉时再按 [isCompact]
     * 判断——注意业务侧如果自己用 vif 分叉，那一层仍会重建。
     */
    internal val masterContent: FoldSplitWindowContent,
    /** 一级详情：只创建一次。小屏是导航栈里的详情页，大屏钉在 PageList 下面做画廊。 */
    internal val primaryContent: FoldSplitWindowContent,
) {
    /** 二级及以上页面。1 号槽位始终存在，所以物理页码 = [PAGE_PRIMARY] + 下标 + 1。 */
    internal var pages: ObservableList<FoldSplitStackPage> by pager.observableList()
        private set

    /** 主窗请求宽度，实际生效值见 [masterWindowWidth]。 */
    var requestedMasterWidth by pager.observable(layout.masterWidth)
        private set

    /** PageList 当前物理页码：0 占位窗，1 一级详情，2 及以上是压上来的页面。 */
    var pageIndex by pager.observable(PAGE_PRIMARY)
        private set

    /** 一级详情的水平位移。0 表示让在主窗右侧，负值表示被覆盖后收向主窗文字列。 */
    var primaryShift by pager.observable(0f)
        private set

    /** 覆盖进度派生的蒙版强度，只在滑动中大于 0，两端都是 0。 */
    var coverShadow by pager.observable(0f)
        private set

    /** 小屏是否停在 master 根列表。大屏不看这个字段。 */
    var compactOnMaster by pager.observable(true)
        private set

    /** 大屏手动收起主窗。不影响 [isCompact]：收起不等于切到小屏导航。 */
    var masterCollapsed by pager.observable(false)
        private set

    /** 舞台正在改宽。这期间 PageList 上报的页码不可信，不要当成返回或关页。 */
    var resizingStage by pager.observable(false)
        private set

    /** 适用方可临时关掉左右滑返回，例如打开全屏预览时。 */
    var swipeBackEnabled by pager.observable(true)

    /**
     * 系统导航栏 inset 依赖 [contentInsetLeft] 等布局量；attr 里读此字段以在改宽/分栏后刷新 Native leadingInset。
     */
    internal var systemChromeLayoutKey by pager.observable(0)
        private set

    internal var navBarTrailingInset by pager.observable(0f)
        private set

    internal var pageListSwipeGate by pager.observable(true)
        private set

    private var measuredWidth by pager.observable(0f)
    private var measuredHeight by pager.observable(0f)

    /** PageList 连续页位置，1.35 表示从 1 号页滑向 2 号页 35%。 */
    private var scrollPosition by pager.observable(0f)

    private var pageListRef: ViewRef<PageListView<*, *>>? = null
    private var mutationVersion = 0
    private var resizeGeneration = 0
    private var keepPageIndexOnResize = PAGE_PRIMARY
    private var hasStageSize = false
    private var lastWasCompact = false
    private var breakpointReady = false

    /** 在一级详情或更深处落稳过之后，滑到 0 号页才算返回，避免首帧误触。 */
    private var placeholderBackArmed = false

    private var masterDragStartWidth = 0f
    private var masterDragStartX = 0f
    private val inPageBack = object : BackPressCallback() {
        override fun handleOnBackPressed() {
            handleBack()
        }
    }

    // region 尺寸

    private fun windowWidth(): Float =
        if (pager.pageData.pageViewWidth > 0f) {
            pager.pageData.pageViewWidth
        } else {
            pager.pageData.deviceWidth
        }

    fun contentWidth(): Float = if (measuredWidth > 0f) measuredWidth else windowWidth()

    /** 由「主窗最低可读宽度 + 详情最低宽度」推导，不按设备型号判断。左右折缝才强制双栏。 */
    fun isCompact(): Boolean {
        if (isSideBySideHinge()) {
            return false
        }
        return contentWidth() < layout.masterMinWidth + layout.detailMinWidth
    }

    /** iPhone Duo 半开。折缝可能是左右，也可能是上下。 */
    fun isHalfFolded(): Boolean = pager.pageData.hingeStatus == HingeStatus.PARTIALLY_OPEN

    /**
     * 半开且折缝把舞台切成左右两块。
     * 竖屏半折的折缝是横条，左右宽度都是 0，不能按左右双栏把内容推出去。
     */
    fun isSideBySideHinge(): Boolean =
        pager.pageData.hingeStatus == HingeStatus.PARTIALLY_OPEN && isSplitAcrossHinge()

    /**
     * 折缝把舞台分成左右两块：存在 active 的竖向 division region，
     * 且折缝右侧还留有内容空间。
     */
    private fun activeVerticalDivision(): ReservedRegion? {
        val width = pager.pageData.pageViewWidth
        return pager.pageData.reservedRegions.lastOrNull { region ->
            region.active && region.kind == ReservedRegionKind.DIVISION &&
                region.height >= region.width
        }?.takeIf { width > 1f && it.x + it.width < width - 0.5f }
    }

    fun isSplitAcrossHinge(): Boolean =
        pager.pageData.hingeStatus == HingeStatus.PARTIALLY_OPEN &&
            activeVerticalDivision() != null

    /** 左右半开时左栏宽度，取系统报的折缝左缘；没有区域数据时左右均分。 */
    fun splitPaneWidth(): Float {
        val division = activeVerticalDivision()
        if (division != null && division.x > 1f) {
            return division.x
        }
        return (contentWidth() / 2f).coerceAtLeast(1f)
    }

    /** 主窗浮层边距。左右半开贴折缝，不再留浮窗空隙。只用于上下，左右用 [masterFrameLeft]。 */
    fun masterChromeInset(): Float =
        if (isSideBySideHinge()) 0f else layout.masterInset

    /**
     * Duo 侧边状态栏等系统遮挡不再混进 safeAreaInsets，
     * 这里从 reservedRegions 的贴边遮挡里取避让宽度。
     * 竖屏时按钮簇在角落，不占整条侧边列，不避让。
     */
    fun leadingSafeInset(): Float {
        if (!isLandscapeStage()) {
            return pager.pageData.safeAreaInsets.left
        }
        var value = pager.pageData.safeAreaInsets.left
        pager.pageData.reservedRegions.forEach { region ->
            if (region.active && region.kind == ReservedRegionKind.OCCLUSION &&
                region.height >= region.width && region.x <= 0.5f
            ) {
                value = max(value, region.x + region.width)
            }
        }
        return value
    }

    fun trailingSafeInset(): Float {
        if (!isLandscapeStage()) {
            return pager.pageData.safeAreaInsets.right
        }
        return max(pager.pageData.safeAreaInsets.right, trailingOcclusionInset())
    }

    /**
     * 贴右的 occlusion 宽度（竖屏状态列 / 横屏竖轨），与 [trailingSafeInset] 横屏逻辑一致。
     * 详情等内容在 Demo 里按需取用；Stack 不统一垫距。
     */
    fun trailingOcclusionInset(): Float {
        var value = 0f
        val width = pager.pageData.pageViewWidth
        pager.pageData.reservedRegions.forEach { region ->
            val rightEdge = region.x + region.width
            if (region.active && region.kind == ReservedRegionKind.OCCLUSION &&
                region.height >= region.width && width > 1f && rightEdge >= width - 0.5f
            ) {
                value = max(value, width - region.x)
            }
        }
        return value
    }

    /**
     * 主窗左边缘。
     * 单独全屏时由内容自己垫 safe area；并排时侧栏贴在左半屏，要让开左侧竖栏。
     */
    fun masterFrameLeft(): Float {
        val safe = leadingSafeInset()
        return if (isSideBySideHinge()) {
            safe
        } else {
            max(layout.masterInset, safe)
        }
    }

    /** 主窗是否正在作为浮层画出来。大屏被收起时是 false。 */
    fun showsMaster(): Boolean = !isCompact() && !masterCollapsed

    fun pageWidth(): Float = contentWidth()

    fun pageHeight(): Float =
        if (measuredHeight > 0f) {
            measuredHeight
        } else {
            (pager.pageData.pageViewHeight - pager.pageData.statusBarHeight).coerceAtLeast(1f)
        }

    /** 大屏浮窗的宽度设置值，受 [FoldSplitStackLayout] 的区间约束。半开时左右等宽。 */
    fun masterWindowWidth(): Float =
        if (isSideBySideHinge()) {
            (splitPaneWidth() - leadingSafeInset()).coerceAtLeast(1f)
        } else {
            requestedMasterWidth.coerceIn(layout.masterMinWidth, layout.masterMaxWidth)
        }

    /**
     * master 内容拿到的实际宽度：小屏铺满舞台，大屏是浮窗宽度。
     *
     * [masterContent] 按这个值做自适应即可，不必知道自己被挂在 PageList 的上面还是下面。
     */
    fun masterContentWidth(): Float =
        if (showsMaster()) masterWindowWidth() else contentWidth()

    /** master 内容拿到的实际高度：大屏要扣掉浮层上下两条边距。 */
    fun masterContentHeight(): Float =
        if (showsMaster()) {
            (pageHeight() - masterChromeInset() * 2f).coerceAtLeast(1f)
        } else {
            pageHeight()
        }

    /** 详情窗口没被主窗遮住的宽度。页面仍是整屏，可读内容的右侧留白由适用方自己加。 */
    fun detailContentWidth(): Float =
        (pageWidth() - contentInsetLeft()).coerceAtLeast(1f)

    /**
     * 页面需要让开的左侧宽度。
     *
     * 页面本身铺满整屏，靠这条内边距把可读内容推到主窗右边，而不是把页面裁窄。
     */
    fun contentInsetLeft(): Float {
        if (!showsMaster()) {
            return 0f
        }
        if (isSideBySideHinge()) {
            val division = activeVerticalDivision()
            if (division != null) {
                return division.x + division.width
            }
        }
        return masterFrameLeft() + masterWindowWidth()
    }

    /** 主窗宽度改到指定值，用于开关、菜单等程序化调整。 */
    fun resizeMaster(width: Float) {
        requestedMasterWidth = width.coerceIn(layout.masterMinWidth, layout.masterMaxWidth)
        refreshGalleryForCurrentPage()
        bumpSystemChromeLayout()
    }

    /** 大屏收起 / 展开主窗。小屏没有浮层主窗，调用无效。 */
    fun collapseMaster() {
        if (isCompact()) {
            return
        }
        masterCollapsed = true
        refreshGalleryForCurrentPage()
        bumpSystemChromeLayout()
    }

    fun expandMaster() {
        masterCollapsed = false
        refreshGalleryForCurrentPage()
        bumpSystemChromeLayout()
    }

    fun toggleMasterCollapsed() {
        if (masterCollapsed) {
            expandMaster()
        } else {
            collapseMaster()
        }
    }

    /**
     * 立刻卸掉所有压栈页并停在一级详情。侧栏换入口时用这个，避免 [open] 叠在旧栈上面。
     */
    fun resetToPrimary() {
        mutationVersion++
        pruneTo(PAGE_PRIMARY)
        resetGallery()
        if (isCompact()) {
            compactOnMaster = false
        }
        pageIndex = PAGE_PRIMARY
        scrollTo(PAGE_PRIMARY, animated = false)
        applyScroll(PAGE_PRIMARY * pageWidth(), pageWidth())
        syncBackInterception()
        bumpSystemChromeLayout()
    }

    internal fun dragMasterWidth(pageX: Float, isStart: Boolean) {
        if (!layout.masterResizable) {
            return
        }
        if (isStart) {
            masterDragStartWidth = requestedMasterWidth
            masterDragStartX = pageX
            return
        }
        resizeMaster(masterDragStartWidth + pageX - masterDragStartX)
    }

    // endregion

    // region 页面状态

    val depth: Int
        get() = pages.size

    fun topPageIndex(): Int = PAGE_PRIMARY + pages.size

    fun currentPage(): FoldSplitStackPage? = pages.getOrNull(pageIndex - PAGE_PRIMARY - 1)

    fun pathTitles(): String =
        pages.take((pageIndex - PAGE_PRIMARY).coerceAtLeast(0)).joinToString(" / ") { it.title }

    /** 是否已经有压在一级详情之上的页面，用于返回键与标题栏判断。 */
    fun hasPushedPage(): Boolean = pages.isNotEmpty() || pageIndex > PAGE_PRIMARY

    /** 栈还能往回退：小屏不在根列表，或已经压了更深页。 */
    fun shouldShowBack(): Boolean =
        hasPushedPage() || (isCompact() && !compactOnMaster)

    /** 配置了 [chrome] 且当前是 iOS，由 stack 接管系统栏和返回键。 */
    fun usesSystemChrome(): Boolean = chrome != null && pager.pageData.isIOS

    /**
     * 配置了系统底栏时，列表建议的 bottom 留白（Home + 工具栏高度）。
     */
    fun systemToolbarContentInsetBottom(): Float {
        if (!usesSystemChrome() || chrome?.toolbar == null) {
            return 0f
        }
        return pager.pageData.safeAreaInsets.bottom + FOLD_SYSTEM_TOOLBAR_HEIGHT + 12f
    }

    /** 非系统栏时，适用方画页内返回。 */
    fun usesDrawnBack(): Boolean = !usesSystemChrome() && shouldShowBack()

    /**
     * 普通 iPhone 上系统按钮浮在透明导航栏里，会盖住页内大标题。
     * 有左侧按钮（返回或 leading item）时，标题要往右让开。
     */
    fun compactChromeLeadingInset(): Float {
        if (!usesSystemChrome() || !isCompact()) {
            return 0f
        }
        var slots = 0
        if (shouldShowBack()) {
            slots += 1
        }
        slots += chrome?.items?.count { it.placement != "trailing" } ?: 0
        if (slots <= 0) {
            return 0f
        }
        return slots * COMPACT_SYSTEM_ITEM + 8f
    }

    /**
     * 可选：个别 Demo 若要让标题别贴到竖轨，可按 trailing 颗数粗估。
     * [FoldSplitStack] 本身不会给舞台/内容加避让；系统栏浮层由 Native 绘制。
     */
    fun compactChromeTrailingInset(): Float {
        if (!usesSystemChrome() || !isCompact()) {
            return 0f
        }
        val slots = chrome?.items?.count { it.placement == "trailing" } ?: 0
        if (slots <= 0) {
            return 0f
        }
        return slots * COMPACT_SYSTEM_ITEM + 8f
    }

    /** 舞台宽大于高。用布局尺寸，没有量到时退回页面宽高，不扣状态栏。 */
    fun isLandscapeStage(): Boolean {
        val width = if (measuredWidth > 0f) measuredWidth else pager.pageData.pageViewWidth
        val height = if (measuredHeight > 0f) measuredHeight else pager.pageData.pageViewHeight
        if (width <= 0f || height <= 0f) {
            return false
        }
        return width > height
    }

    /** 大屏且无铰链（iPad 双栏）。 */
    internal fun isIPadLikeStage(): Boolean =
        !isCompact() && pager.pageData.hingeStatus == HingeStatus.UNKNOWN

    /**
     * 导航栏 / 返回键是否让开 master（备份规则）。
     * iPad 双栏始终让开；Duo 横屏走竖轨不让开；Duo 竖屏展开/半开让开。
     */
    fun isolatesChromeFromMaster(): Boolean {
        if (!showsMaster()) {
            return false
        }
        if (isSideBySideHinge()) {
            return true
        }
        if (isIPadLikeStage()) {
            return true
        }
        if (isLandscapeStage()) {
            return false
        }
        val status = pager.pageData.hingeStatus
        return status == HingeStatus.FULLY_OPEN || status == HingeStatus.PARTIALLY_OPEN
    }

    /** 大屏工具栏按钮要让开的左侧宽度。不该让开时为 0。 */
    fun toolbarLeadingOffset(): Float {
        if (!isolatesChromeFromMaster()) {
            return 0f
        }
        val offset = chrome?.toolbar?.leadingInset(this) ?: contentInsetLeft()
        return offset.coerceAtLeast(0f)
    }

    /** 左右屏同时显示且需要让开左边时，右侧留白。否则为 0。 */
    fun toolbarTrailingOffset(): Float {
        if (!isolatesChromeFromMaster()) {
            return 0f
        }
        val toolbar = chrome?.toolbar ?: return 0f
        return toolbar.trailingInset(this).coerceAtLeast(0f)
    }

    /**
     * 返回留在 leading，axis 不改写，有竖轨由系统自己放过去。
     * iPad 分栏时返回仍在横栏上，按主栏右缘垫 leadingInset。
     */
    fun chromeBackLeadingInset(): Float {
        if (!shouldShowBack() || !isolatesChromeFromMaster()) {
            return 0f
        }
        return toolbarLeadingOffset()
    }

    fun navigationBarTrailingInset(): Float {
        if (!usesSystemChrome() || !isolatesChromeFromMaster()) {
            return 0f
        }
        return toolbarTrailingOffset()
    }

    private fun bumpSystemChromeLayout() {
        navBarTrailingInset = navigationBarTrailingInset()
        systemChromeLayoutKey++
    }

    internal fun bumpSystemChromeLayoutPublic() {
        bumpSystemChromeLayout()
    }

    fun handleBack() {
        // 从二级退回后立刻再进三级时，PageList 的页码回调可能还停在一级。
        // 大屏这时 close() 会直接返回，系统返回看起来没反应。按栈顶关一层。
        if (pages.isNotEmpty() && pageIndex <= PAGE_PRIMARY) {
            pageIndex = topPageIndex()
        }
        if (hasPushedPage()) {
            close()
        } else if (isCompact() && !compactOnMaster) {
            backToMaster()
        }
    }

    /** 是否真正停在压上来的页面槽位，用于手势与触摸判断。 */
    fun isOnPushedSlot(): Boolean {
        val index = if (resizingStage) keepPageIndexOnResize else pageIndex
        return pages.isNotEmpty() && index > PAGE_PRIMARY
    }

    /**
     * 小屏：停在详情或更深页时都可以左右滑（对齐原版 settings：1 号详情滑回 0 号根列表）。
     * 大屏只在压上来的页面开左右滑，避免挡住底下钉住的一级详情。
     */
    fun canSwipePageList(): Boolean {
        if (!pageListSwipeGate || !swipeBackEnabled) {
            return false
        }
        if (isCompact()) {
            return !compactOnMaster
        }
        return isOnPushedSlot()
    }

    fun pageListHandlesTouch(): Boolean {
        if (isCompact() && !compactOnMaster) {
            return true
        }
        return isOnPushedSlot()
    }

    /**
     * 小屏且没有压栈页时，一级详情挂在 PageList 1 号槽里，才能整页滑回 master。
     * 有更深页时改钉在外面，避免和顶页并排露出接缝。
     */
    fun showsPrimaryInPageList(): Boolean = isCompact() && pages.isEmpty()

    fun showsPrimaryHost(): Boolean = !showsPrimaryInPageList()

    /**
     * 指定物理页码被下一页覆盖的进度，供适用方自定义转场。
     */
    fun coverProgress(physicalIndex: Int): Float =
        (scrollPosition - physicalIndex).coerceIn(0f, 1f)

    /** 是否是压栈栈顶那一页。只有这一页挂在 PageList 里跟着滑，其余钉在下面。 */
    fun isTopPushedPage(page: FoldSplitStackPage): Boolean =
        pages.lastOrNull()?.id == page.id

    /**
     * 单独看一级详情时内容让在主窗右边；被覆盖时收到主窗文字列。
     * 两者之差就是画廊后退量。
     */
    fun galleryRecedeOffset(): Float =
        (contentInsetLeft() + layout.contentPad - layout.masterTextLeft).coerceAtLeast(0f)

    /**
     * 一级详情宿主的水平位移。
     *
     * 小屏跟 PageList 的 1 号页对齐：在 master 右侧屏外，被覆盖时整页滑走。
     * 大屏只做画廊后退，不跟 PageList 整页移走。
     */
    fun primaryHostShift(): Float =
        if (isCompact()) {
            // 被更深页盖住时停在原位，不要跟着 PageList 滑到左侧，否则回滑会露出并排接缝。
            (PAGE_PRIMARY - scrollPosition).coerceAtLeast(0f) * pageWidth()
        } else {
            primaryShift
        }

    /** 切一级详情：小屏离开根列表并推到 1 号页，同时收掉所有更深页面。 */
    fun selectPrimary(closeDeeperPages: Boolean = true) {
        val leavingMaster = isCompact() && compactOnMaster
        if (isCompact()) {
            compactOnMaster = false
        }
        if (closeDeeperPages && hasPushedPage()) {
            closeToPrimary()
        }
        if (leavingMaster) {
            pager.setTimeout(0) {
                scrollTo(PAGE_PRIMARY, animated = true)
            }
        }
        syncBackInterception()
        bumpSystemChromeLayout()
    }

    /** 在当前页之上再压一页，可以一直往下开三级、四级。 */
    fun open(
        id: String,
        title: String,
        data: Any? = null,
        animated: Boolean = true,
        content: FoldSplitPageContent,
    ) {
        require(id.isNotBlank()) { "FoldSplitStack page id must not be blank" }
        mutationVersion++
        // 刚侧滑回来但前向页还没卸载时，先裁掉再追加，避免页码错位。
        pruneTo(pageIndex.coerceAtLeast(PAGE_PRIMARY))
        pages.add(FoldSplitStackPage(id, title, data, content))
        if (isCompact()) {
            compactOnMaster = false
        }
        syncBackInterception()
        bumpSystemChromeLayout()
        scrollWhenMounted(topPageIndex(), animated)
    }

    /** 关闭顶部若干页；小屏在一级详情上再关一次会回到 master 根列表。 */
    fun close(levels: Int = 1, animated: Boolean = true): Boolean {
        if (levels <= 0) {
            return false
        }
        if (pageIndex > PAGE_PRIMARY) {
            mutationVersion++
            val target = (pageIndex - levels).coerceAtLeast(PAGE_PRIMARY)
            if (!scrollTo(target, animated)) {
                scrollWhenMounted(target, animated)
            }
            schedulePrune(target, if (animated) POP_FALLBACK_DELAY_MS else 0)
            bumpSystemChromeLayout()
            return true
        }
        if (isCompact() && !compactOnMaster) {
            backToMaster(animated)
            return true
        }
        return false
    }

    fun closeToPrimary(animated: Boolean = true): Boolean = close(pages.size, animated)

    /** 小屏回到 master 根列表。大屏没有这一层，调用无效。 */
    fun backToMaster(animated: Boolean = true) {
        if (!isCompact()) {
            return
        }
        mutationVersion++
        pruneTo(PAGE_PRIMARY)
        resetGallery()
        placeholderBackArmed = false
        compactOnMaster = true
        pageIndex = PAGE_PLACEHOLDER
        scrollTo(PAGE_PLACEHOLDER, animated)
        syncBackInterception()
        bumpSystemChromeLayout()
    }

    // endregion

    // region 组件内部

    internal fun attach(ref: ViewRef<PageListView<*, *>>) {
        pageListRef = ref
    }

    internal fun applyScroll(offsetX: Float, viewWidth: Float) {
        val width = viewWidth.coerceAtLeast(1f)
        scrollPosition = offsetX / width
        // 1 号页才是一级详情；相对 1 号页的位移才驱动画廊，滑向占位窗时进度为 0。
        val progress = coverProgress(PAGE_PRIMARY)
        primaryShift = -galleryRecedeOffset() * progress
        coverShadow = (4f * progress * (1f - progress)).coerceIn(0f, 1f)
    }

    internal fun onNativePageChanged(rawIndex: Int) {
        val previous = pageIndex
        val index = rawIndex.coerceIn(PAGE_PLACEHOLDER, topPageIndex())
        pageIndex = index
        if (index != previous) {
            bumpSystemChromeLayout()
        }
        if (index >= previous) {
            applyScroll(index * pageWidth(), pageWidth())
        }
        if (index >= PAGE_PRIMARY) {
            placeholderBackArmed = true
        }
        syncBackInterception()
        if (resizingStage) {
            return
        }
        if (index == PAGE_PLACEHOLDER) {
            if (isCompact()) {
                // 小屏停在 0 号页：透出底下的 master 根列表，不要弹回 1 号页。
                if (!compactOnMaster && placeholderBackArmed) {
                    backToMaster()
                }
            } else {
                pager.setTimeout(0) {
                    scrollTo(PAGE_PRIMARY, animated = false)
                }
            }
            return
        }
        if (index < previous) {
            // 等 PageList 的 SpringAnimation(400ms) 落稳再卸页。
            // 立刻 prune 会缩短 contentSize，目标页变成最后一页，弹簧过冲就会把二级页弹一下。
            schedulePrune(index, POP_FALLBACK_DELAY_MS)
        }
    }

    internal fun updateStage(width: Float, height: Float) {
        val widthChanged = abs(measuredWidth - width) > 0.5f
        if (widthChanged) {
            resizingStage = true
            keepPageIndexOnResize = when {
                pages.isNotEmpty() && pageIndex > PAGE_PRIMARY -> pageIndex
                compactOnMaster -> PAGE_PLACEHOLDER
                else -> PAGE_PRIMARY
            }
        }
        measuredWidth = width
        measuredHeight = height
        val hadStage = hasStageSize
        hasStageSize = true
        syncBreakpoint()
        syncBackInterception()
        bumpSystemChromeLayout()

        if (!widthChanged || !hadStage) {
            resizingStage = false
            return
        }
        settleAfterResize(keepPageIndexOnResize.coerceIn(PAGE_PLACEHOLDER, topPageIndex()))
    }

    /**
     * 断点可能因铰链半开而变，舞台像素却不变。布局和铰链通知都走这里。
     */
    internal fun syncBreakpoint() {
        val compact = isCompact()
        if (!breakpointReady) {
            // 首帧不是「从大屏收到小屏」：lastWasCompact 初始是 false，
            // 否则 iPhone 一进设置页就会把 compactOnMaster 打成 false，系统返回键出现在根列表上。
            breakpointReady = true
            lastWasCompact = compact
            if (!compact) {
                compactOnMaster = false
            }
            return
        }
        val becameCompact = compact && !lastWasCompact
        val becameWide = !compact && lastWasCompact
        if (becameCompact) {
            // 大屏收到小屏：停在当前这一级，不要退回根列表，也不要关掉已打开的页面。
            compactOnMaster = false
            masterCollapsed = false
            if (keepPageIndexOnResize == PAGE_PLACEHOLDER) {
                keepPageIndexOnResize = PAGE_PRIMARY
            }
        } else if (becameWide && compactOnMaster) {
            // 小屏根列表没有对应的大屏整页，展开后停在一级详情，列表回到主窗。
            compactOnMaster = false
            keepPageIndexOnResize = PAGE_PRIMARY
        }
        lastWasCompact = compact
    }

    /**
     * 改宽后 PageList 会按旧像素除以新页宽算出错误页码，连续两次归位，最后一帧才重新收页码和手势。
     */
    private fun settleAfterResize(target: Int) {
        val generation = ++resizeGeneration
        pager.setTimeout(0) first@{
            if (generation != resizeGeneration) {
                return@first
            }
            scrollTo(target, animated = false)
            pager.setTimeout(MOUNT_DELAY_MS) settle@{
                if (generation != resizeGeneration) {
                    return@settle
                }
                pageIndex = target
                applyScroll(target * pageWidth(), pageWidth())
                if (target <= PAGE_PRIMARY) {
                    pruneTo(PAGE_PRIMARY)
                    primaryShift = 0f
                    coverShadow = 0f
                }
                scrollTo(target, animated = false)
                resizingStage = false
                syncBackInterception()
                if (target > PAGE_PRIMARY) {
                    kickPageListSwipe()
                } else {
                    pageListSwipeGate = true
                }
            }
        }
    }

    /**
     * 改宽后原生 PageList 有时接不到左右 pan，直到子页增删或真正换页。
     * 先关掉再打开 scrollEnable，逼它重新挂手势。
     */
    private fun kickPageListSwipe() {
        if (isCompact() && compactOnMaster) {
            pageListSwipeGate = true
            return
        }
        pageListSwipeGate = false
        pager.setTimeout(0) {
            pageListSwipeGate = true
        }
    }

    private fun refreshGalleryForCurrentPage() {
        if (pageIndex > PAGE_PRIMARY) {
            applyScroll(pageIndex * pageWidth(), pageWidth())
        }
    }

    private fun resetGallery() {
        primaryShift = 0f
        coverShadow = 0f
    }

    private fun scrollTo(index: Int, animated: Boolean): Boolean =
        pageListRef?.view?.scrollToPageIndex(index, animated) == true

    private fun scrollWhenMounted(index: Int, animated: Boolean, attempt: Int = 0) {
        // 等新增的 PageList 子页真正挂载完成后再启动系统分页动画。
        pager.setTimeout(if (attempt == 0) MOUNT_DELAY_MS else SCROLL_RETRY_DELAY_MS) {
            if (!scrollTo(index, animated) && attempt < MAX_SCROLL_RETRY) {
                scrollWhenMounted(index, animated, attempt + 1)
            }
        }
    }

    private fun schedulePrune(index: Int, delay: Int) {
        val version = mutationVersion
        pager.setTimeout(delay) {
            if (version == mutationVersion && pageIndex == index) {
                pruneTo(index)
                if (index == PAGE_PRIMARY) {
                    resetGallery()
                }
                // 卸掉后一页后 contentSize 变短，无动画归位一次，避免停在越界 offset 上再回弹。
                scrollTo(index, animated = false)
                syncBackInterception()
            }
        }
    }

    private fun pruneTo(physicalIndex: Int) {
        val keep = (physicalIndex - PAGE_PRIMARY).coerceAtLeast(0)
        while (pages.size > keep) {
            pages.removeAt(pages.lastIndex)
        }
    }

    /**
     * 小屏停在详情侧、或任意屏停在压上来的页面时拦截返回键；
     * 小屏根列表和大屏一级详情不拦，把返回键还给宿主。
     */
    private fun shouldInterceptBack(): Boolean =
        hasPushedPage() || (isCompact() && !compactOnMaster)

    private fun syncBackInterception() {
        val handler = pager.getBackPressHandler()
        val registered = handler.containsCallback(inPageBack)
        if (shouldInterceptBack()) {
            if (!registered) {
                handler.addCallback(inPageBack)
            }
        } else if (registered) {
            handler.removeCallback(inPageBack)
        }
    }

    // endregion

    internal companion object {
        /** 透明占位窗。小屏滑到这里透出 master 根列表，大屏会被弹回一级详情。 */
        const val PAGE_PLACEHOLDER = 0

        /** 一级详情槽。小屏没有压栈页时内容挂在这里，才能整页滑回 0 号；否则透明，内容在外面那份宿主上。 */
        const val PAGE_PRIMARY = 1

        private const val MOUNT_DELAY_MS = 50
        private const val SCROLL_RETRY_DELAY_MS = 16
        private const val MAX_SCROLL_RETRY = 4
        private const val POP_FALLBACK_DELAY_MS = 450
        /** 普通 iPhone 系统栏一颗玻璃按钮大约占的宽度。 */
        private const val COMPACT_SYSTEM_ITEM = 48f
    }
}

private const val FOLD_SPLIT_Z_BEHIND_PAGES = 0
private const val FOLD_SPLIT_Z_PAGE_LIST = 1
private const val FOLD_SPLIT_Z_FRONT = 2

/** 与 UIKit `UIToolbar` 标准高度一致，供 [FoldSplitStackController.systemToolbarContentInsetBottom]。 */
internal const val FOLD_SYSTEM_TOOLBAR_HEIGHT = 49f

/**
 * 渲染主窗与详情栈。父容器给它一个确定尺寸即可（通常是 `flex(1f)`）。
 *
 * 组件只铺 frame：所有背景、圆角、玻璃、阴影、蒙版都由内容 lambda 自己画。
 *
 * [FoldSplitStackController.masterContent] 只创建一次。一级详情在小屏无压栈页时挂进
 * PageList 1 号槽，其余情况钉在外面那份宿主上。
 */
internal fun ViewContainer<*, *>.FoldSplitStack(controller: FoldSplitStackController) {
    View {
        attr {
            flex(1f)
            overflow(false)
            // 半开会改 isCompact / 主窗宽度，舞台像素可能不变，这里订阅铰链并刷新断点。
            controller.syncBreakpoint()
        }
        event {
            layoutFrameDidChange { frame ->
                controller.updateStage(frame.width, frame.height)
                controller.bumpSystemChromeLayoutPublic()
            }
        }
        foldSplitSystemChrome(controller)
        foldSplitMasterHost(controller)
        foldSplitPrimaryHost(controller)
        foldSplitPushedPageHosts(controller)
        foldSplitPageList(controller)
    }
}

private fun ViewContainer<*, *>.foldSplitSystemChrome(controller: FoldSplitStackController) {
    val chrome = controller.chrome ?: return
    vif({ controller.usesSystemChrome() }) {
        UINavigationBar {
            attr {
                tintColor(chrome.tintColor)
                visibility(true)
                hideScrollEdgeEffects(true)
                leadingInset(controller.chromeBackLeadingInset())
                trailingInset(controller.navBarTrailingInset)
                controller.systemChromeLayoutKey
                controller.requestedMasterWidth
                controller.masterCollapsed
            }
            items {
                navbarItems {
                    navbarItem {
                        attr {
                            symbol(chrome.backSymbol)
                            title(chrome.backTitle)
                            placement(UIBarButtonItemPlacement.Leading)
                            index(0)
                            itemGroup(0)
                            tintColor(chrome.tintColor)
                            axis(chrome.backAxis)
                            visibility(controller.shouldShowBack())
                        }
                        event {
                            click {
                                println("系统栏点击 返回")
                                controller.handleBack()
                            }
                        }
                    }
                    chrome.items.forEach { item ->
                        navbarItem {
                            attr {
                                placement(
                                    if (item.placement == "leading") {
                                        UIBarButtonItemPlacement.Leading
                                    } else {
                                        UIBarButtonItemPlacement.Trailing
                                    },
                                )
                                symbol(item.symbol)
                                title(item.title)
                                index(item.index)
                                itemGroup(item.itemGroup)
                                axis(item.axis)
                                tintColor(chrome.tintColor)
                                visibility(true)
                            }
                            event {
                                click {
                                    println("系统栏点击 导航 ${item.title}")
                                    item.onClick?.invoke()
                                }
                            }
                        }
                    }
                }
            }
        }
        foldSplitSystemToolbar(controller, chrome)
    }
}

private fun ViewContainer<*, *>.foldSplitSystemToolbar(
    controller: FoldSplitStackController,
    chrome: FoldSplitSystemChrome,
) {
    val toolbar = chrome.toolbar ?: return
    UIToolbar {
        attr {
            tintColor(chrome.tintColor)
            visibility(true)
            controller.systemChromeLayoutKey
            controller.requestedMasterWidth
            controller.masterCollapsed
            leadingInset(controller.toolbarLeadingOffset())
            trailingInset(controller.toolbarTrailingOffset())
        }
        items {
            vif({ controller.showsMaster() && controller.toolbarLeadingOffset() > 0f }) {
                toolbarItem {
                    attr {
                        kind(UIToolbarItemKind.FixedSpace)
                        spaceWidth(controller.toolbarLeadingOffset())
                        index(-2)
                        visibility(true)
                    }
                }
            }
            toolbar.items.forEach { item ->
                toolbarItem {
                    attr {
                        kind(
                            when (item.kind) {
                                "flexibleSpace" -> UIToolbarItemKind.FlexibleSpace
                                "fixedSpace" -> UIToolbarItemKind.FixedSpace
                                else -> UIToolbarItemKind.Item
                            },
                        )
                        symbol(item.symbol)
                        title(item.title)
                        index(item.index)
                        tintColor(chrome.tintColor)
                        axis(item.axis)
                        visibility(true)
                    }
                    event {
                        click {
                            println("系统栏点击 工具栏 ${item.title}")
                            item.onClick?.invoke()
                        }
                    }
                }
            }
            vif({ controller.showsMaster() && controller.toolbarTrailingOffset() > 0f }) {
                toolbarItem {
                    attr {
                        kind(UIToolbarItemKind.FixedSpace)
                        spaceWidth(controller.toolbarTrailingOffset())
                        index(1000)
                        visibility(true)
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldSplitPageList(controller: FoldSplitStackController) {
    PageList {
        ref {
            controller.attach(it)
        }
        attr {
            pageDirection(true)
            pageItemWidth(controller.pageWidth())
            pageItemHeight(controller.pageHeight())
            defaultPageIndex(
                if (controller.isCompact()) {
                    FoldSplitStackController.PAGE_PLACEHOLDER
                } else {
                    FoldSplitStackController.PAGE_PRIMARY
                },
            )
            // 0/1 两页首屏就要在，避免点进详情时下一页还没挂上。
            firstContentLoadMaxIndex(controller.pages.size + 2)
            keepItemAlive(true)
            bouncesEnable(false)
            showScrollerIndicator(false)
            hideTopScrollEdgeEffect(true)
            hideBottomScrollEdgeEffect(true)
            overflow(false)
            backgroundColor(Color.TRANSPARENT)
            zIndex(FOLD_SPLIT_Z_PAGE_LIST)
            // 大屏一级详情要点透到底下钉住的那层；只有压上来的页面才由 PageList 接手势。
            touchEnable(controller.pageListHandlesTouch())
            scrollEnable(controller.canSwipePageList())
        }
        event {
            scroll { params ->
                controller.applyScroll(params.offsetX, params.viewWidth)
            }
            pageIndexDidChanged { params ->
                controller.onNativePageChanged((params as JSONObject).optInt("index"))
            }
        }
        // 0 号页：透明占位窗。小屏 master 根列表钉在 PageList 底下，滑到这里直接透出来。
        View {
            attr {
                width(controller.pageWidth())
                height(controller.pageHeight())
                backgroundColor(Color.TRANSPARENT)
                touchEnable(false)
            }
        }
        // 1 号槽：小屏把一级详情挂进来，才能像原版 settings 那样整页滑回根列表。
        // 大屏 / 有压栈页时保持透明，真正内容在 PageList 外面那一份宿主上。
        View {
            attr {
                width(controller.pageWidth())
                height(controller.pageHeight())
                backgroundColor(Color.TRANSPARENT)
                touchEnable(controller.showsPrimaryInPageList())
            }
            vif({ controller.showsPrimaryInPageList() }) {
                controller.primaryContent(this, controller)
            }
        }
        vfor({ controller.pages }) { page ->
            View {
                attr {
                    width(controller.pageWidth())
                    height(controller.pageHeight())
                    backgroundColor(Color.TRANSPARENT)
                    overflow(false)
                }
                // 只有栈顶进 PageList：这一页跟着格子滑，并接手势。
                // 被盖住的页若也挂在相邻格子里，左滑时两页从中间接缝露出来。
                vif({ controller.isTopPushedPage(page) }) {
                    page.content(this, controller, page)
                }
            }
        }
    }
}

/**
 * 被盖住的压栈页钉在 PageList 下面。顶页从 PageList 里滑走时，透过透明格子看到的是
 * 停在原位的底页，而不是左侧那一格正在跟着滚走的兄弟页。
 */
private fun ViewContainer<*, *>.foldSplitPushedPageHosts(controller: FoldSplitStackController) {
    vfor({ controller.pages }) { page ->
        View {
            attr {
                positionAbsolute()
                touchEnable(false)
                if (controller.isTopPushedPage(page)) {
                    // 栈顶页的内容在 PageList 里。这里收成 0，避免空壳盖住真正钉住的底页。
                    left(0f)
                    top(0f)
                    width(0f)
                    height(0f)
                    right(Float.undefined)
                    bottom(Float.undefined)
                } else {
                    left(0f)
                    top(0f)
                    right(0f)
                    bottom(0f)
                    width(Float.undefined)
                    height(Float.undefined)
                    zIndex(FOLD_SPLIT_Z_BEHIND_PAGES)
                }
            }
            vif({ !controller.isTopPushedPage(page) }) {
                page.content(this, controller, page)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldSplitPrimaryHost(controller: FoldSplitStackController) {
    View {
        attr {
            positionAbsolute()
            touchEnable(
                controller.showsPrimaryHost() &&
                    !controller.isOnPushedSlot() &&
                    !(controller.isCompact() && controller.compactOnMaster),
            )
            if (controller.showsPrimaryHost()) {
                left(0f)
                top(0f)
                right(0f)
                bottom(0f)
                width(Float.undefined)
                height(Float.undefined)
                zIndex(FOLD_SPLIT_Z_BEHIND_PAGES)
                transform(Translate(0f, 0f, controller.primaryHostShift(), 0f))
            } else {
                // 内容已挂进 PageList 1 号槽，这里收成 0，避免空壳挡住左右滑。
                left(0f)
                top(0f)
                width(0f)
                height(0f)
                right(Float.undefined)
                bottom(Float.undefined)
            }
        }
        vif({ controller.showsPrimaryHost() }) {
            controller.primaryContent(this, controller)
        }
    }
}

private fun ViewContainer<*, *>.foldSplitMasterHost(controller: FoldSplitStackController) {
    View {
        attr {
            positionAbsolute()
            val chrome = controller.masterChromeInset()
            // 三套 frame 都必须写全：收起时的 height(0) 若不在展开分支清掉，侧栏会继续是 0 高。
            if (controller.isCompact()) {
                left(0f)
                top(0f)
                right(0f)
                bottom(0f)
                width(Float.undefined)
                height(Float.undefined)
                zIndex(FOLD_SPLIT_Z_BEHIND_PAGES)
                touchEnable(true)
            } else if (controller.showsMaster()) {
                left(controller.masterFrameLeft())
                top(chrome)
                right(Float.undefined)
                bottom(chrome)
                width(controller.masterWindowWidth())
                height(Float.undefined)
                zIndex(FOLD_SPLIT_Z_FRONT)
                touchEnable(true)
            } else {
                left(controller.masterFrameLeft())
                top(chrome)
                right(Float.undefined)
                bottom(Float.undefined)
                width(0f)
                height(0f)
                zIndex(FOLD_SPLIT_Z_BEHIND_PAGES)
                touchEnable(false)
            }
        }
        controller.masterContent(this, controller)
        vif({ controller.showsMaster() && controller.layout.masterResizable && !controller.isSideBySideHinge() }) {
            foldSplitMasterResizeHandle(controller)
        }
    }
}

/** 右边缘拖宽热区，属于窗口尺寸的一部分；指示条做得很轻，不干扰适用方的视觉。 */
private fun ViewContainer<*, *>.foldSplitMasterResizeHandle(
    controller: FoldSplitStackController,
) {
    View {
        attr {
            positionAbsolute()
            right(0f)
            top(0f)
            bottom(0f)
            width(16f)
            allCenter()
        }
        event {
            pan { params ->
                when (params.state) {
                    "start" -> controller.dragMasterWidth(params.pageX, isStart = true)
                    "move" -> controller.dragMasterWidth(params.pageX, isStart = false)
                }
            }
        }
        View {
            attr {
                width(4f)
                height(36f)
                borderRadius(2f)
                backgroundColor(Color(0x66C7C7CC))
            }
        }
    }
}
