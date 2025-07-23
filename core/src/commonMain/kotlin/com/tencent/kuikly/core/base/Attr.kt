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

package com.tencent.kuikly.core.base

import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.base.attr.ILayoutAttr
import com.tencent.kuikly.core.base.attr.IStyleAttr
import com.tencent.kuikly.core.collection.toFastMap
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexLayout
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.FlexPositionType
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.StyleSpace
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.layout.valueEquals
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.utils.ConvertUtil
import kotlin.math.max

open class Attr : Props(), IStyleAttr, ILayoutAttr {
    var flexNode: FlexNode? = null
    var keepAlive: Boolean = false
    internal var isStaticAttr = true
    private var animationMap: HashMap<String, Animation>? = null
    internal var isBeginApplyAttrProperty = false
    internal var propSetByFrameTasks: LinkedHashMap<String, FrameTask>? = null

    override fun viewDidRemove() {
        super.viewDidRemove()
        flexNode = null
        animationMap?.clear()
        propSetByFrameTasks?.clear()
        getPager().animationManager?.clearAnimations(nativeRef)
    }

    fun beginApplyAttrProperty() {
        // 尝试标记执行动画
        animationMap?.also {
            val currentObservablePropertyKey = PagerManager.getCurrentReactiveObserver().currentChangingPropertyKey ?: ""
            it[currentObservablePropertyKey]?.also { animation ->
                // 设置propertyAnimation
                isBeginApplyAttrProperty = true
                setProp(StyleConst.ANIMATION, animation.toString())
            }
        }

        getPager().animationManager?.willBeginAnimation(nativeRef) {
            it?.run {
                isBeginApplyAttrProperty = true
                setProp(StyleConst.ANIMATION, toString())
            }
        }
    }

    fun endApplyAttrProperty() {
        if (isBeginApplyAttrProperty) {
            isBeginApplyAttrProperty = false
            if (flexNode?.isDirty == true) {
                getPager().onLayoutView()
            }
            setProp(StyleConst.ANIMATION, "")
            getPager().animationManager?.didEndAnimation(nativeRef)
        }
    }
    open fun onViewLayoutFrameDidChanged(view: DeclarativeBaseView<*, *>) {
        if (propSetByFrameTasks != null) {
            val frame = view.flexNode.layoutFrame
            propSetByFrameTasks?.toFastMap()?.forEach {
                it.value.invoke(frame)
            }
        }
    }
    
    /**
     *  设置一个属性任务，当节点的布局结果存在或者/变化时回调改属性任务
     *  @param taskKey 指定属性任务的唯一key，一般为属性名，如"transform"
     *  @param frameTask frame变化回调任务（注：当已经存在frame布局结果时，会立即回调）
     */
    fun setPropByFrameTask(taskKey: String, frameTask: FrameTask) {
        if (flexNode?.layoutFrame?.isDefaultValue() == false) {
            frameTask(flexNode?.layoutFrame ?: Frame.zero)
        }
        if (propSetByFrameTasks == null) {
            propSetByFrameTasks = linkedMapOf()
        }
        propSetByFrameTasks?.set(taskKey, frameTask)
    }
    /**
     * 删除propFrame的任务
     * @param taskKey 指定属性任务的唯一key
     */
    fun removePropFrameTask(taskKey: String) {
        propSetByFrameTasks?.remove(taskKey)
    }

    // region: style
    fun size(
        width: Float, /* = Float.undefined */
        height: Float /* = Float.undefined */
    ): Attr {
        if (!width.valueEquals(Float.undefined)) width(width = width)
        if (!height.valueEquals(Float.undefined)) height(height = height)
        return this
    }

    override fun width(width: Float): Attr {
        flexNode?.styleWidth = width
        return this
    }

    override fun height(height: Float): Attr {
        flexNode?.styleHeight = height
        return this
    }

    fun maxSize(
        maxWidth: Float, /* = Float.undefined */
        maxHeight: Float /* = Float.undefined */
    ): Attr {
        if (!maxWidth.valueEquals(Float.undefined)) maxWidth(maxWidth = maxWidth)
        if (!maxHeight.valueEquals(Float.undefined)) maxHeight(maxHeight = maxHeight)
        return this
    }

    override fun maxWidth(maxWidth: Float): Attr {
        flexNode?.styleMaxWidth = maxWidth
        return this
    }

