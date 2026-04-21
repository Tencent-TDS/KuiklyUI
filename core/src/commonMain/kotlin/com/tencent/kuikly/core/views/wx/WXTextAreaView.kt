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
 * WeChat mini-program native `textarea` component confirm-type values.
 */
object WXTextAreaConfirmType {
    const val RETURN = "return"
    const val SEND = "send"
    const val SEARCH = "search"
    const val NEXT = "next"
    const val GO = "go"
    const val DONE = "done"
}

/**
 * Cross-platform Compose-style wrapper of the WeChat mini-program native `textarea` component.
 *
 * On mini-program platform (`pageData.params.is_miniprogram == "1"`) it renders the native
 * `<textarea/>` via `KRWXTextAreaView`; on other platforms it falls back to a plain view.
 */
class WXTextAreaView : ComposeView<WXTextAreaAttr, WXTextAreaEvent>() {

    override fun createEvent(): WXTextAreaEvent = WXTextAreaEvent()

    override fun createAttr(): WXTextAreaAttr = WXTextAreaAttr()

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
                // Leave empty body; the native textarea renders itself.
            }
        }
    }

    companion object {
        internal const val IS_MINI_PROGRAM = "is_miniprogram"
        internal const val VIEW_NAME_MINI_PROGRAM = "KRWXTextAreaView"
    }
}

/**
 * Attributes for [WXTextAreaView]. Mirrors native `textarea` attributes.
 */
class WXTextAreaAttr : ComposeAttr() {

    /** Current value. */
    fun value(v: String): WXTextAreaAttr { PROP_VALUE with v; return this }

    /** Placeholder text shown when value is empty. */
    fun placeholder(v: String): WXTextAreaAttr { PROP_PLACEHOLDER with v; return this }

    /** Inline style string applied to the placeholder. */
    fun placeholderStyle(v: String): WXTextAreaAttr { PROP_PLACEHOLDER_STYLE with v; return this }

    /** CSS class applied to the placeholder. */
    fun placeholderClass(v: String): WXTextAreaAttr { PROP_PLACEHOLDER_CLASS with v; return this }

    /** Whether the textarea is disabled. */
    fun disabled(v: Boolean): WXTextAreaAttr { PROP_DISABLED with v; return this }

    /** Maximum length of the input value. `-1` means no limit. */
    fun maxLength(v: Int): WXTextAreaAttr { PROP_MAX_LENGTH with v; return this }

    /** Whether to auto focus on mount. */
    fun autoFocus(v: Boolean): WXTextAreaAttr { PROP_AUTO_FOCUS with v; return this }

    /** Whether to keep focus. */
    fun focus(v: Boolean): WXTextAreaAttr { PROP_FOCUS with v; return this }

    /** Whether auto height adjusts by content. */
    fun autoHeight(v: Boolean): WXTextAreaAttr { PROP_AUTO_HEIGHT with v; return this }

    /** Whether the textarea is position-fixed (must be true if placed inside a position-fixed layer). */
    fun fixed(v: Boolean): WXTextAreaAttr { PROP_FIXED with v; return this }

    /** Distance between cursor and keyboard. */
    fun cursorSpacing(v: Int): WXTextAreaAttr { PROP_CURSOR_SPACING with v; return this }

    /** Cursor position. */
    fun cursor(v: Int): WXTextAreaAttr { PROP_CURSOR with v; return this }

    /** Whether to show the confirm bar above the keyboard. */
    fun showConfirmBar(v: Boolean): WXTextAreaAttr { PROP_SHOW_CONFIRM_BAR with v; return this }

    /** Selection start index. */
    fun selectionStart(v: Int): WXTextAreaAttr { PROP_SELECTION_START with v; return this }

    /** Selection end index. */
    fun selectionEnd(v: Int): WXTextAreaAttr { PROP_SELECTION_END with v; return this }

