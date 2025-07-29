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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.Size
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.base.toInt
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.FlexPositionType
import com.tencent.kuikly.core.layout.MeasureFunction
import com.tencent.kuikly.core.layout.MeasureOutput
import com.tencent.kuikly.core.layout.isUndefined
import com.tencent.kuikly.core.module.FontModule
import com.tencent.kuikly.core.views.shadow.TextShadow

open class TextView : DeclarativeBaseView<TextAttr, TextEvent>(), MeasureFunction {

    var shadow: TextShadow? = null
    private var didLayout = false

    override fun willInit() {
        super.willInit()
        shadow = TextShadow(pagerId, nativeRef, viewName())
    }

    override fun didSetProp(propKey: String, propValue: Any) {
        if (canSyncToRenderView(propKey)) {
            super.didSetProp(propKey, propValue)
        }
        if (isShadowProp(propKey)) {
            shadow?.setProp(propKey, propValue)
            flexNode.markDirty()
        }
    }

    internal fun markDirty(){
        flexNode.markDirty()
        shadow?.markDirty()
    }

    override fun syncProp(propKey: String, propValue: Any) {
        if (canSyncToRenderView(propKey)) {
            super.syncProp(propKey, propValue)
        }
    }

    override fun createFlexNode() {
        super.createFlexNode()
        flexNode.measureFunction = this
    }

    override fun createRenderView() {
        super.createRenderView()
        if (didLayout) {
            renderView?.setShadow()
        }
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        flexNode.measureFunction = null
        shadow?.removeFromParentComponent()
        shadow = null
    }

    override fun createAttr(): TextAttr {
        return TextAttr()
    }

    override fun createEvent(): TextEvent {
        return TextEvent()
    }

    override fun viewName(): String {
        return if (attr.didSetTextGradient) {
            ViewConst.TYPE_GRADIENT_RICH_TEXT
        } else {
            ViewConst.TYPE_RICH_TEXT
        }
    }

    fun updateShadow() {
        if (shadow?.calculateFromCache != true) {
            renderView?.setShadow()
        }
    }

    override fun measure(
        node: FlexNode,
        width: Float,
        height: Float,
        measureOutput: MeasureOutput
    ) {
        val cHeight = measureHeightToFloat(height)
        val cWidth = measureWidthToFloat(width)
        var size = shadow?.calculateRenderViewSize(cWidth, cHeight) ?: Size(0f, 0f)
        if (flexNode.flex != 0f && flexNode.positionType == FlexPositionType.RELATIVE) {
            size = flexLayoutSize(size ?: Size(0f, 0f), width, height)
        }
        if (!flexNode.styleWidth.isUndefined()) {
            size = Size(flexNode.styleWidth, size.height)
        }
        if (!flexNode.styleMinWidth.isUndefined()) {
            size = widthLayoutSize(size, flexNode.styleMinWidth, height)
        }
        if (!flexNode.styleHeight.isUndefined()) {
            size = Size(size.width, flexNode.styleHeight)
        }
        if (!flexNode.styleMinHeight.isUndefined()) {
            size = heightLayoutSize(size, width, flexNode.styleMinHeight)
        }
        didLayout = true

        updateShadow()
        measureOutput.width = size?.width ?: 0f
        measureOutput.height = size?.height ?: 0f
        tryFireLineBreakMarginEvent()
    }

    private fun tryFireLineBreakMarginEvent() {
        if (attr.getProp(TextConst.LINE_BREAK_MARGIN) != null) {
            getPager().addTaskWhenPagerDidCalculateLayout {
                val isLineBreakMargin = shadow?.callMethod(TextConst.SHADOW_METHOD_IS_LINE_BREAK_MARGIN, "") == "1"
                if (isLineBreakMargin) {
                    event.handler?.invoke(null)
                }
            }
        }
    }

    private fun canSyncToRenderView(propKey: String): Boolean {
        if (propKey == TextConst.VALUE
            || propKey == TextConst.VALUES
            || propKey == TextConst.TEXT_COLOR
            || propKey == TextConst.FONT_SIZE
        ) {
            return false
        }
        return true
    }