    override fun maxHeight(maxHeight: Float): Attr {
        flexNode?.styleMaxHeight = maxHeight
        return this
    }

    fun minSize(
        minWidth: Float, /* = Float.undefined */
        minHeight: Float /* = Float.undefined */
    ): Attr {
        if (!minWidth.valueEquals(Float.undefined)) minWidth(minWidth = minWidth)
        if (!minHeight.valueEquals(Float.undefined)) minHeight(minHeight = minHeight)
        return this
    }

    override fun minWidth(minWidth: Float): Attr {
        flexNode?.styleMinWidth = minWidth
        return this
    }

    override fun minHeight(minHeight: Float): Attr {
        flexNode?.styleMinHeight = minHeight
        return this
    }

    override fun zIndex(zIndex: Int, useOutline: Boolean): Attr {
        StyleConst.ZINDEX with zIndex
        if ((!useOutline || getProp(StyleConst.USE_OUTLINE) != null) && pagerData.isAndroid) {
            StyleConst.USE_OUTLINE with useOutline
        }
        return this
    }

    override fun flex(flex: Float): Attr {
        flexNode?.flex = flex
        return this
    }

    fun backgroundColor(hexColor: Long): Attr {
        backgroundColor(Color(hexColor))
        return this
    }

    override fun backgroundColor(color: Color): Attr {
        StyleConst.BACKGROUND_COLOR with color.toString()
        return this
    }

    override fun backgroundLinearGradient(
        direction: Direction,
        vararg colorStops: ColorStop
    ): Attr {
        var cssLinearGradient = "linear-gradient(${direction.ordinal}"
        for (color in colorStops) {
            cssLinearGradient += ",$color"
        }
        cssLinearGradient += ")"
        StyleConst.BACKGROUND_IMAGE with cssLinearGradient
        return this
    }

    override fun boxShadow(boxShadow: BoxShadow): Attr {
        StyleConst.BOX_SHADOW with boxShadow.toString()
        return this
    }
    /**
     * 这个方法用于设置盒子阴影属性。
     * @param boxShadow 定义盒子阴影特性的 {@link BoxShadow} 对象。
     * @param useShadowPath 一个布尔值，表示是否使用显式阴影路径(默认值为 false)。
     * 这是特定于 iOS 的，可以用于优化阴影渲染性能，一般情况下建议设置为true，
     * @return 当前 {@link Attr} 实例。
     * */
    fun boxShadow(boxShadow: BoxShadow, useShadowPath: Boolean = false): Attr {
        if (useShadowPath) {
            StyleConst.USE_SHADOW_PATH with useShadowPath.toInt()
        }
        StyleConst.BOX_SHADOW with boxShadow.toString()
        return this
    }


    override fun borderRadius(borderRadius: BorderRectRadius): Attr {
        StyleConst.BORDER_RADIUS with borderRadius.toString()
        return this
    }

    fun alignContent(flexAlign: FlexAlign): Attr {
        flexNode?.alignContent = flexAlign
        return this
    }

    fun borderRadius(
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float
    ): Attr {
        borderRadius(
            BorderRectRadius(
                topLeftCornerRadius = topLeft,
                topRightCornerRadius = topRight,
                bottomLeftCornerRadius = bottomLeft,
                bottomRightCornerRadius = bottomRight
            )
        )
        return this
    }

    fun borderRadius(allBorderRadius: Float): Attr {
        borderRadius(
            BorderRectRadius(
                allBorderRadius,
                allBorderRadius,
                allBorderRadius,
                allBorderRadius
            )
        )
        return this
    }

    override fun border(border: Border): Attr {
        StyleConst.BORDER with border.toString()
        return this
    }

    override fun visibility(visibility: Boolean): Attr {
        StyleConst.VISIBILITY with visibility.toInt()
        return this
    }

    override fun opacity(opacity: Float): Attr {
        StyleConst.OPACITY with opacity
        return this
    }

    override fun touchEnable(touchEnable: Boolean): Attr {
        StyleConst.TOUCH_ENABLE with touchEnable.toInt()
        return this
    }

    override fun animation(animation: Animation, value: Any): Attr {
        if (animationMap == null) {
            animationMap = hashMapOf()
        }
        val observablePropertyKey = PagerManager.getCurrentReactiveObserver().currentObservablePropertyKey ?: ""
        if (observablePropertyKey.isEmpty()) {
            return this
        }
        animationMap!![observablePropertyKey] = animation
        return this
    }