    /** Whether to adjust page position when keyboard is up. */
    fun adjustPosition(v: Boolean): WXTextAreaAttr { PROP_ADJUST_POSITION with v; return this }

    /** Whether to hold the keyboard after tapping elsewhere. */
    fun holdKeyboard(v: Boolean): WXTextAreaAttr { PROP_HOLD_KEYBOARD with v; return this }

    /** Whether to disable default padding inside the textarea. */
    fun disableDefaultPadding(v: Boolean): WXTextAreaAttr { PROP_DISABLE_DEFAULT_PADDING with v; return this }

    /** Text of the "done" button on the soft keyboard. See [WXTextAreaConfirmType]. */
    fun confirmType(v: String): WXTextAreaAttr { PROP_CONFIRM_TYPE with v; return this }

    /** Whether to keep the keyboard open after "done" is pressed. */
    fun confirmHold(v: Boolean): WXTextAreaAttr { PROP_CONFIRM_HOLD with v; return this }

    /** Name used by the form. */
    fun name(v: String): WXTextAreaAttr { PROP_NAME with v; return this }

    companion object {
        internal const val PROP_VALUE = "value"
        internal const val PROP_PLACEHOLDER = "placeholder"
        internal const val PROP_PLACEHOLDER_STYLE = "placeholderStyle"
        internal const val PROP_PLACEHOLDER_CLASS = "placeholderClass"
        internal const val PROP_DISABLED = "disabled"
        internal const val PROP_MAX_LENGTH = "maxLength"
        internal const val PROP_AUTO_FOCUS = "autoFocus"
        internal const val PROP_FOCUS = "focus"
        internal const val PROP_AUTO_HEIGHT = "autoHeight"
        internal const val PROP_FIXED = "fixed"
        internal const val PROP_CURSOR_SPACING = "cursorSpacing"
        internal const val PROP_CURSOR = "cursor"
        internal const val PROP_SHOW_CONFIRM_BAR = "showConfirmBar"
        internal const val PROP_SELECTION_START = "selectionStart"
        internal const val PROP_SELECTION_END = "selectionEnd"
        internal const val PROP_ADJUST_POSITION = "adjustPosition"
        internal const val PROP_HOLD_KEYBOARD = "holdKeyboard"
        internal const val PROP_DISABLE_DEFAULT_PADDING = "disableDefaultPadding"
        internal const val PROP_CONFIRM_TYPE = "confirmType"
        internal const val PROP_CONFIRM_HOLD = "confirmHold"
        internal const val PROP_NAME = "name"
    }
}

/**
 * Events for [WXTextAreaView]. Callback receives a [JSONObject] whose `data` is the
 * JSON-serialized native `detail`.
 */
class WXTextAreaEvent : ComposeEvent() {

    fun onInput(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_INPUT, handler)
    fun onFocus(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_FOCUS, handler)
    fun onBlur(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_BLUR, handler)
    fun onConfirm(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_CONFIRM, handler)
    fun onLineChange(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_LINE_CHANGE, handler)
    fun onKeyboardHeightChange(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_KEYBOARD_HEIGHT_CHANGE, handler)

    private fun registerJsonCallback(eventName: String, handler: (JSONObject) -> Unit) {
        register(eventName) { handler(it as JSONObject) }
    }

    companion object {
        internal const val CALLBACK_INPUT = "inputCallback"
        internal const val CALLBACK_FOCUS = "focusCallback"
        internal const val CALLBACK_BLUR = "blurCallback"
        internal const val CALLBACK_CONFIRM = "confirmCallback"
        internal const val CALLBACK_LINE_CHANGE = "lineChangeCallback"
        internal const val CALLBACK_KEYBOARD_HEIGHT_CHANGE = "keyboardHeightChangeCallback"
    }
}

/**
 * DSL builder for [WXTextAreaView].
 */
fun ViewContainer<*, *>.WXTextArea(init: WXTextAreaView.() -> Unit) {
    addChild(WXTextAreaView(), init)
}
