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

package com.tencent.kuikly.core.views.ohos

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.internal.GroupAttr
import com.tencent.kuikly.core.views.internal.GroupEvent
import com.tencent.kuikly.core.views.internal.GroupView

/**
 * 创建一个鸿蒙「沉浸光感」TabBar 容器（Immersive Light Sense TabBar）。
 *
 * 官方判定沉浸光感只在两个区域生效 —— Navigation 标题栏 和 **Tabbar**
 * （系统告警原文：`Material inactive: out of scope. Use component in navigation
 * title bar or Tabbar.`）。[ImmersiveLightSenseOhos] 覆盖标题栏，本组件覆盖底部页签栏。
 *
 * 该容器在 OHOS 侧走 ArkTS 转发通道（V2）：容器本体渲染为原生 `Tabs`
 * （barPosition = End），子视图被挂进页签栏区域的 ContentSlot，因此落在 Tabbar scope 内
 * —— 即便子视图是 Kuikly 自绘节点，也能正常渲染沉浸材质。
 *
 * 使用约束（与标题栏容器一致）：
 * 1. 只有放在本容器内的组件才能获得沉浸光感，容器外的页面组件不受影响；
 * 2. 材质要设在**子视图**上（见 [com.tencent.kuikly.core.base.Attr.immersiveMaterial]）；
 * 3. 子视图背景需保持透明，否则会遮挡材质层；
 * 4. 语义上它是页面底部的页签栏，与自绘页签栏二选一，不要同时存在；
 * 5. 官方建议控制材质面积，且不要嵌套材质容器。
 *
 * 不支持沉浸光感的设备上自动降级为普通容器（子视图照常显示，只是没有材质）。
 */
fun ViewContainer<*, *>.ImmersiveLightSenseTabBarOhos(
    init: ImmersiveLightSenseTabBarOhosView.() -> Unit
) {
    ImmersiveLightSenseTabBarOhosView().also {
        addChild(it, init)
    }
}

open class ImmersiveLightSenseTabBarOhosView :
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
 * 这是**原生组件**：页签项由 ArkUI `Tabs` 原生渲染，业务通过 [items] / [selectedIndex]
 * 传数据，不再往里面放自绘子视图。
 *
 * 通用布局属性中，**高度**即页签栏高度。
 * 材质参数用基类 [com.tencent.kuikly.core.base.Attr.immersiveMaterial]。
 */
class ImmersiveLightSenseTabBarOhosAttr : GroupAttr() {

    /** 页签文案列表（原生渲染，不支持自绘视图） */
    fun items(items: List<String>) {
        KEY_ITEMS with items.joinToString(",")
    }

    /** 当前选中页签下标 */
    fun selectedIndex(index: Int) {
        KEY_SELECTED_INDEX with index
    }

    private companion object {
        const val KEY_ITEMS = "items"
        const val KEY_SELECTED_INDEX = "selectedIndex"
    }
}

/**
 * 沉浸光感 TabBar 的事件。
 */
class ImmersiveLightSenseTabBarOhosEvent : GroupEvent() {

    internal var tabChangedHandlerFn: ((Int) -> Unit)? = null

    /** 页签切换，回传新的选中下标 */
    fun tabChanged(handler: (Int) -> Unit) {
        tabChangedHandlerFn = handler
        register(KEY_TAB_CHANGED) { params ->
            // 原生侧传的是 JSON 字符串（{"index":N}），框架解码后是 Map；
            // 值可能是 Int / Double / String，这里都兼容，避免解析不到
            val value: Any? = if (params is Map<*, *>) params["index"] else params
            val index = when (value) {
                is Number -> value.toInt()
                is String -> value.toFloatOrNull()?.toInt() ?: value.toIntOrNull()
                else -> null
            }
            handler(index ?: 0)
        }
    }

    private companion object {
        const val KEY_TAB_CHANGED = "tabChanged"
    }
}
