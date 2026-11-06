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
 * 「自制标题栏」验证组件 —— **原生 Navigation**（非 HdsNavigation）的官方能力，
 * 对标 [ImmersiveLightSenseNavigationOhos]（HdsNavigation 版）。
 *
 * 核心验证点（官方文档「示例20：systemMaterial 开启标题栏材质效果」同款路径）：
 * - `NavigationTitleOptions.systemMaterial`：标题栏系统材质（返回键/menu 背板生效）；
 * - `NavigationTitleOptions.scrollEffectOptions`：滚动渐变模糊
 *   （COMMON_BLUR / GRADIENT_BLUR，API 26）。
 *
 * 与 HdsNavigation 版的差异：
 * - 标题栏是原生 Navigation 的标准标题栏（文字+返回键，无自定义菜单）；
 * - 无 HdsNavigation 的 menus/样式 options（能力子集，作为"自制路线能达到什么"的对照）。
 */
fun ViewContainer<*, *>.ImmersiveLightSenseNavigationSelfDrawnOhos(
    init: ImmersiveLightSenseNavigationSelfDrawnOhosView.() -> Unit
) {
    ImmersiveLightSenseNavigationSelfDrawnOhosView().also {
        addChild(it, init)
    }
}

class ImmersiveLightSenseNavigationSelfDrawnOhosView :
    GroupView<ImmersiveLightSenseNavigationSelfDrawnOhosAttr, ImmersiveLightSenseNavigationSelfDrawnOhosEvent>() {

    override fun createAttr(): ImmersiveLightSenseNavigationSelfDrawnOhosAttr =
        ImmersiveLightSenseNavigationSelfDrawnOhosAttr()

    override fun createEvent(): ImmersiveLightSenseNavigationSelfDrawnOhosEvent =
        ImmersiveLightSenseNavigationSelfDrawnOhosEvent()

    override fun viewName(): String = VIEW_NAME

    companion object {
        /** wire 名：与原生侧 ViewsRegisterEntry 注册名一致 */
        const val VIEW_NAME = "KRImmersiveNavigationSelfDrawnView"
    }
}

/**
 * attr DSL（与 HdsNavigation 版 attr 面一致，便于两版对照）：
 * ```
 * ImmersiveLightSenseNavigationSelfDrawnOhos {
 *     attr {
 *         absolutePosition(0f, 0f, 0f, 0f)
 *         title("标题")
 *         titleMode(2)                      // Mini
 *         enableScrollEffect(true)
 *         scrollEffectType(1)               // GRADIENT_BLUR
 *         immersiveMaterial(style = 2)      // 标题栏 systemMaterial
 *     }
 * }
 * ```
 */
class ImmersiveLightSenseNavigationSelfDrawnOhosAttr : GroupAttr() {

    /** 主标题文字 */
    fun title(title: String) {
        KEY_TITLE with title
    }

    /** 标题模式：0=Free 1=Full 2=Mini（默认） */
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

    /** 滚动效果类型：0=COMMON_BLUR（普通模糊）1=GRADIENT_BLUR（渐变模糊，默认） */
    fun scrollEffectType(type: Int) {
        KEY_SCROLL_EFFECT_TYPE with type
    }

    /**
     * 内容区背景色。原生 Navigation 内容区默认白底（HdsNavigation 默认透明），
     * 设透明可对齐 HdsNavigation 观感；也可设任意纯色。
     */
    fun contentBackgroundColor(color: Color) {
        KEY_CONTENT_BACKGROUND_COLOR with color.toString()
    }

    private companion object {
        const val KEY_TITLE = "title"
        const val KEY_TITLE_MODE = "titleMode"
        const val KEY_HIDE_BACK_BUTTON = "hideBackButton"
        const val KEY_ENABLE_SCROLL_EFFECT = "enableScrollEffect"
        const val KEY_SCROLL_EFFECT_TYPE = "scrollEffectType"
        const val KEY_CONTENT_BACKGROUND_COLOR = "contentBackgroundColor"
    }
}

class ImmersiveLightSenseNavigationSelfDrawnOhosEvent : GroupEvent() {
}
