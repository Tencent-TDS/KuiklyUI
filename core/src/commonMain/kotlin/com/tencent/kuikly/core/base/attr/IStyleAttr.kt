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

package com.tencent.kuikly.core.base.attr

import com.tencent.kuikly.core.base.Anchor
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderRectRadius
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.Rotate
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.Skew

/**
 * 样式属性接口，用于设置视图的样式属性。
 */
interface IStyleAttr {

    // region: zIndex
    /**
     * 设置视图的层叠顺序。
     * @param zIndex 层叠顺序值。
     * @param useOutline Android使用OutlineViewProvider开关，为了向前兼容，默认开。设为false可以解决zIndex阴影问题。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun zIndex(zIndex: Int, useOutline: Boolean = true): IStyleAttr
    // endregion

    /**
     * 设置视图的背景颜色。
     * @param color 背景颜色值。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun backgroundColor(color: Color): IStyleAttr

    /**
     * 设置视图的背景渐变色。
     * @param direction 渐变色方向。
     * @param colorStops 渐变色设置，至少需要传入两个参数，表示起始颜色和终止颜色。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun backgroundLinearGradient(direction: Direction, vararg colorStops: ColorStop): IStyleAttr

    /**
     * 设置视图的阴影。
     * @param boxShadow 阴影属性。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun boxShadow(boxShadow: BoxShadow): IStyleAttr
    // endregion

    /**
     * 设置视图的圆角边框。
     * 注：设置圆角后，则子孩子无法超出自身区域显示即overflow失效，需通过wrapper 一个View来变向实现overflow需求
     * @param borderRadius 圆角大小，最大不超过自身尺寸的一半（即最大半圆）。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun borderRadius(borderRadius: BorderRectRadius): IStyleAttr

    /**
     * 设置视图的边框线。
     * @param border 边框线属性。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun border(border: Border): IStyleAttr

    /**
     * 设置视图的可见性。
     * @param visibility 布尔值，表示视图是否可见。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun visibility(visibility: Boolean): IStyleAttr

    /**
     * 设置视图的透明度。
     * @param opacity 透明度值（0.0 到 1.0）。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun opacity(opacity: Float): IStyleAttr
    // endregion

    // region: interaction
    /**
     * 设置视图是否有手势能力。
     * @param touchEnable 布尔值，表示视图是否可触摸（默认为true，为false则自动禁止接收所有手势对该视图）。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun touchEnable(touchEnable: Boolean): IStyleAttr
    // endregion

    // region: animation
    /**
     * 设置视图的动画参数（旧动画接口）。
     * @param animation 动画配置。
     * @param value 绑定触发动画的响应式变量。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun animation(animation: Animation, value: Any): IStyleAttr
    // endregion

    // region: animation
    /**
     * 设置视图的动画参数（新动画接口）。
     * @param animation 动画配置。
     * @param value 绑定触发动画的响应式变量。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun animate(animation: Animation, value: Any): IStyleAttr
    // endregion

    // region: transform
    /** 设置视图的变换属性。
    * @param rotate 旋转属性，默认为 Rotate.DEFAULT。
    * @param scale 缩放属性，默认为 Scale.DEFAULT。
    * @param translate 平移属性，默认为 Translate.DEFAULT。
    * @param anchor 锚点属性，默认为 Anchor.DEFAULT。
    * @param skew 倾斜属性，默认为 Skew.DEFAULT。
    * @return 返回 IStyleAttr 接口以支持链式调用。
    */
    fun transform(
        rotate: Rotate = Rotate.DEFAULT,
        scale: Scale = Scale.DEFAULT,
        translate: Translate = Translate.DEFAULT,
        anchor: Anchor = Anchor.DEFAULT,
        skew: Skew = Skew.DEFAULT
    ): IStyleAttr

    /**
     * 设置视图的无障碍化属性，用于描述元素。
     * @param accessibility 无障碍化描述字符串。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun accessibility(accessibility: String): IStyleAttr

    /**
     *设置视图的无障碍化元素对应类型。
     *此方法用于辅助功能（如屏幕阅读器）更好地理解和描述这个视图。例如，如果视图是一个按钮，
     *可以设置其角色为AccessibilityRole.Button，这样辅助功能就会将其识别为按钮，并提供相应的交互提示。
     * @param role 视图的无障碍角色，应为AccessibilityRole枚举的一个值。
     * @return 返回当前对象，以支持链式调用。 */
    fun accessibilityRole(role: AccessibilityRole): IStyleAttr

    /**
     * 设置视图的自动暗黑模式。
     * @param enable 布尔值，表示是否启用自动暗黑模式。
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun autoDarkEnable(enable: Boolean): IStyleAttr

    /**
     * 设置该view以及其children下视图能否在TurboDisplay首屏中持续更新其属性值
     * @param enable 布尔值，表示是否持续更新其属性值到TurboDisplay首屏，默认true
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    fun turboDisplayAutoUpdateEnable(enable: Boolean): IStyleAttr

    // endregion
}

/**
 * AccessibilityRole 枚举类定义了一组无障碍角色，这些角色可以用于描述视图的用途或行为。
 *
 * 每个枚举值都对应一个字符串，这个字符串可以用于设置视图的 `accessibilityRole` 属性。
 * 这样，辅助功能（如屏幕阅读器）就可以更好地理解和描述视图。
 *
 * 注意：这些角色可能不会在所有版本的Android系统中完全支持。在某些情况下，可能需要使用其他方法（如 `contentDescription`）来提供类似的信息。
 */
enum class AccessibilityRole(val roleName: String) {
    NONE("none"),
    /** 表示视图是一个按钮 */
    BUTTON("button"),
    /** 表示视图是一个搜索框 */
    SEARCH("search"),
    /** 表示视图是一个文本 */
    TEXT("text"),
    /** 表示视图是一个图像 */
    IMAGE("image"),
    /** 表示视图是一个复选框 */
    CHECKBOX("checkbox")
}