/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of the KuiklyUI license;
 * you may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.views.ohos.immersive

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.internal.GroupAttr
import com.tencent.kuikly.core.views.internal.GroupEvent
import com.tencent.kuikly.core.views.internal.GroupView

/**
 * 创建「自绘 TabBar」（Immersive Light Sense Self-Drawn TabBar）。
 *
 * 与 [ImmersiveLightSenseTabBarOhos]（HdsTabs 版）并存的实验路线：
 * 原生 Tabs（barPosition End）提供生效区域，**页签格由 ArkTS 自绘**，
 * 胶囊外形（圆角/高度）完全由业务控制 —— 消除 HdsTabs 版"圆角不可控"的官方限制。
 *
 * 差异速查：
 * - 本版：外形自由（pillRadius/pillHeight 任意）、格子是普通节点（点击直接可用）；
 *   无 HDS 内置选中动画。
 * - HdsTabs 版：官方胶囊样式 + 内置切换动画 + 材质由系统层合成（质感最佳）；
 *   圆角不可控、点击命中不可达（需业务侧透明点击条）。
 *
 * 验证用法：
 * ```
 * ImmersiveLightSenseTabBarSelfDrawnOhos {
 *     attr {
 *         absolutePosition(left = 0f, right = 0f, bottom = 0f)  // 全宽贴底
 *         height(68f)                                            // bar 区域高
 *         items(listOf("首页", "发现", "我的"))
 *         pillRadius(26f)          // ← 圆角任意：本组件的核心卖点
 *         pillHeight(52f)
 *         immersiveMaterial(style = 2)
 *     }
 *     event { tabChanged { index -> } }
 * }
 * ```
 */
fun ViewContainer<*, *>.ImmersiveLightSenseTabBarSelfDrawnOhos(
    init: ImmersiveLightSenseTabBarSelfDrawnOhosView.() -> Unit
) {
    ImmersiveLightSenseTabBarSelfDrawnOhosView().also {
        addChild(it, init)
    }
}

class ImmersiveLightSenseTabBarSelfDrawnOhosView :
    GroupView<ImmersiveLightSenseTabBarSelfDrawnOhosAttr, ImmersiveLightSenseTabBarSelfDrawnOhosEvent>() {

    override fun createAttr(): ImmersiveLightSenseTabBarSelfDrawnOhosAttr =
        ImmersiveLightSenseTabBarSelfDrawnOhosAttr()

    override fun createEvent(): ImmersiveLightSenseTabBarSelfDrawnOhosEvent =
        ImmersiveLightSenseTabBarSelfDrawnOhosEvent()

    override fun viewName(): String = VIEW_NAME

    companion object {
        const val VIEW_NAME = "KRImmersiveTabBarSelfDrawnView"
    }
}

/** 自绘 TabBar 的属性。布局属性照常走通用 attr；材质用基类 immersiveMaterial()。 */
class ImmersiveLightSenseTabBarSelfDrawnOhosAttr : GroupAttr() {

    /** 页签文案列表 */
    fun items(items: List<String>): ImmersiveLightSenseTabBarSelfDrawnOhosAttr {
        KEY_ITEMS with items.joinToString(",")
        return this
    }

    /** 当前选中页签下标 */
    fun selectedIndex(index: Int): ImmersiveLightSenseTabBarSelfDrawnOhosAttr {
        KEY_SELECTED_INDEX with index
        return this
    }

    /** 整个 bar 的圆角（vp）—— 作用在 bar 底板层（不是单个 item 格子），任意值 */
    fun pillRadius(radius: Float): ImmersiveLightSenseTabBarSelfDrawnOhosAttr {
        KEY_PILL_RADIUS with radius
        return this
    }

    /**
     * bar 底板背景色，默认透明：底板不透明时格子的沉浸光感材质折射到的是
     * 纯色底板（看不出效果），透明让页面背景透进 bar 区域、材质有内容可折射。
     */
    fun pillBackgroundColor(color: Color): ImmersiveLightSenseTabBarSelfDrawnOhosAttr {
        KEY_PILL_BACKGROUND_COLOR with color.toString()
        return this
    }

    /** 悬浮位置：胶囊左右各内缩的距离（FloatingTabBarStyle.barSideMargin，默认 16） */
    fun barInsetX(inset: Float): ImmersiveLightSenseTabBarSelfDrawnOhosAttr {
        KEY_BAR_INSET_X with inset
        return this
    }

    /** 悬浮位置：胶囊底部抬离容器底的距离（FloatingTabBarStyle.barBottomMargin，默认 48） */
    fun barBottomOffset(offset: Float): ImmersiveLightSenseTabBarSelfDrawnOhosAttr {
        KEY_BAR_BOTTOM_OFFSET with offset
        return this
    }

    /** 胶囊格高度（vp） */
    fun pillHeight(height: Float): ImmersiveLightSenseTabBarSelfDrawnOhosAttr {
        KEY_PILL_HEIGHT with height
        return this
    }

    companion object {
        private const val KEY_ITEMS = "items"
        private const val KEY_SELECTED_INDEX = "selectedIndex"
        private const val KEY_PILL_RADIUS = "pillRadius"
        private const val KEY_PILL_HEIGHT = "pillHeight"
        private const val KEY_PILL_BACKGROUND_COLOR = "pillBackgroundColor"
        private const val KEY_BAR_INSET_X = "barInsetX"
        private const val KEY_BAR_BOTTOM_OFFSET = "barBottomOffset"
    }
}

/** 自绘 TabBar 的事件。 */
class ImmersiveLightSenseTabBarSelfDrawnOhosEvent : GroupEvent() {

    /** 页签切换，回传新的选中下标（原生侧 JSON 字符串，正则提取，与 HdsTabs 版一致） */
    fun tabChanged(handler: (Int) -> Unit) {
        register(KEY_TAB_CHANGED) { params ->
            val text = params?.toString() ?: ""
            val match = Regex("index\"?\\s*:\\s*(-?\\d+)").find(text)
            handler(match?.groupValues?.get(1)?.toIntOrNull() ?: 0)
        }
    }

    companion object {
        private const val KEY_TAB_CHANGED = "tabChanged"
    }
}
