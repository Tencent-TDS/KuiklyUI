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
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.Size
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.FlexPositionType
import com.tencent.kuikly.core.layout.MeasureFunction
import com.tencent.kuikly.core.layout.MeasureOutput
import com.tencent.kuikly.core.layout.isUndefined
import com.tencent.kuikly.core.views.shadow.TextShadow

class AutoHeightTextAreaView(val singleLine: Boolean = false) :
    DeclarativeBaseView<TextAreaAttr, TextAreaEvent>(), MeasureFunction {
    var shadow: TextShadow? = null


    override fun createEvent(): TextAreaEvent {
        return TextAreaEvent()
    }

    override fun createAttr(): TextAreaAttr {
        return TextAreaAttr()
    }

    override fun willInit() {
        super.willInit()
        getViewAttr().fontSize(15f)
        shadow = TextShadow(pagerId, nativeRef, ViewConst.TYPE_RICH_TEXT)
    }

    override fun viewName(): String {
        if (singleLine) {
            return ViewConst.TYPE_TEXT_FIELD
        } else {
            return ViewConst.TYPE_TEXT_AREA
        }
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        flexNode.measureFunction = null
        shadow?.removeFromParentComponent()
        shadow = null
    }

    override fun createFlexNode() {
        super.createFlexNode()
        flexNode.measureFunction = this
    }

    private fun remeasureText(text: String) {
        val ctx = this
        ctx.shadow?.setProp("text", text)
        ctx.flexNode.markDirty()
    }

    override fun didSetProp(propKey: String, propValue: Any) {
        super.didSetProp(propKey, propValue)
        if (isShadowProp(propKey)) {
            shadow?.setProp(propKey, propValue)
            flexNode.markDirty()
        }
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

    fun focus() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("focus", "")
        }
    }

    fun blur() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("blur", "")
        }
    }

    /**
     * 获取光标当前位置
     * @param callback 结果回调
     */
    fun cursorIndex(callback: (cursorIndex: Int) -> Unit) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("getCursorIndex", "") {
                val index = it?.optInt("cursorIndex") ?: -1
                callback(index)
            }
        }
    }

    /**
     * 设置当前光标位置
     * @param index 光标位置
     */
    fun setCursorIndex(index: Int) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setCursorIndex", index.toString())
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
        val flex = flexNode.flex != 0f
        val stretch =
            flexNode.alignSelf == FlexAlign.STRETCH || (flexNode.alignSelf == FlexAlign.AUTO && flexNode.parent?.alignItems == FlexAlign.STRETCH)
        if ((flex || stretch) && flexNode.positionType == FlexPositionType.RELATIVE) {
            size = flexLayoutSize(size, width, height, flex, stretch)
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
        measureOutput.width = size?.width ?: 0f
        measureOutput.height = size?.height ?: 0f
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

    private fun flexLayoutSize(
        measureOutputSize: Size,
        fitWidth: Float,
        fitHeight: Float,
        flex: Boolean,
        stretch: Boolean
    ): Size {
        val flexDirection = flexNode.parent?.flexDirection
        var outWidth = measureOutputSize.width
        var outHeight = measureOutputSize.height
        if (flexDirection == FlexDirection.ROW || flexDirection == FlexDirection.ROW_REVERSE) {
            if (flex && !fitWidth.isUndefined() && outWidth < fitWidth) {
                outWidth = fitWidth
            }
            if (stretch && !fitHeight.isUndefined()) {
                outHeight = fitHeight
            }
        } else {
            if (flex && !fitHeight.isUndefined() && outHeight < fitHeight) {
                outHeight = fitHeight
            }
            if (stretch && !fitWidth.isUndefined()) {
                outWidth = fitWidth
            }
        }
        return Size(outWidth, outHeight)
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

    fun onTextChanged(text: String) {
        remeasureText(text)
    }

}