    override fun animate(animation: Animation, value: Any): IStyleAttr {
        val observablePropertyKey = PagerManager.getCurrentReactiveObserver().currentObservablePropertyKey ?: ""
        if (observablePropertyKey.isEmpty()) {
            return this
        }
        // 如果有旧的动画的情况下，不支持新接口
        if (animationMap?.get(observablePropertyKey) != null) {
            throwRuntimeError("不支持新旧动画接口同时存在（animation、animate），请统一切换到新的动画接口")
            return this
        }

        if (getPager().animationManager == null) {
            getPager().animationManager = AnimationManager()
        }
        getPager().animationManager?.setAnimation(observablePropertyKey, nativeRef, animation, flexNode?.isDirty?:false)
        return this
    }

    fun transform(rotate: Rotate): Attr {
        return transform(
            rotate = rotate,
            scale = Scale.DEFAULT,
            translate = Translate.DEFAULT,
            anchor = Anchor.DEFAULT
        )
    }

    fun transform(scale: Scale): Attr {
        return transform(
            rotate = Rotate.DEFAULT,
            scale = scale,
            translate = Translate.DEFAULT,
            anchor = Anchor.DEFAULT
        )
    }

    // 位移
    fun transform(translate: Translate): Attr {
        return transform(
            rotate = Rotate.DEFAULT,
            scale = Scale.DEFAULT,
            translate = translate,
            anchor = Anchor.DEFAULT
        )
    }
    // 倾斜
    fun transform(skew: Skew): Attr {
        return transform(
            rotate = Rotate.DEFAULT,
            scale = Scale.DEFAULT,
            translate = Translate.DEFAULT,
            anchor = Anchor.DEFAULT,
            skew = skew
        )
    }

    override fun transform(
        rotate: Rotate, /* = Rotate.DEFAULT */
        scale: Scale, /* = Scale.DEFAULT */
        translate: Translate, /* = Translate.DEFAULT */
        anchor: Anchor, /* = Anchor.DEFAULT */
        skew: Skew /* = Skew.DEFAULT */
    ): Attr {
        if (translate.offsetX != 0f || translate.offsetY != 0f) {
            setPropByFrameTask(StyleConst.TRANSFORM) {
                val percentageX = translate.percentageX + ConvertUtil.toIntegerPxOfDpValue(translate.offsetX) / max(ConvertUtil.toIntegerPxOfDpValue(it.width), 0.01f)
                val percentageY = translate.percentageY + ConvertUtil.toIntegerPxOfDpValue(translate.offsetY) / max(ConvertUtil.toIntegerPxOfDpValue(it.height), 0.01f)
                val translateOffset = Translate(percentageX, percentageY)
                StyleConst.TRANSFORM with "$rotate|$scale|$translateOffset|$anchor|$skew|${rotate.toRotateXYString()}"
            }
        } else {
            removePropFrameTask(StyleConst.TRANSFORM)
            StyleConst.TRANSFORM with "$rotate|$scale|$translate|$anchor|$skew|${rotate.toRotateXYString()}"
        }
        return this
    }

    // 无障碍化，作用于元素的描述
    override fun accessibility(accessibility: String): IStyleAttr {
        StyleConst.ACCESSIBILITY with accessibility
        return this
    }
    /**
     *设置视图的无障碍化元素对应类型。
     *此方法用于辅助功能（如屏幕阅读器）更好地理解和描述这个视图。例如，如果视图是一个按钮，
     *可以设置其角色为AccessibilityRole.Button，这样辅助功能就会将其识别为按钮，并提供相应的交互提示。
     * @param role 视图的无障碍角色，应为AccessibilityRole枚举的一个值。
     * @return 返回当前对象，以支持链式调用。 */
    override fun accessibilityRole(role: AccessibilityRole): IStyleAttr {
        StyleConst.ACCESSIBILITY_ROLE with role.roleName
        return this
    }

