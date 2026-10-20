/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. KuiklyUI is licensed under the terms of
 * the KuiklyUI license, and you may not use it except in compliance
 * with the License. You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.views.ohos.immersive

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.internal.GroupAttr
import com.tencent.kuikly.core.views.internal.GroupEvent
import com.tencent.kuikly.core.views.internal.GroupView

/**
 * 创建一个鸿蒙「沉浸光感」TabBar（Immersive Light Sense TabBar）。
 *
 * 官方判定沉浸光感只在 Navigation 标题栏 与 **Tabbar** 两个区域生效；
 * [ImmersiveLightSenseOhos] 覆盖标题栏，本组件覆盖底部页签栏。
 *
 * 本组件**完全由 ArkTS 渲染**（与 Dialog / Switch 同类用法：业务只传数据与回调）：
 * - 悬浮胶囊、页签项、选中态、材质全部由 ArkTS 绘制；
 * - 页面内容区由原生 Swiper 提供，左右滑动可切换页签；
 * - 材质设在胶囊整块上，且落在 tabBar scope 内（这正是材质能生效的位置）。
 *
 * 悬浮位置模板（对齐官方示例）：
 * - [barInsetX]：胶囊左右各内缩的距离（对称悬浮）；
 * - [barBottomOffset]：胶囊底部抬离容器底的距离（悬浮感）。
 *
 * 不要往本组件里放子视图 —— 栏不是用 ContentSlot 承载自绘内容的
 * （自绘栏与原生容器会产生双路点击事件，实测页签会在两个索引间来回打架）。
 *
 * 不支持的设备上自动降级：栏照常显示，只是没有材质。
 */
fun ViewContainer<*, *>.ImmersiveLightSenseTabBarOhos(
    init: ImmersiveLightSenseTabBarOhosView.() -> Unit
) {
    ImmersiveLightSenseTabBarOhosView().also {
        addChild(it, init)
    }
}

class ImmersiveLightSenseTabBarOhosView :
    GroupView<ImmersiveLightSenseTabBarOhosAttr, ImmersiveLightSenseTabBarOhosEvent>() {

    override fun createAttr(): ImmersiveLightSenseTabBarOhosAttr = ImmersiveLightSenseTabBarOhosAttr()

    override fun createEvent(): ImmersiveLightSenseTabBarOhosEvent = ImmersiveLightSenseTabBarOhosEvent()

    override fun viewName(): String = VIEW_NAME

    companion object {
        const val VIEW_NAME = "KRImmersiveLightSenseTabBarView"
    }
}

/**
 * 沉浸光感 TabBar 的属性。
 *
 * 通用布局属性中，**高度**应不小于 barHeight + barBottomOffset（胶囊 + 抬离量）。
 * 材质参数用基类 [com.tencent.kuikly.core.base.Attr.immersiveMaterial]。
 */
class ImmersiveLightSenseTabBarOhosAttr : GroupAttr() {

    /** 页签文案列表 */
    fun items(items: List<String>) {
        KEY_ITEMS with items.joinToString(",")
    }

    /** 当前选中页签下标（组件会同步驱动原生 Swiper） */
    fun selectedIndex(index: Int) {
        KEY_SELECTED_INDEX with index
    }

    /** 页签栏高度（胶囊整体圆角 = 高度 / 2） */
    fun barHeight(height: Float) {
        KEY_BAR_HEIGHT with height
    }

    /** 悬浮内缩：胶囊左右各离开屏幕边的距离 */
    fun barInsetX(inset: Float) {
        KEY_BAR_INSET_X with inset
    }

    /** 悬浮抬离：胶囊底部离开容器底的距离 */
    fun barBottomOffset(offset: Float) {
        KEY_BAR_BOTTOM_OFFSET with offset
    }

    private companion object {
        const val KEY_ITEMS = "items"
        const val KEY_SELECTED_INDEX = "selectedIndex"
        const val KEY_BAR_HEIGHT = "barHeight"
        const val KEY_BAR_INSET_X = "barInsetX"
        const val KEY_BAR_BOTTOM_OFFSET = "barBottomOffset"
    }
}

/**
 * 沉浸光感 TabBar 的事件。
 *
 * 触发来源：原生 Swiper 滑动切换、点击 ArkTS 页签项。
 */
class ImmersiveLightSenseTabBarOhosEvent : GroupEvent() {

    /** 页签切换，回传新的选中下标 */
    fun tabChanged(handler: (Int) -> Unit) {
        register(KEY_TAB_CHANGED) { params ->
            // 原生侧传的是 JSON 字符串（{"index":N}），框架解码成它自己的 JSONObject
            // （不是 kotlin Map，is Map 判断不成立）。这里直接从字符串形态里提取下标，
            // 对 Map / JSONObject / 裸字符串三种形态都成立。
            val text = params?.toString() ?: ""
            val match = Regex("index\"?\\s*[:=]\\s*(-?\\d+)").find(text)
            handler(match?.groupValues?.get(1)?.toIntOrNull() ?: 0)
        }
    }

    private companion object {
        const val KEY_TAB_CHANGED = "tabChanged"
    }
}