    private fun isShadowProp(propKey: String): Boolean {
        if (propKey == Attr.StyleConst.TRANSFORM
            || propKey == Attr.StyleConst.OPACITY
            || propKey == Attr.StyleConst.VISIBILITY
            || propKey == Attr.StyleConst.BACKGROUND_COLOR
        ) {
            return false
        }
        return true
    }

    private fun measureHeightToFloat(height: Float): Float {
        return if (height.isUndefined()) {
            -1f
        } else {
            height
        }
    }

    private fun measureWidthToFloat(width: Float): Float {
        return if (width.isUndefined()) {
            100000f
        } else {
            width
        }
    }

    private fun widthLayoutSize(measureOutputSize: Size, fitWidth: Float, fitHeight: Float): Size {
        if (fitWidth.isUndefined()) {
            return measureOutputSize
        }
        var outWidth = measureOutputSize.width
        if (outWidth < fitWidth) outWidth = fitWidth
        return Size(outWidth, measureOutputSize.height)
    }

    private fun heightLayoutSize(measureOutputSize: Size, fitWidth: Float, fitHeight: Float): Size {
        if (fitHeight.isUndefined()) {
            return measureOutputSize
        }
        var outputHeight = measureOutputSize.height
        if (outputHeight < fitHeight) outputHeight = fitHeight
        return Size(measureOutputSize.width, outputHeight)
    }

    private fun flexLayoutSize(measureOutputSize: Size, fitWidth: Float, fitHeight: Float): Size {
        val flexDirection = flexNode.parent?.flexDirection
        if (flexDirection == FlexDirection.ROW || flexDirection == FlexDirection.ROW_REVERSE) {
            if (fitWidth.isUndefined()) {
                return measureOutputSize
            }
            var outWidth = measureOutputSize.width
            if (outWidth < fitWidth) outWidth = fitWidth
            return Size(outWidth, measureOutputSize.height)
        } else {
            if (fitHeight.isUndefined()) {
                return measureOutputSize
            }
            var outHeight = measureOutputSize.height
            if (outHeight < fitHeight) outHeight = fitHeight
            return Size(measureOutputSize.width, outHeight)

        }
    }

}

open class TextAttr : Attr() {
    internal var didSetTextGradient = false
    internal var letterSpacing: Float? = null
    open fun value(value: String): TextAttr {
        TextConst.VALUE with value
        return this
    }

    open fun text(text: String): TextAttr {
        TextConst.VALUE with text
        return this
    }

    open fun color(color: Long): TextAttr {
        TextConst.TEXT_COLOR with Color(color).toString()
        return this
    }

    open fun color(color: Color): TextAttr {
        TextConst.TEXT_COLOR with color.toString()
        return this
    }

    /**
     * 设置字体大小
     * @param size 字体大小
     * @param scaleFontSizeEnable 是否允许端侧随宿主字体模式缩放，默认根据Pager.scaleFontSizeEnable() 返回值决定（业务可通过Pager重写该方式统一页面生效）
     */
    open fun fontSize(size: Float, scaleFontSizeEnable: Boolean? = null): TextAttr {
        TextConst.FONT_SIZE with (FontModule.scaleFontSize(size, scaleFontSizeEnable))
        letterSpacing?.also {
            letterSpacing(it)
        }
        return this
    }

    /**
     * 仅iOS、鸿蒙支持ExtraLight字重
     */
    open fun fontWeightExtraLight(): TextAttr {
        TextConst.FONT_WEIGHT with "200"
        return this
    }

    /**
     * 仅iOS、鸿蒙支持Light字重
     */
    open fun fontWeightLight(): TextAttr {
        TextConst.FONT_WEIGHT with "300"
        return this
    }

    open fun fontWeightNormal(): TextAttr {
        TextConst.FONT_WEIGHT with FontWeight.NORMAL.value
        return this
    }

    open fun fontWeightBold(): TextAttr {
        TextConst.FONT_WEIGHT with FontWeight.BOLD.value
        return this
    }

    open fun fontWeightMedium(): TextAttr {
        TextConst.FONT_WEIGHT with FontWeight.MEDIUM.value
        return this
    }

