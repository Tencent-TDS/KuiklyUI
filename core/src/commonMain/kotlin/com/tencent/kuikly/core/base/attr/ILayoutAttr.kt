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

import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.FlexPositionType
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.layout.undefined

/**
 * Kuikly 框架视图节点布局接口
 * 框架布局原理和 Yoga 保持一致
 *
 * @see [Yoga_Layout](https://yogalayout.com/)
 */

/**
 * 容器布局属性接口，用于设置容器视图的布局属性。
 * 继承自 ILayoutAttr 接口，添加了容器相关的布局属性方法。
 */
interface IContainerLayoutAttr : ILayoutAttr {
    // region: container
    /**
     * 设置布局内部子节点的放置方向，同时指定了主轴(main axis)的方向。
     * @param flexDirection 放置方向，默认FlexDirection.COLUMN（例如：FlexDirection.COLUMN）。
     * @return 返回 IContainerLayoutAttr 接口以支持链式调用。
     * 此接口规则和 [Yoga_flex_direction](https://yogalayout.com/docs/flex-direction) 一致。
     */
    fun flexDirection(flexDirection: FlexDirection): IContainerLayoutAttr

    /**
     * 设置布局内部子节点在主轴上的对齐方式。
     * @param justifyContent 对齐方式，默认.FLEX_START（例如：FlexJustifyContent.CENTER）。
     * @return 返回 IContainerLayoutAttr 接口以支持链式调用。
     */
    fun justifyContent(justifyContent: FlexJustifyContent): IContainerLayoutAttr

    /**
     * 设置布局内部子节点在交叉轴上的对齐方式。
     * @param alignItems 对齐方式，默认STRETCH（例如：FlexAlign.CENTER）。
     * @return 返回 IContainerLayoutAttr 接口以支持链式调用。
     */
    fun alignItems(alignItems: FlexAlign): IContainerLayoutAttr

    /**
     * 设置布局内部子节点的换行方式。
     * @param flexWrap 换行方式（例如：FlexWrap.WRAP，默认为NOWRAP）。
     * @return 返回 IContainerLayoutAttr 接口以支持链式调用。
     */
    fun flexWrap(flexWrap: FlexWrap): IContainerLayoutAttr

    /**
     * 设置容器的内边距。
     * @param top 顶部内边距值，默认为 Float.undefined。
     * @param left 左侧内边距值，默认为 Float.undefined。
     * @param bottom 底部内边距值，默认为 Float.undefined。
     * @param right 右侧内边距值，默认为 Float.undefined。
     * @return 返回 IContainerLayoutAttr 接口以支持链式调用。
     */
    fun padding(
        top: Float = Float.undefined,
        left: Float = Float.undefined,
        bottom: Float = Float.undefined,
        right: Float = Float.undefined
    ): IContainerLayoutAttr

}

/**
 * 布局属性接口，用于设置视图的布局属性。
 * 与 Flex 布局概念一致。
 */
interface ILayoutAttr {

    /**
     * 设置元素内容区域的宽度。
     * @param width 宽度值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun width(width: Float): ILayoutAttr

    /**
     * 设置元素内容区域的高度。
     * @param height 高度值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun height(height: Float): ILayoutAttr

    /**
     * 设置内容区域元素的最大宽度。
     * @param maxWidth 最大宽度值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun maxWidth(maxWidth: Float): ILayoutAttr

    /**
     * 设置内容区域元素的最大高度。
     * @param maxHeight 最大高度值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun maxHeight(maxHeight: Float): ILayoutAttr

    /**
     * 设置内容区域元素的最小宽度。
     * @param minWidth 最小宽度值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun minWidth(minWidth: Float): ILayoutAttr

    /**
     * 设置内容区域元素的最小高度。
     * @param minHeight 最小高度值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun minHeight(minHeight: Float): ILayoutAttr

    /**
     * 设置元素的 Flex 属性。
     * @param flex Flex 值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun flex(flex: Float): ILayoutAttr

    /**
     * 设置元素的顶部位置。
     * 仅在绝对布局（positionType.ABSOLUTE）下生效。
     * @param top 顶部位置值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun top(top: Float): ILayoutAttr

    /**
     * 设置元素的左侧位置。
     * 仅在绝对布局（positionType.ABSOLUTE）下生效。
     * @param left 左侧位置值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun left(left: Float): ILayoutAttr

    /**
     * 设置元素的底部位置。
     * 仅在绝对布局（positionType.ABSOLUTE）下生效。
     * @param bottom 底部位置值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun bottom(bottom: Float): ILayoutAttr

    /**
     * 设置元素的右侧位置。
     * 仅在绝对布局（positionType.ABSOLUTE）下生效。
     * @param right 右侧位置值。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun right(right: Float): ILayoutAttr

    /**
     * 设置元素的布局类型。
     * @param positionType 布局类型（例如：FlexPositionType.ABSOLUTE）。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun positionType(positionType: FlexPositionType): ILayoutAttr

    /**
     * 设置元素的自对齐属性。
     * @param alignSelf 自对齐属性（例如：FlexAlign.CENTER）。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun alignSelf(alignSelf: FlexAlign): ILayoutAttr

    /**
     * 设置元素的外边距。
     * @param top 顶部外边距值，默认为 Float.undefined。
     * @param left 左侧外边距值，默认为 Float.undefined。
     * @param bottom 底部外边距值，默认为 Float.undefined。
     * @param right 右侧外边距值，默认为 Float.undefined。
     * @return 返回 ILayoutAttr 接口以支持链式调用。
     */
    fun margin(
        top: Float = Float.undefined,
        left: Float = Float.undefined,
        bottom: Float = Float.undefined,
        right: Float = Float.undefined
    ): ILayoutAttr
}
