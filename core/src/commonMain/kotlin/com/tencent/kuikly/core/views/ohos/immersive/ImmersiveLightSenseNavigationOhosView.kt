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
 * HdsNavigation 标题模式，取值与官方 `HdsNavigationTitleMode` 一一对应。
 * 供 [ImmersiveLightSenseNavigationOhosAttr.titleMode] 使用。
 */
object HdsNavigationTitleMode {
    /** 自由模式：标题随滚动联动缩放 */
    const val FREE = 0

    /** 全屏模式：大标题 */
    const val FULL = 1

    /** 迷你模式：常规单行标题（默认） */
    const val MINI = 2

    /** 模态模式：模态页面标题 */
    const val MODAL = 3
}

/**
 * 标题栏滚动效果类型，取值与官方 `ScrollEffectType` 一一对应。
 * 供 [ImmersiveLightSenseNavigationOhosAttr.scrollEffectType] 使用。
 */
object HdsScrollEffectType {
    /** 普通模糊：对背景均匀模糊，随透明渐变显隐 */
    const val COMMON_BLUR = 0

    /** 渐进模糊：均匀模糊且边界清晰，标题栏内容随滚动线性变化 */
    const val GRADUAL_BLUR = 1

    /** 渐变模糊：渐变模糊样式 */
    const val GRADIENT_BLUR = 2

    /** 沉浸式渐变模糊：HdsIosTabs 等官方示例采用的效果 */
    const val IMMERSIVE_GRADIENT_BLUR = 3
}

/**
 * 官方 HdsNavigation 组件（鸿蒙「沉浸光感」标题栏 + 滚动渐变）。
 *
 * 能力面（官方 SDK 声明 + 官方示例）：
 * - [ImmersiveLightSenseNavigationOhosAttr.title]：主标题文字；
 * - [ImmersiveLightSenseNavigationOhosAttr.titleMode]：0=FREE / 1=FULL / 2=MINI（默认）；
 * - [ImmersiveLightSenseNavigationOhosAttr.hideBackButton]：隐藏返回按钮；
 * - [ImmersiveLightSenseNavigationOhosAttr.enableScrollEffect] +
 *   [ImmersiveLightSenseNavigationOhosAttr.scrollEffectType]：滚动联动 ——
 *   内容上滚时标题栏产生渐变模糊（1=GRADIENT_BLUR，沉浸光感核心视效）
 *   或普通模糊（0=COMMON_BLUR）；
 * - [com.tencent.kuikly.core.base.Attr.immersiveMaterial]：标题栏按钮的沉浸光感材质；
 * - 标题栏菜单固定 3 个（图标+label），组件内部提供，暂不开放定制。
 *
 * 用法：组件**全屏挂载**（absolutePosition 四边全给），内容区由组件内部的
 * 滚动长图承载（验证滚动渐变用）；控制面板等 Kuikly 内容声明在其后、浮于其上。
 */
fun ViewContainer<*, *>.ImmersiveLightSenseNavigationOhos(
    init: ImmersiveLightSenseNavigationOhosView.() -> Unit
) {
    ImmersiveLightSenseNavigationOhosView().also {
        addChild(it, init)
    }
}

class ImmersiveLightSenseNavigationOhosView :
    GroupView<ImmersiveLightSenseNavigationOhosAttr, ImmersiveLightSenseNavigationOhosEvent>() {

    override fun createAttr(): ImmersiveLightSenseNavigationOhosAttr = ImmersiveLightSenseNavigationOhosAttr()

    override fun createEvent(): ImmersiveLightSenseNavigationOhosEvent = ImmersiveLightSenseNavigationOhosEvent()

    override fun viewName(): String = VIEW_NAME

    companion object {
        /** wire 名：与原生侧 ViewsRegisterEntry 注册名一致 */
        const val VIEW_NAME = "KRImmersiveNavigationOhosView"
    }
}

/**
 * attr DSL：
 * ```
 * ImmersiveLightSenseNavigationOhos {
 *     attr {
 *         absolutePosition(0f, 0f, 0f, 0f)  // 全屏
 *         title("MainTitle")
 *         titleMode(2)                      // MINI
 *         enableScrollEffect(true)
 *         scrollEffectType(1)               // GRADIENT_BLUR 渐变模糊
 *         immersiveMaterial(style = 2)
 *     }
 * }
 * ```
 */
class ImmersiveLightSenseNavigationOhosAttr : GroupAttr() {

    /** 主标题文字 */
    fun title(title: String) {
        KEY_TITLE with title
    }

    /** 副标题（HdsNavigation 标题栏主标题下方的小字） */
    fun subTitle(subTitle: String) {
        KEY_SUB_TITLE with subTitle
    }

    /**
     * 菜单按钮图标（sys.symbol 符号名，不带前缀；与 [menuLabels] 同下标组成一个菜单项）。
     * 可用符号以原生侧映射表为准，查不到回退默认图标。
     */
    fun menuIcons(icons: List<String>) {
        KEY_MENU_ICONS with icons.joinToString(",")
    }

    /** 菜单按钮文字（与 [menuIcons] 同下标） */
    fun menuLabels(labels: List<String>) {
        KEY_MENU_LABELS with labels.joinToString(",")
    }

    /** 标题模式，取值见 [HdsNavigationTitleMode] */
    fun titleMode(mode: Int) {
        KEY_TITLE_MODE with mode
    }

    /** 是否隐藏返回按钮（默认 false 显示） */
    fun hideBackButton(hide: Boolean) {
        KEY_HIDE_BACK_BUTTON with hide.toString()
    }

    /** 滚动联动总开关：内容上滚时标题栏产生模糊/渐变 */
    fun enableScrollEffect(enable: Boolean) {
        KEY_ENABLE_SCROLL_EFFECT with enable.toString()
    }

    /** 滚动效果类型，取值见 [HdsScrollEffectType] */
    fun scrollEffectType(type: Int) {
        KEY_SCROLL_EFFECT_TYPE with type
    }

    private companion object {
        const val KEY_TITLE = "title"
        const val KEY_SUB_TITLE = "subTitle"
        const val KEY_MENU_ICONS = "menuIcons"
        const val KEY_MENU_LABELS = "menuLabels"
        const val KEY_TITLE_MODE = "titleMode"
        const val KEY_HIDE_BACK_BUTTON = "hideBackButton"
        const val KEY_ENABLE_SCROLL_EFFECT = "enableScrollEffect"
        const val KEY_SCROLL_EFFECT_TYPE = "scrollEffectType"
    }
}

class ImmersiveLightSenseNavigationOhosEvent : GroupEvent() {

    /** 标题栏菜单按钮点击（index 为菜单项下标，与 menuIcons/menuLabels 对应） */
    fun onMenuClick(handler: (Int) -> Unit) {
        register(KEY_MENU_CLICK) { params ->
            // 原生侧传 JSON 字符串（{"index":N}），框架解码成自己的 JSONObject。
            // 与 tabChanged 同款：从字符串形态提取下标，对 Map/JSONObject/裸字符串均成立。
            val text = params?.toString() ?: ""
            val match = Regex("index\"?\\s*[:=]\\s*(-?\\d+)").find(text)
            val index = match?.groupValues?.get(1)?.toIntOrNull() ?: -1
            handler(index)
        }
    }

    private companion object {
        const val KEY_MENU_CLICK = "menuClick"
    }
}
