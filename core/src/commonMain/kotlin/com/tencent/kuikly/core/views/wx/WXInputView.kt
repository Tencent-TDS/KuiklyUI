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

package com.tencent.kuikly.core.views.wx

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * WeChat mini-program native `input` component type values.
 */
object WXInputType {
    const val TEXT = "text"
    const val NUMBER = "number"
    const val IDCARD = "idcard"
    const val DIGIT = "digit"
    const val SAFE_PASSWORD = "safe-password"
    const val NICKNAME = "nickname"
}

/**
 * WeChat mini-program native `input` component confirm-type values.
 */
object WXInputConfirmType {
    const val SEND = "send"
    const val SEARCH = "search"
    const val NEXT = "next"
    const val GO = "go"
    const val DONE = "done"
}

/**
 * Cross-platform Compose-style wrapper of the WeChat mini-program native `input` component.
 *
 * On mini-program platform (when `pageData.params.is_miniprogram == "1"`), it renders the
 * native `<input/>` (via `KRWXInputView`). On other platforms it falls back to a plain view
 * so native-only capabilities are silently ignored.
 */
class WXInputView : ComposeView<WXInputAttr, WXInputEvent>() {

    override fun createEvent(): WXInputEvent {
        return WXInputEvent()
    }

    override fun createAttr(): WXInputAttr {
        return WXInputAttr()
    }

    override fun viewName(): String {
        val pageData = getPager().pageData
        if (pageData.params.optString(IS_MINI_PROGRAM) == "1") {
            return VIEW_NAME_MINI_PROGRAM
        }
        return ViewConst.TYPE_VIEW
    }

    override fun body(): ViewBuilder {
        return {
            attr {
                // Leave empty body; the native input renders itself.
            }
        }
    }

    companion object {
        internal const val IS_MINI_PROGRAM = "is_miniprogram"
        internal const val VIEW_NAME_MINI_PROGRAM = "KRWXInputView"
    }
}

/**
 * Attributes for [WXInputView].
 *
 * Mirrors the native WeChat mini-program `input` component attributes.
 */
class WXInputAttr : ComposeAttr() {

    /**
     * Input current value.
     */
    fun value(v: String): WXInputAttr {
        PROP_VALUE with v
        return this
    }

    /**
     * Input type, see [WXInputType] for candidate values.
     */
    fun type(v: String): WXInputAttr {
        PROP_TYPE with v
        return this
    }

    /**
     * Whether the input is a password field.
     */
    fun password(v: Boolean): WXInputAttr {
        PROP_PASSWORD with v
        return this
    }

    /**
     * Placeholder text shown when value is empty.
     */
    fun placeholder(v: String): WXInputAttr {
        PROP_PLACEHOLDER with v
        return this
    }

    /**
     * Inline style string applied to the placeholder.
     */
    fun placeholderStyle(v: String): WXInputAttr {
        PROP_PLACEHOLDER_STYLE with v
        return this
    }

    /**
     * CSS class applied to the placeholder.
     */
    fun placeholderClass(v: String): WXInputAttr {
        PROP_PLACEHOLDER_CLASS with v
        return this
    }

    /**
     * Whether the input is disabled.
     */
    fun disabled(v: Boolean): WXInputAttr {
        PROP_DISABLED with v
        return this
    }

    /**
     * Maximum number of characters, `-1` means no limit.
     */
    fun maxLength(v: Int): WXInputAttr {
        PROP_MAX_LENGTH with v
        return this
    }

    /**
     * Whether to auto focus the input.
     */
    fun focus(v: Boolean): WXInputAttr {
        PROP_FOCUS with v
        return this
    }

    /**
     * Text of the "done" button on the soft keyboard. See [WXInputConfirmType].
     */
    fun confirmType(v: String): WXInputAttr {
        PROP_CONFIRM_TYPE with v
        return this
    }

    /**
     * Whether to keep the keyboard open after "done" is pressed.
     */
    fun confirmHold(v: Boolean): WXInputAttr {
        PROP_CONFIRM_HOLD with v
        return this
    }

    /**
     * Whether to adjust the page when keyboard is up.
     */
    fun adjustPosition(v: Boolean): WXInputAttr {
        PROP_ADJUST_POSITION with v
        return this
    }