    // 自动暗黑模式
    /**
     * true（默认值）:
     *      iOS: 对应overrideUserInterfaceStyle设置为UIUserInterfaceStyleUnspecified
     *      Android: 对应setForceDarkAllowed设置为ture
     * false:
     *      iOS: 对应overrideUserInterfaceStyle设置为UIUserInterfaceStyleLight
     *      Android: 对应setForceDarkAllowed设置为false
     */
    override fun autoDarkEnable(enable: Boolean): IStyleAttr {
        StyleConst.AUTO_DARK_ENABLE with enable.toInt()
        return this
    }
    /**
     * 设置该view以及其children下视图能否在TurboDisplay首屏中持续更新其属性值
     * @param enable 布尔值，表示是否持续更新其属性值到TurboDisplay首屏，默认true
     * @return 返回 IStyleAttr 接口以支持链式调用。
     */
    override fun turboDisplayAutoUpdateEnable(enable: Boolean): IStyleAttr {
        StyleConst.TURBO_DISPLAY_AUTO_UPDATE_ENABLE with enable.toInt()
        return this
    }
    // endregion

    override fun top(top: Float): Attr {
        flexNode?.setStylePosition(FlexLayout.PositionType.POSITION_TOP, top)
        return this
    }

    override fun left(left: Float): Attr {
        flexNode?.setStylePosition(FlexLayout.PositionType.POSITION_LEFT, left)
        return this
    }

    override fun bottom(bottom: Float): Attr {
        flexNode?.setStylePosition(FlexLayout.PositionType.POSITION_BOTTOM, bottom)
        return this
    }

    override fun right(right: Float): Attr {
        flexNode?.setStylePosition(FlexLayout.PositionType.POSITION_RIGHT, right)
        return this
    }

    override fun positionType(positionType: FlexPositionType): Attr {
        flexNode?.positionType = positionType
        return this
    }

    fun top(percentage: Percentage) {
        flexNode?.markDisable()
        getPager().addTaskWhenPagerUpdateLayoutFinish {
            flexNode?.markEnable()
            flexNode?.parent?.layoutFrame?.height?.also {
                top(it * percentage.toFloat())
            }
        }
    }

    fun left(percentage: Percentage) {
        flexNode?.markDisable()
        getPager().addTaskWhenPagerUpdateLayoutFinish {
            flexNode?.markEnable()
            flexNode?.parent?.layoutFrame?.width?.also {
                left(it * percentage.toFloat())
            }
        }
    }

    fun right(percentage: Percentage) {
        flexNode?.markDisable()
        getPager().addTaskWhenPagerUpdateLayoutFinish {
            flexNode?.markEnable()
            flexNode?.parent?.layoutFrame?.width?.also {
                right(it * percentage.toFloat())
            }
        }
    }

    fun bottom(percentage: Percentage) {
        flexNode?.markDisable()
        getPager().addTaskWhenPagerUpdateLayoutFinish {
            flexNode?.markEnable()
            flexNode?.parent?.layoutFrame?.height?.also {
                bottom(it * percentage.toFloat())
            }
        }
    }

    fun positionAbsolute(): Attr {
        positionType(FlexPositionType.ABSOLUTE)
        return this
    }

    fun positionRelative(): Attr {
        positionType(FlexPositionType.RELATIVE)
        return this
    }

    fun absolutePositionAllZero() {
        absolutePosition(0f, 0f, 0f, 0f)
    }

    fun absolutePosition(
        top: Float = Float.undefined,
        left: Float = Float.undefined,
        bottom: Float = Float.undefined,
        right: Float = Float.undefined
    ): Attr {
        positionAbsolute()
        if (!top.valueEquals(Float.undefined)) top(top = top)
        if (!left.valueEquals(Float.undefined)) left(left = left)
        if (!bottom.valueEquals(Float.undefined)) bottom(bottom = bottom)
        if (!right.valueEquals(Float.undefined)) right(right = right)
        return this
    }

    override fun alignSelf(alignSelf: FlexAlign): Attr {
        flexNode?.alignSelf = alignSelf
        return this
    }

    fun alignSelfCenter(): Attr {
        alignSelf(FlexAlign.CENTER)
        return this
    }

    fun alignSelfFlexStart(): Attr {
        alignSelf(FlexAlign.FLEX_START)
        return this
    }

    fun alignSelfFlexEnd(): Attr {
        alignSelf(FlexAlign.FLEX_END)
        return this
    }

    fun alignSelfStretch(): Attr {
        alignSelf(FlexAlign.STRETCH)
        return this
    }

