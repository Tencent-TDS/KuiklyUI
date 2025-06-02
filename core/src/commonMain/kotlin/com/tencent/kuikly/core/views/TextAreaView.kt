/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.module.FontModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

class TextAreaView : DeclarativeBaseView<TextAreaAttr, TextAreaEvent>() {

    override fun willInit() {
        super.willInit()
        getViewAttr().fontSize(15)
    }
    override fun createAttr(): TextAreaAttr {
        return TextAreaAttr()
    }

    override fun createEvent(): TextAreaEvent {
        return TextAreaEvent()
    }

    override fun viewName(): String {
        return ViewConst.TYPE_TEXT_AREA
    }

    override fun createRenderView() {
        super.createRenderView()
        if (attr.autofocus) {
            focus()
        }
    }

    @Deprecated("Use TextAreaAttr#text() instead",
        ReplaceWith("TextAreaAttr { attr{ text() }")
    )
    fun setText(text: String) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setText", text)
        }
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

}

class TextAreaAttr : Attr() {

    internal var autofocus = false

    fun text(text: String): TextAreaAttr {
        TextConst.VALUE with text
        return this
    }

    /**
     * 设置输入文本的文本样式
     * 配合TextArea的textDidChange来更改spans实现输入框富文本化
     * 注：设置新inputSpans后，光标会保持原index
     * @param spans 富文本样式
     */
    fun inputSpans(spans: InputSpans): TextAreaAttr {
        TextConst.VALUES with spans.toJSONArray().toString()
        return this
    }

    fun fontSize(size: Any): TextAreaAttr {
        TextConst.FONT_SIZE with size
        return this
    }

    fun fontSize(size: Float, scaleFontSizeEnable: Boolean? = null): TextAreaAttr {
        TextConst.FONT_SIZE with FontModule.scaleFontSize(size, scaleFontSizeEnable)
        return this
    }

    fun lines(lines: Int): TextAreaAttr {
        TextConst.LINES with lines
        return this
    }

    fun fontWeightNormal(): TextAreaAttr {
        TextConst.FONT_WEIGHT with "400"
        return this
    }

    fun fontWeightBold(): TextAreaAttr  {
        TextConst.FONT_WEIGHT with "700"
        return this
    }

    fun fontWeightMedium(): TextAreaAttr  {
        TextConst.FONT_WEIGHT with "500"
        return this
    }

    fun textAlignCenter(): TextAreaAttr {
        TextConst.TEXT_ALIGN with "center"
        return this
    }

    fun textAlignLeft(): TextAreaAttr {
        TextConst.TEXT_ALIGN with "left"
        return this
    }

    fun textAlignRight(): TextAreaAttr {
        TextConst.TEXT_ALIGN with "right"
        return this
    }

    fun color(color: Color): TextAreaAttr {
        TextConst.TEXT_COLOR with color.toString()
        return this
    }

    fun tintColor(color: Color): TextAreaAttr {
        TextConst.TINT_COLOR with color.toString()
        return this
    }

    fun placeholderColor(color: Color): TextAreaAttr {
        "placeholderColor" with color.toString()
        return this
    }

    fun placeholder(placeholder: String): TextAreaAttr {
        "placeholder" with placeholder
        return this
    }

    fun maxTextLength(maxLength: Int) {
        "maxTextLength" with maxLength
    }

    fun keyboardTypePassword(): TextAreaAttr {
        KEYBOARD_TYPE with "password"
        return this
    }

    fun keyboardTypeNumber() {
        KEYBOARD_TYPE with "number"
    }

    fun keyboardTypeEmail() {
        KEYBOARD_TYPE with "email"
    }

    fun returnKeyTypeSearch() {
        RETURN_KEY_TYPE with "search"
    }

    fun returnKeyTypeSend() {
        RETURN_KEY_TYPE with "send"
    }

    fun returnKeyTypeDone() {
        RETURN_KEY_TYPE with "done"
    }

    fun returnKeyTypeNext() {
        RETURN_KEY_TYPE with "next"
    }

    fun returnKeyTypeContinue() {
        RETURN_KEY_TYPE with "continue"
    }

    fun returnKeyTypeGo() {
        RETURN_KEY_TYPE with "go"
    }

    fun returnKeyTypeGoogle() {
        RETURN_KEY_TYPE with "google"
    }

    fun autofocus(focus: Boolean) {
        autofocus = focus
    }

    fun editable(editable: Boolean) {
        "editable" with editable.toInt()
    }