    /**
     * Whether to hold the keyboard even if user taps outside the input.
     */
    fun holdKeyboard(v: Boolean): WXInputAttr {
        PROP_HOLD_KEYBOARD with v
        return this
    }

    /**
     * Distance between cursor and keyboard.
     */
    fun cursorSpacing(v: Int): WXInputAttr {
        PROP_CURSOR_SPACING with v
        return this
    }

    /**
     * Cursor position.
     */
    fun cursor(v: Int): WXInputAttr {
        PROP_CURSOR with v
        return this
    }

    /**
     * Selection start index.
     */
    fun selectionStart(v: Int): WXInputAttr {
        PROP_SELECTION_START with v
        return this
    }

    /**
     * Selection end index.
     */
    fun selectionEnd(v: Int): WXInputAttr {
        PROP_SELECTION_END with v
        return this
    }

    /**
     * Input name used by the form.
     */
    fun name(v: String): WXInputAttr {
        PROP_NAME with v
        return this
    }

    companion object {
        internal const val PROP_VALUE = "value"
        internal const val PROP_TYPE = "type"
        internal const val PROP_PASSWORD = "password"
        internal const val PROP_PLACEHOLDER = "placeholder"
        internal const val PROP_PLACEHOLDER_STYLE = "placeholderStyle"
        internal const val PROP_PLACEHOLDER_CLASS = "placeholderClass"
        internal const val PROP_DISABLED = "disabled"
        internal const val PROP_MAX_LENGTH = "maxLength"
        internal const val PROP_FOCUS = "focus"
        internal const val PROP_CONFIRM_TYPE = "confirmType"
        internal const val PROP_CONFIRM_HOLD = "confirmHold"
        internal const val PROP_ADJUST_POSITION = "adjustPosition"
        internal const val PROP_HOLD_KEYBOARD = "holdKeyboard"
        internal const val PROP_CURSOR_SPACING = "cursorSpacing"
        internal const val PROP_CURSOR = "cursor"
        internal const val PROP_SELECTION_START = "selectionStart"
        internal const val PROP_SELECTION_END = "selectionEnd"
        internal const val PROP_NAME = "name"
    }
}

/**
 * Events for [WXInputView].
 *
 * Every callback receives a [JSONObject] with a `data` key carrying the JSON-stringified
 * native `detail` payload. For `onInput`/`onConfirm`/`onFocus`/`onBlur` the payload typically
 * contains at least `value` and `cursor`.
 */
class WXInputEvent : ComposeEvent() {

    /**
     * Called when the input value changes.
     */
    fun onInput(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_INPUT, handler)
    }

    /**
     * Called when the input gets focused.
     */
    fun onFocus(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_FOCUS, handler)
    }

    /**
     * Called when the input loses focus.
     */
    fun onBlur(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_BLUR, handler)
    }

    /**
     * Called when "done" is pressed on the soft keyboard.
     */
    fun onConfirm(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_CONFIRM, handler)
    }

    /**
     * Called when keyboard height changes.
     */
    fun onKeyboardHeightChange(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_KEYBOARD_HEIGHT_CHANGE, handler)
    }

    /**
     * Called when WeChat reviews the user-entered nickname (used by `type="nickname"`).
     */
    fun onNickNameReview(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_NICKNAME_REVIEW, handler)
    }

    private fun registerJsonCallback(eventName: String, handler: (JSONObject) -> Unit) {
        register(eventName) {
            handler(it as JSONObject)
        }
    }

    companion object {
        internal const val CALLBACK_INPUT = "inputCallback"
        internal const val CALLBACK_FOCUS = "focusCallback"
        internal const val CALLBACK_BLUR = "blurCallback"
        internal const val CALLBACK_CONFIRM = "confirmCallback"
        internal const val CALLBACK_KEYBOARD_HEIGHT_CHANGE = "keyboardHeightChangeCallback"
        internal const val CALLBACK_NICKNAME_REVIEW = "nickNameReviewCallback"
    }
}

/**
 * DSL builder for [WXInputView].
 */
fun ViewContainer<*, *>.WXInput(init: WXInputView.() -> Unit) {
    addChild(WXInputView(), init)
}
