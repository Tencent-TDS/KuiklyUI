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

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.internal.GroupAttr
import com.tencent.kuikly.core.views.internal.GroupEvent
import com.tencent.kuikly.core.views.internal.GroupView

/**
 * 创建一个鸿蒙「沉浸光感」Select（ArkUI Select）。
 *
 * 官方白名单：Slider / Toggle / **Select** / 弹窗类组件可**全页面生效**沉浸光感，
 * 不受「必须在 Navigation 标题栏或底部 TabBar 内」的位置限制。
 * Kuikly 自绘的 Select 不在名单里拿不到材质，只有原生 Select 才行。
 *
 * 原生侧为 ArkTS 转发实现（CAPI 没有 ARKUI_NODE_SELECT），与 TabBar 同一条通道；
 * 布局尺寸照常由 Kuikly frame 决定（attr 的 size/width/height）。
 *
 * 用法：
 * ```
 * ImmersiveLightSenseSelectOhos {
 *     attr {
 *         width(150f); height(40f)
 *         options(listOf("低", "中", "高"))
 *         selectedIndex(1)
 *         immersiveMaterial(style = 2)   // 可选
 *     }
 *     event { selectChanged { index, value -> } }
 * }
 * ```
 */
fun ViewContainer<*, *>.ImmersiveLightSenseSelectOhos(
    init: ImmersiveLightSenseSelectOhosView.() -> Unit
) {
    ImmersiveLightSenseSelectOhosView().also {
        addChild(it, init)
    }
}

class ImmersiveLightSenseSelectOhosView :
    GroupView<ImmersiveLightSenseSelectOhosAttr, ImmersiveLightSenseSelectOhosEvent>() {

    override fun createAttr(): ImmersiveLightSenseSelectOhosAttr = ImmersiveLightSenseSelectOhosAttr()

    override fun createEvent(): ImmersiveLightSenseSelectOhosEvent = ImmersiveLightSenseSelectOhosEvent()

    override fun viewName(): String = VIEW_NAME

    companion object {
        const val VIEW_NAME = "KRImmersiveSelectView"
    }
}

/**
 * 沉浸光感 Select 的属性。
 * 布局属性（size/margin 等）照常走通用 attr；材质用基类 immersiveMaterial()。
 */
class ImmersiveLightSenseSelectOhosAttr : GroupAttr() {

    /** 选项文案列表（决定 item 数量） */
    fun options(options: List<String>): ImmersiveLightSenseSelectOhosAttr {
        KEY_OPTIONS with options.joinToString(",")
        return this
    }

    /** 当前选中项下标 */
    fun selectedIndex(index: Int): ImmersiveLightSenseSelectOhosAttr {
        KEY_SELECTED_INDEX with index
        return this
    }

    companion object {
        private const val KEY_OPTIONS = "options"
        private const val KEY_SELECTED_INDEX = "selectedIndex"
    }
}

/** 沉浸光感 Select 的事件。 */
class ImmersiveLightSenseSelectOhosEvent : GroupEvent() {

    /**
     * 选中某一项。回传 (下标, 文案)。
     * 原生侧以 JSON 字符串回传（{"index":N,"value":"x"}），这里用正则提取，
     * 与 TabBar 的 tabChanged 同一套容错逻辑。
     */
    fun selectChanged(handler: (index: Int, value: String) -> Unit) {
        register(KEY_SELECT_CHANGED) { params ->
            val text = params?.toString() ?: ""
            val index = Regex("index\"?\\s*:\\s*(-?\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val value = Regex("value\"?\\s*:\\s*\"([^\"]*)\"").find(text)?.groupValues?.get(1) ?: ""
            handler(index, value)
        }
    }

    companion object {
        private const val KEY_SELECT_CHANGED = "selectChanged"
    }
}