    open fun fontWeightSemiBold(): TextAttr {
        TextConst.FONT_WEIGHT with FontWeight.SEMISOLID.value
        return this
    }

    @Deprecated("use fontWeightSemiBold() instead", replaceWith = ReplaceWith("fontWeightSemiBold()"))
    open fun fontWeightSemisolid(): TextAttr {
        return fontWeightSemiBold()
    }

    open fun fontWeight400(): TextAttr {
        return fontWeightNormal()
    }

    open fun fontWeight500(): TextAttr {
        return fontWeightMedium()
    }

    open fun fontWeight600(): TextAttr {
        return fontWeightSemisolid()
    }

    open fun fontWeight700(): TextAttr {
        return fontWeightBold()
    }

    open fun fontFamily(fontFamily: String): TextAttr {
        TextConst.FONT_FAMILY with fontFamily
        return this
    }

    open fun lines(lines: Int): TextAttr {
        TextConst.LINES with lines
        return this
    }

    /**
     * 最后一行折叠"..." 距离最右边距离，常用于显示"更多"展开场景使用
     * @param lineBreakMargin 最后一行折叠距离text frame.right距离
     */
    open fun lineBreakMargin(lineBreakMargin: Float): TextAttr {
        TextConst.LINE_BREAK_MARGIN with lineBreakMargin
        return this
    }

    open fun textOverFlowTail(): TextAttr {
        TextConst.TEXT_OVERFLOW with "tail"
        return this
    }

    open fun textOverFlowClip(): TextAttr {
        TextConst.TEXT_OVERFLOW with "clip"
        return this
    }

    open fun textOverFlowMiddle(): TextAttr {
        TextConst.TEXT_OVERFLOW with "middle"
        return this
    }

    open fun textOverFlowWordWrapping(): TextAttr {
        TextConst.TEXT_OVERFLOW with "wordWrapping"
        return this
    }

    open fun textDecorationUnderLine(): TextAttr {
        TextConst.TEXT_DECORATION with "underline"
        return this
    }

    open fun textDecorationLineThrough(): TextAttr {
        TextConst.TEXT_DECORATION with "line-through"
        return this
    }

    open fun textAlignCenter(): TextAttr {
        TextConst.TEXT_ALIGN with TextAlign.CENTER.value
        return this
    }

    open fun textAlignLeft(): TextAttr {
        TextConst.TEXT_ALIGN with TextAlign.LEFT.value
        return this
    }

    open fun textAlignRight(): TextAttr {
        TextConst.TEXT_ALIGN with TextAlign.RIGHT.value
        return this
    }

    open fun lineHeight(lineHeight: Float): TextAttr {
        TextConst.LINE_HEIGHT with lineHeight
        return this
    }

    open fun lineSpacing(value: Float): TextAttr {
        TextConst.LINE_SPACING with value
        return this
    }

    open fun letterSpacing(value: Float): TextAttr {
        var spacing = value
        if (pagerData.isAndroid && pagerData.nativeBuild < 4) { // 安卓版本最低支持判断
            val fontSize = getProp(TextConst.FONT_SIZE) as? Float
            val dp2PxScale = 3f // 低版本未能直接拿到屏幕PPI比例，这里使用平均值兼容
            if (fontSize != null && fontSize > 0f) {
                spacing = (value / fontSize) / dp2PxScale
            }
        }
        letterSpacing = value
        TextConst.LETTER_SPACING with spacing
        return this
    }

    open fun paragraphSpacing(value: Float): TextAttr {
        TextConst.PARAGRAPH_SPACING with value
        return this
    }

    open fun fontStyleNormal(): TextAttr {
        TextConst.FONT_STYLE with FontStyle.NORMAL.value
        return this
    }

    open fun fontStyleItalic(): TextAttr {
        TextConst.FONT_STYLE with FontStyle.ITALIC.value
        return this
    }

    /**
     * 首行缩进距离
     * @param headIndent 缩进距离
     */
    open fun firstLineHeadIndent(headIndent: Float): TextAttr {
        TextConst.HEAD_INDENT with headIndent
        return this
    }

