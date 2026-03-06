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

    fun returnKeyTypeNone() {
        RETURN_KEY_TYPE with "none"
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

    fun returnKeyTypePrevious() {
        RETURN_KEY_TYPE with "previous"
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

    @Deprecated(
        "Use maxTextLength(length: Int, type: LengthLimitType) instead",
        ReplaceWith("maxTextLength(maxLength, LengthLimitType)")
    )
    fun maxTextLength(maxLength: Int) {
        "maxTextLength" with maxLength
    }

    fun maxTextLength(length: Int, type: LengthLimitType) {
        "lengthLimitType" with type.value
        "maxTextLength" with length
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

    /**
     * 仅iOS支持
     * 当设置为true的时候，输入框中如果是空的，则软键盘的Return Key会自动置灰禁用，非空的时候自动启用。
     */
    fun enablesReturnKeyAutomatically(flag: Boolean): InputAttr {
        ENABLES_RETURN_KEY_AUTOMATICALLY with if( flag ) 1 else 0
        return this
    }

    /**
     * 是否启用拼音输入回调
     * @param enable 是否启用，默认为false
     */
    fun enablePinyinCallback(enable: Boolean = false): InputAttr {
        "enablePinyinCallback" with (if (enable) 1 else 0)
        return this
    }

    /**
     * 设置是否在点击 IME 动作按钮（如 Send/Go/Search）时自动收起键盘
     *
     * @param autoHide 是否自动收起键盘，默认为 false
     *                 - true: 点击 Send 等按钮后自动收起键盘
     *                 - false: 点击 Send 等按钮后保持键盘打开，由业务自己控制
     */
    fun autoHideKeyboardOnImeAction(enable: Boolean): InputAttr {
        TextConst.AUTO_HIDE_KEYBOARD_ON_IME_ACTION with (if (enable) 1 else 0)
        return this
    }

    companion object {
        const val RETURN_KEY_TYPE = "returnKeyType"
        const val KEYBOARD_TYPE = "keyboardType"
        const val IME_NO_FULLSCREEN = "imeNoFullscreen"
        const val ENABLES_RETURN_KEY_AUTOMATICALLY =  "enablesReturnKeyAutomatically"
    }
}

data class InputParams(
    val text: String,
    val imeAction: String? = null,
    val length: Int? = null
)

data class KeyboardParams(
    val height: Float,
    val duration: Float,
    val curve: Int = 0
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
            val length = if (it.has("length")) it.optInt("length") else null
            handler(InputParams(text, length = length))
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
            val imeAction = it.optString("ime_action").ifEmpty {
                getView()?.getViewAttr()?.getProp(InputAttr.RETURN_KEY_TYPE) as? String ?: ""
            }
            handler(InputParams(text, imeAction))
        }
    }

    /**
     * Called when keyboard height changes.
     * @param isSync Sync callback to ensure UI animation syncs with keyboard, default true
     * @param handler Callback handler with keyboard params
     */
    fun keyboardHeightChange(isSync: Boolean = true, handler: (KeyboardParams) -> Unit) {
        register(KEYBOARD_HEIGHT_CHANGE, {
            it as JSONObject
            val height = it.optDouble("height").toFloat()
            val duration = it.optDouble("duration").toFloat()
            val curve = it.optInt("curve")
            handler(KeyboardParams(height, duration, curve))
        }, isSync = isSync)
    }

    /**
     * 当用户按下return键时调用的方法（与 inputReturn 方法相同）
     * @param handler 处理用户按下返回键事件的回调函数
     */
    @Deprecated("Use inputReturn instead", ReplaceWith("inputReturn(handler)"))
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

/**
 * 输入长度限制类型
 *
 * | 示例       | BYTE | CHARACTER | VISUAL_WIDTH | 说明                                  |
 * |----------|------|-----------|--------------|-------------------------------------|
 * | `""`       | 0    | 0         | 0            | 空字符串：0                              |
 * | `"a"`      | 1    | 1         | 1            | 英文：UTF8字节数1，字符个数1，视觉宽度1             |
 * | `"中"`      | 3    | 1         | 2            | 中文：UTF8字节数3，字符个数1，视觉宽度2             |
 * | `"😂"`     | 4    | 1         | 2            | Emoji：UTF8字节数4，字符个数1，视觉宽度2          |
 * | `"[img]"` | 5    | 1         | 2            | ImageSpan：描述文本的UTF8字节数5，字符个数1，视觉宽度2 |
 * | `"\u200B"` | 3    | 1         | 1            | 不可见字符：UTF8字节数3，字符个数1，视觉宽度按1计算       |
 *
 * > 注：VISUAL_WIDTH模式下，未识别出来的不可见字符可能会被统计为2
 */
enum class LengthLimitType(val value: Int) {
    /** 限制输入的长度按字节计算 */
    BYTE(0),
    /** 限制输入的长度按字符计算 */
    CHARACTER(1),
    /** 限制输入的长度按视觉宽度计算 */
    VISUAL_WIDTH(2)
}