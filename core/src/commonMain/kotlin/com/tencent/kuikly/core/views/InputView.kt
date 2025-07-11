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
import com.tencent.kuikly.core.module.FontModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

class InputView : DeclarativeBaseView<InputAttr, InputEvent>() {

    override fun createAttr(): InputAttr {
        return InputAttr()
    }

    override fun createEvent(): InputEvent {
        return InputEvent()
    }

    override fun viewName(): String {
        return ViewConst.TYPE_TEXT_FIELD
    }

    override fun createRenderView() {
        super.createRenderView()
        if (attr.autofocus) {
            focus()
        }
    }

    fun setText(text: String) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setText", text)
        }
    }

    fun getText(text: String) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("text", text)
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

class InputAttr : Attr() {

    internal var autofocus = false

    /**
     * 主动设置输入框文本内容（该赋值会替换原输入框内容）
     * @param text 新输入框文本内容
     */
    fun text(text: String): InputAttr {
        TextConst.VALUE with text
        return this
    }

    /**
     * 设置输入文本的文本样式
     * 配合TextArea的textDidChange来更改spans实现输入框富文本化
     * 注：设置新inputSpans后，光标会保持原index
     * @param spans 富文本样式
     */
    fun inputSpans(spans: InputSpans): InputAttr {
        TextConst.VALUES with spans.toJSONArray().toString()
        return this
    }

    fun fontSize(size: Any): InputAttr {
        TextConst.FONT_SIZE with size
        return this
    }

    fun fontSize(size: Float, scaleFontSizeEnable: Boolean? = null): InputAttr {
        TextConst.FONT_SIZE with FontModule.scaleFontSize(size, scaleFontSizeEnable)
        return this
    }

    fun lines(lines: Int): InputAttr {
        TextConst.LINES with lines
        return this
    }

    fun fontWeightNormal(): InputAttr {
        TextConst.FONT_WEIGHT with "400"
        return this
    }

    fun fontWeightBold(): InputAttr  {
        TextConst.FONT_WEIGHT with "700"
        return this
    }

    fun fontWeightMedium(): InputAttr  {
        TextConst.FONT_WEIGHT with "500"
        return this
    }

    fun color(color: Color) {
        TextConst.TEXT_COLOR with color.toString()
    }

    fun tintColor(color: Color) {
        TextConst.TINT_COLOR with color.toString()
    }

    fun placeholderColor(color: Color) {
        TextConst.PLACEHOLDER_COLOR with color.toString()
    }

    fun placeholder(placeholder: String) {
        TextConst.PLACEHOLDER with placeholder
    }

    fun keyboardTypePassword() {
        KEYBOARD_TYPE with "password"
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

    fun textAlignCenter(): InputAttr {
        TextConst.TEXT_ALIGN with "center"
        return this
    }

    fun textAlignLeft(): InputAttr {
        TextConst.TEXT_ALIGN with "left"
        return this
    }

    fun textAlignRight(): InputAttr {
        TextConst.TEXT_ALIGN with "right"
        return this
    }

    fun maxTextLength(maxLength: Int) {
        "maxTextLength" with maxLength
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
    fun useDpFontSizeDim(useDp: Boolean = true): InputAttr {
        TextConst.TEXT_USE_DP_FONT_SIZE_DIM with useDp.toInt()
        return this
    }

    /**
     * 仅android支持，IME输入法是否不要进入全屏模式
     * android上，横屏状态下输入框默认会进入全屏模式
     * 可使用imeNoFullscreen(true)来取消全屏显示背景
     * @param isNoFullscreen 是否不要进入全屏模式
     * @return 对象本身
     */
    fun imeNoFullscreen(isNoFullscreen: Boolean): InputAttr {
        IME_NO_FULLSCREEN with isNoFullscreen
        return this
    }

    companion object {
        const val RETURN_KEY_TYPE = "returnKeyType"
        const val KEYBOARD_TYPE = "keyboardType"
        const val IME_NO_FULLSCREEN = "imeNoFullscreen"
    }
}

data class InputParams(
    val text: String
    )
data class KeyboardParams(
    val height: Float,
    val duration: Float
)

class InputEvent : Event() {
    /**
     * 当文本发生变化时调用的方法
     * @param isSyncEdit 是否同步编辑，该值为true则可以实现同步修改输入文本不会异步更新带来的跳变
     * @param handler 处理文本变化事件的回调函数
     */
    fun textDidChange(isSyncEdit: Boolean = false, handler: InputEventHandlerFn) {
        register(TEXT_DID_CHANGE, {
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
        register(INPUT_FOCUS){
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
        register(INPUT_BLUR){
            it as JSONObject
            val text = it.optString("text")
            handler(InputParams(text))
        }
    }

    /**
     * 当用户按下return键时调用的方法
     * @param handler 处理用户按下return键事件的回调函数
     */
    fun inputReturn(handler: InputEventHandlerFn) {
        register(INPUT_RETURN){
            it as JSONObject
            val text = it.optString("text")
            handler(InputParams(text))
        }
    }

    /**
     * 当键盘高度发生变化时调用的方法
     * @param handler 处理键盘高度变化事件的回调函数
     */
    fun keyboardHeightChange(handler: (KeyboardParams) -> Unit) {
        register(KEYBOARD_HEIGHT_CHANGE){
            it as JSONObject
            val height = it.optDouble("height").toFloat()
            val duration = it.optDouble("duration").toFloat()
            handler(KeyboardParams(height, duration))
        }
    }

    /**
     * 当用户按下return键时调用的方法（与 inputReturn 方法相同）
     * @param handler 处理用户按下返回键事件的回调函数
     */
    fun onTextReturn(handler: InputEventHandlerFn) {
        register(INPUT_RETURN){
            it as JSONObject
            val text = it.optString("text")
            handler(InputParams(text))
        }
    }
    /**
     * 当文本长度超过限制时调用的方法(即输入长度超过attr#maxTextLength()属性设置的长度)
     * @param handler 处理文本长度超过限制事件的回调函数
     */
    fun textLengthBeyondLimit(handler: EventHandlerFn /* = (param: kotlin.Any?) -> kotlin.Unit */) {
        register(TEXT_LENGTH_BEYOND_LIMIT, handler)
    }

    companion object {
        const val TEXT_DID_CHANGE = "textDidChange"
        const val INPUT_FOCUS = "inputFocus"
        const val INPUT_BLUR = "inputBlur"
        const val KEYBOARD_HEIGHT_CHANGE = "keyboardHeightChange"
        const val TEXT_LENGTH_BEYOND_LIMIT = "textLengthBeyondLimit"
        const val INPUT_RETURN = "inputReturn"
    }
}

fun ViewContainer<*, *>.Input(init: InputView.() -> Unit) {
    addChild(InputView(), init)
}

typealias InputEventHandlerFn = (InputParams) -> Unit