    override fun margin(
        top: Float, /* = Float.undefined */
        left: Float, /* = Float.undefined */
        bottom: Float, /* = Float.undefined */
        right: Float /* = Float.undefined */
    ): Attr {
        if (!top.valueEquals(Float.undefined)) {
            flexNode?.setMargin(StyleSpace.Type.TOP, top)
        }
        if (!left.valueEquals(Float.undefined)) {
            flexNode?.setMargin(StyleSpace.Type.LEFT, left)
        }
        if (!bottom.valueEquals(Float.undefined)) {
            flexNode?.setMargin(StyleSpace.Type.BOTTOM, bottom)
        }
        if (!right.valueEquals(Float.undefined)) {
            flexNode?.setMargin(StyleSpace.Type.RIGHT, right)
        }
        return this
    }

    fun marginTop(top: Float): Attr {
        margin(top = top)
        return this
    }

    fun marginLeft(left: Float): Attr {
        margin(left = left)
        return this
    }

    fun marginBottom(bottom: Float): Attr {
        margin(bottom = bottom)
        return this
    }

    fun marginRight(right: Float): Attr {
        margin(right = right)
        return this
    }

    fun margin(all: Float): Attr {
        margin(all, all, all, all)
        return this
    }

    fun overflow(clipChild: Boolean): Attr {
        StyleConst.OVERFLOW with clipChild.toInt()
        return this
    }

    fun debugName(debugName: String): Attr {
        StyleConst.DEBUG_NAME with debugName
        return this
    }

    fun keepAlive(keepAlive: Boolean) {
        this.keepAlive = keepAlive
    }

    fun preventTouch(enable: Boolean) {
        StyleConst.PREVENT_TOUCH with enable
    }

    fun consumeTouchDown(enable: Boolean) {
        StyleConst.CONSUME_DOWN with enable
    }

    fun superTouch(enable: Boolean) {
        StyleConst.SUPER_TOUCH with enable
    }

    object StyleConst {
        const val BACKGROUND_COLOR = "backgroundColor"

        // border
        const val BORDER = "border"
        const val BORDER_WIDTH = "borderWidth"
        const val BORDER_COLOR = "borderColor"
        const val BORDER_RADIUS = "borderRadius"

        const val BOX_SHADOW = "boxShadow"
        const val USE_SHADOW_PATH = "useShadowPath"

        // view
        const val VISIBILITY = "visibility"
        const val OPACITY = "opacity"
        const val TOUCH_ENABLE = "touchEnable"
        const val TRANSFORM = "transform"
        const val OVERFLOW = "overflow"
        const val BACKGROUND_IMAGE = "backgroundImage"
        const val ANIMATION = "animation"
        const val LAZY_ANIMATION_KEY = "lazyAnimationKey"
        const val ZINDEX = "zIndex"
        const val USE_OUTLINE = "useOutline"
        const val ACCESSIBILITY = "accessibility"
        const val ACCESSIBILITY_ROLE = "accessibilityRole"
        const val WRAPPER_BOX_SHADOW_VIEW = "wrapperBoxShadowView" // only for ios
        const val AUTO_DARK_ENABLE = "autoDarkEnable"
        const val TURBO_DISPLAY_AUTO_UPDATE_ENABLE = "turboDisplayAutoUpdateEnable"
        // 设置属性用作 UI-Inspector 中的视图名称
        const val DEBUG_NAME = "debugName"
        const val PREVENT_TOUCH = "preventTouch"
        const val CONSUME_DOWN = "consumeDown"
        const val SUPER_TOUCH = "superTouch"
    }
}

enum class Direction(value: Int) {
    TO_TOP(0),
    TO_BOTTOM(1),
    TO_LEFT(2),
    TO_RIGHT(3),
    TO_TOP_LEFT(4),
    TO_TOP_RIGHT(5),
    TO_BOTTOM_LEFT(6),
    TO_BOTTOM_RIGHT(7),
}

class ColorStop(val color: Color, val stopIn01: Float) {
    override fun toString(): String {
        return "$color $stopIn01"
    }
}

class BorderRectRadius(
    private val topLeftCornerRadius: Float,
    private val topRightCornerRadius: Float,
    private val bottomLeftCornerRadius: Float,
    private val bottomRightCornerRadius: Float
) {
    override fun toString(): String {
        return "$topLeftCornerRadius,$topRightCornerRadius,$bottomLeftCornerRadius,$bottomRightCornerRadius"
    }
}

enum class BorderStyle(val value: String) {
    SOLID("solid"),
    DOTTED("dotted"),
    DASHED("dashed"),
}

class Border(
    val lineWidth: Float,
    val lineStyle: BorderStyle,
    val color: Color
) {
    override fun toString(): String {
        return "$lineWidth ${lineStyle.value} $color"
    }
}