    /**
     * 是否使用dp作为字体单位
     * android上，字体默认是映射到sp, 如果不想字体跟随系统的字体大小，
     * 可指定文本使用useDpFontSizeDim(true)来表示不跟随系统字体大小
     * @param useDp 是否使用dp单位作为字体大小单位
     * @return 对象本身
     */
    fun useDpFontSizeDim(useDp: Boolean = true): TextAreaAttr {
        TextConst.TEXT_USE_DP_FONT_SIZE_DIM with useDp.toInt()
        return this
    }

    companion object {
        const val RETURN_KEY_TYPE = "returnKeyType"
        const val KEYBOARD_TYPE = "keyboardType"
    }
}

class InputSpans {

    private val spans = fastArrayListOf<InputSpan>()

    fun addSpan(span: InputSpan): InputSpans {
        spans.add(span)
        return this
    }

    fun removeSpan(span: InputSpan): InputSpans {
        spans.remove(span)
        return this
    }

    fun removeSpanAt(index: Int): InputSpans {
        if (index < 0 || index >= spans.size) {
            return this
        }
        spans.removeAt(index)
        return this
    }

    fun clear(): InputSpans {
        spans.clear()
        return this
    }

    internal fun toJSONArray(): JSONArray {
        val values = JSONArray()
        spans.forEach {
            values.put(it.toJSON())
        }
        return values
    }
}

class InputSpan {
    private val propMap = JSONObject()

    fun text(text: String): InputSpan {
        propMap.put(TextConst.VALUE, text)
        return this
    }

    fun fontSize(size: Any): InputSpan {
        propMap.put(TextConst.FONT_SIZE, size)
        return this
    }

    fun color(color: Color): InputSpan {
        propMap.put(TextConst.TEXT_COLOR, color.toString())
        return this
    }

    fun fontWeightNormal(): InputSpan {
        propMap.put(TextConst.FONT_WEIGHT, "400")
        return this
    }

    fun fontWeightMedium(): InputSpan  {
        propMap.put(TextConst.FONT_WEIGHT, "500")
        return this
    }

    fun fontWeightBold(): InputSpan  {
        propMap.put(TextConst.FONT_WEIGHT, "700")
        return this
    }

    internal fun toJSON(): JSONObject {
        return propMap
    }
}

class TextAreaEvent : Event() {

    /**
     * 当文本发生变化时调用的方法
     * @param isSyncEdit 是否同步编辑，该值为true则可以实现同步修改输入文本不会异步更新带来的跳变
     * @param handler 处理文本变化事件的回调函数
     */
    fun textDidChange(isSyncEdit: Boolean = false, handler: InputEventHandlerFn) {
        this.register(TEXT_DID_CHANGE, {
            it as JSONObject
            val text = it.optString("text")
            handler(InputParams(text))
        }, isSync = isSyncEdit)
    }

    /**
     * 当输入框获得焦点时调用的方法
     * @param handler 处理输入框获得焦点事件的回调函数
     */
    fun inputFocus(handler: InputEventHandlerFn) {
        this.register(INPUT_FOCUS){
            it as JSONObject
            val text = it.optString("text")
            handler(InputParams(text))
        }
    }
    /**
     * 当输入框失去焦点时调用的方法
     * @param handler 处理输入框失去焦点事件的回调函数
     */
    fun inputBlur(handler: InputEventHandlerFn) {
        this.register(INPUT_BLUR){
            it as JSONObject
            val text = it.optString("text")
            handler(InputParams(text))
        }
    }

    /**
     * 当键盘高度发生变化时调用的方法
     * @param handler 处理键盘高度变化事件的回调函数
     */
    fun keyboardHeightChange(handler: (KeyboardParams) -> Unit ) {
        this.register(KEYBOARD_HEIGHT_CHANGE){
            it as JSONObject
            val height = it.optDouble("height").toFloat()
            val duration = it.optDouble("duration").toFloat()
            handler(KeyboardParams(height, duration))
        }
    }
    /**
     * 当文本长度超过限制时调用的方法(即输入长度超过attr.maxTextLength属性设置的长度)
     * @param handler 处理文本长度超过限制事件的回调函数
     */
    fun textLengthBeyondLimit(handler: EventHandlerFn /* = (parma: kotlin.Any?) -> kotlin.Unit */) {
        this.register(TEXT_LENGTH_BEYOND_LIMIT,handler)
    }

    companion object {
        const val TEXT_DID_CHANGE = "textDidChange"
        const val INPUT_FOCUS = "inputFocus"
        const val INPUT_BLUR = "inputBlur"
        const val KEYBOARD_HEIGHT_CHANGE = "keyboardHeightChange"
        const val TEXT_LENGTH_BEYOND_LIMIT = "textLengthBeyondLimit"
    }
}

fun ViewContainer<*, *>.TextArea(init: TextAreaView.() -> Unit) {
    addChild(TextAreaView(), init)
}