    /**
     * 文本后置处理器, 供业务扩展使用, 端可根据processorName来标识怎么后置处理文本
     * 比如: 文本支持 emoji
     */
    open fun textPostProcessor(processorName: String): TextAttr {
        TextConst.TEXT_POST_PROCESSOR with processorName
        return this
    }

    /**
     * 是否使用dp作为字体单位
     * android上，字体默认是映射到sp, 如果不想字体跟随系统的字体大小，
     * 可指定文本使用useDpFontSizeDim(true)来表示不跟随系统字体大小
     * @param useDp 是否使用dp单位作为字体大小单位
     * @return 对象本身
     */
    open fun useDpFontSizeDim(useDp: Boolean = true): TextAttr {
        TextConst.TEXT_USE_DP_FONT_SIZE_DIM with useDp.toInt()
        return this
    }

    /**
     * 字体阴影
     */
    open fun textShadow(offsetX: Float, offsetY: Float, radius: Float, color: Color): TextAttr {
        TextConst.TEXT_SHADOW with BoxShadow(offsetX, offsetY, radius, color).toString()
        return this
    }

    /**
     * 设置文字描边颜色和宽度
     * @param color 文字描边颜色
     * @param color 文字描边宽度
     */
    open fun textStroke(color: Color, width: Float = 2f): TextAttr  {
        TextConst.STROKE_COLOR with color.toString()
        TextConst.STROKE_WIDTH with width
        return this
    }

    /**
     * 文本渐变
     * @param direction 渐变方向
     * @param colorStops 渐变颜色和位置参数
     */
    override fun backgroundLinearGradient(
        direction: Direction,
        vararg colorStops: ColorStop
    ): Attr {
        didSetTextGradient = true
        return super.backgroundLinearGradient(direction, *colorStops)
    }

}

open class TextEvent : Event() {

    internal var handler: EventHandlerFn? = null

    /**
     * 监听是否触发了LineBreakMargin
     * @param handler 事件处理器
     */
    open fun onLineBreakMargin(handler: EventHandlerFn) {
        this.handler = handler
    }
}

object TextConst {
    const val VIEW_NAME = "TextView"
    const val VALUE = "text"
    const val VALUES = "values"
    const val FONT_SIZE = "fontSize"
    const val FONT_WEIGHT = "fontWeight"
    const val FONT_STYLE = "fontStyle"
    const val FONT_FAMILY = "fontFamily"
    const val TEXT_OVERFLOW = "lineBreakMode"
    const val TEXT_DECORATION = "textDecoration"
    const val TEXT_COLOR = "color"
    const val TINT_COLOR = "tintColor"
    const val LINES = "numberOfLines"
    const val LINE_SPACING = "lineSpacing"
    const val LETTER_SPACING = "letterSpacing"
    const val LINE_HEIGHT = "lineHeight"
    const val PARAGRAPH_SPACING = "paragraphSpacing"
    const val TEXT_ALIGN = "textAlign"
    const val LINE_BREAK_MARGIN = "lineBreakMargin"
    const val HEAD_INDENT = "headIndent"
    const val TEXT_SHADOW = "textShadow"
    const val STROKE_COLOR = "strokeColor"
    const val STROKE_WIDTH = "strokeWidth"
    const val TEXT_POST_PROCESSOR = "textPostProcessor"
    const val TEXT_USE_DP_FONT_SIZE_DIM = "useDpFontSizeDim"

    const val SHADOW_METHOD_IS_LINE_BREAK_MARGIN = "isLineBreakMargin"
    const val PLACEHOLDER = "placeholder"
    const val PLACEHOLDER_COLOR = "placeholderColor"
}

enum class TextAlign(val value: String) {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right")
}

enum class FontStyle(val value: String) {
    NORMAL("normal"),
    ITALIC("italic")
}

enum class FontWeight(val value: String) {
    NORMAL("400"),
    MEDIUM("500"),
    SEMISOLID("600"),
    BOLD("700")
}

fun ViewContainer<*, *>.Text(init: TextView.() -> Unit) {
    val textView = createViewFromRegister(ViewConst.TYPE_TEXT_CLASS_NAME) as? TextView
    if (textView != null) { // 存在自定义扩展
        addChild(textView, init)
    } else {
        addChild(TextView(), init)
    }
}