class BoxShadow(
    private val offsetX: Float,
    private val offsetY: Float,
    private val shadowRadius: Float,
    private val shadowColor: Color
) {
    override fun toString(): String {
        return "$offsetX $offsetY $shadowRadius $shadowColor"
    }
}

/**
 * 旋转类
 * 支持2d和3d旋转
 */
class Rotate(
    private val angle: Float = 0f, // 围绕z轴旋转,属于2d平面旋转（range of [-360, 360]）
    private val xAngle: Float = 0f, //  围绕x轴旋转xAngle角度 属于3d旋转 （range of [-360, 360]）
    private val yAngle: Float = 0f // 围绕y轴旋转yAngle角度 属于3d旋转 （range of [-360, 360]）
) {
    // 是否为3d旋转
    val is3d: Boolean get() = xAngle != 0f || yAngle != 0f

    companion object {
        val DEFAULT: Rotate = Rotate(0f)
    }

    override fun toString(): String {
        return "$angle"
    }

    fun toRotateXYString(): String {
        return "$xAngle $yAngle"
    }

}

class Scale(
    private val x: Float,  // range of [0, max]
    private val y: Float = 1f  // range of [0, max]
) {

    companion object {
        val DEFAULT: Scale = Scale(1f, 1f)
    }

    override fun toString(): String {
        return "$x $y"
    }
}

/**
 * Translate类用于定义位移属性。
 * @property percentageX 在 X 轴上的位移百分比，范围为 [-1,1]，例如 1 = 100%。（当然也可以超过1，如：2，这样就是200%偏移）
 * @property percentageY 在 Y 轴上的位移百分比，范围为 [-1,1]，例如 1 = 100%。默认值为0。（当然也可以超过1，如：2，这样就是200%偏移）
 * @property offsetX 在基于 percentageX 的位移结果上，增加 offsetX 以相对偏移调整多少 dp 距离。默认值为0。
 * @property offsetY 在基于 percentageY 的位移结果上，增加 offsetY 以相对偏移调整多少 dp 距离。默认值为0。
 */
class Translate(
    var percentageX: Float,
    var percentageY: Float = 0f,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f
) {

    companion object {
        val DEFAULT: Translate = Translate(0f, 0f)
    }

    override fun toString(): String {
        return "$percentageX $percentageY"
    }
}

/*
 * 倾斜变换类，transform(skew()) 函数可将元素倾斜 （注：正数表示逆时针倾斜，负数表示顺时针倾斜）
  请注意，当您将元素倾斜 90 度时，元素将会变得无法看见，因为它的宽度或高度将会变为 0。
 */
class Skew(
    private val horizontalSkewAngle: Float = 0f,  // 水平方向倾斜角度(deg) range of [-360,360]，单位角度
    private val verticalSkewAngle: Float = 0f  // 垂直方向倾斜角度(deg) range of [-360,360]，单位角度
) {

    companion object {
        val DEFAULT: Skew = Skew(0f, 0f)
    }

    override fun toString(): String {
        return "$horizontalSkewAngle $verticalSkewAngle"
    }
}

class Anchor(
    private val x: Float,  // range of [0,1]
    private val y: Float  // range of [0,1]
) {

    companion object {
        val DEFAULT: Anchor = Anchor(0.5f, 0.5f)
    }

    override fun toString(): String {
        return "$x $y"
    }
}

/* 百分比单位 */
class Percentage(private val number100: Float) {
    override fun toString(): String {
        return number100.toString()
    }

    fun toFloat(): Float {
        return number100 / 100f
    }
}

/*
* 用于表示矩形边缘的内边距。它包含四个浮点值，分别表示顶部、左侧、底部和右侧的内边距。
 */
data class EdgeInsets(val top: Float, val left: Float, val bottom: Float, val right: Float) {
    override fun toString(): String {
        return "$top $left $bottom $right"
    }
    companion object {
        val default = EdgeInsets(0f, 0f, 0f, 0f)
        fun decodeWithString(string: String): EdgeInsets {
            if (string.isEmpty()) {
                return default
            }
            val splits = string.split(" ")
            if (splits.size >= 4) {
                return EdgeInsets(top = splits[0].toFloat(), left = splits[1].toFloat(), bottom = splits[2].toFloat(), right = splits[3].toFloat())
            }
            return default
        }
    }
}
