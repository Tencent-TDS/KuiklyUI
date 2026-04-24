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

package com.tencent.kuikly.core.render.web.runtime.miniapp.expand.components.wx

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.EventHandler
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.wx.MiniWXTextAreaViewElement
import org.w3c.dom.Element

/**
 * Render view export for the WeChat mini-program native `textarea` component.
 *
 * Delegates property setting and event forwarding to [MiniWXTextAreaViewElement].
 */
class KRWXTextAreaView : IKuiklyRenderViewExport {
    private val textareaElement = MiniWXTextAreaViewElement()

    override val ele: Element
        get() = textareaElement.unsafeCast<Element>()

    // Event callbacks
    private var inputCallback: KuiklyRenderCallback? = null
    private var focusCallback: KuiklyRenderCallback? = null
    private var blurCallback: KuiklyRenderCallback? = null
    private var confirmCallback: KuiklyRenderCallback? = null
    private var lineChangeCallback: KuiklyRenderCallback? = null
    private var keyboardHeightChangeCallback: KuiklyRenderCallback? = null

    init {
        textareaElement.addEventListener(EVENT_INPUT, createEventForwarder { inputCallback })
        textareaElement.addEventListener(EVENT_FOCUS, createEventForwarder { focusCallback })
        textareaElement.addEventListener(EVENT_BLUR, createEventForwarder { blurCallback })
        textareaElement.addEventListener(EVENT_CONFIRM, createEventForwarder { confirmCallback })
        textareaElement.addEventListener(EVENT_LINE_CHANGE, createEventForwarder { lineChangeCallback })
        textareaElement.addEventListener(
            EVENT_KEYBOARD_HEIGHT_CHANGE,
            createEventForwarder { keyboardHeightChangeCallback }
        )
    }

    private fun createEventForwarder(callbackSupplier: () -> KuiklyRenderCallback?): EventHandler {
        return { event: dynamic ->
            val data: dynamic = JSON.stringify(event.detail)
            callbackSupplier()?.invoke(mapOf<String, Any>(KEY_DATA to data.unsafeCast<String>()))
        }
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_VALUE -> { textareaElement.value = propValue as String; true }
            PROP_PLACEHOLDER -> { textareaElement.placeholder = propValue as String; true }
            PROP_PLACEHOLDER_STYLE -> { textareaElement.placeholderStyle = propValue as String; true }
            PROP_PLACEHOLDER_CLASS -> { textareaElement.placeholderClass = propValue as String; true }
            PROP_DISABLED -> { textareaElement.disabled = toBoolean(propValue); true }
            PROP_MAX_LENGTH -> { textareaElement.maxLength = toInt(propValue); true }
            PROP_AUTO_FOCUS -> { textareaElement.autoFocus = toBoolean(propValue); true }
            PROP_FOCUS -> { textareaElement.focus = toBoolean(propValue); true }
            PROP_AUTO_HEIGHT -> { textareaElement.autoHeight = toBoolean(propValue); true }
            PROP_FIXED -> { textareaElement.fixed = toBoolean(propValue); true }
            PROP_CURSOR_SPACING -> { textareaElement.cursorSpacing = toInt(propValue); true }
            PROP_CURSOR -> { textareaElement.cursor = toInt(propValue); true }
            PROP_SHOW_CONFIRM_BAR -> { textareaElement.showConfirmBar = toBoolean(propValue); true }
            PROP_SELECTION_START -> { textareaElement.selectionStart = toInt(propValue); true }
            PROP_SELECTION_END -> { textareaElement.selectionEnd = toInt(propValue); true }
            PROP_ADJUST_POSITION -> { textareaElement.adjustPosition = toBoolean(propValue); true }
            PROP_HOLD_KEYBOARD -> { textareaElement.holdKeyboard = toBoolean(propValue); true }
            PROP_DISABLE_DEFAULT_PADDING -> { textareaElement.disableDefaultPadding = toBoolean(propValue); true }
            PROP_CONFIRM_TYPE -> { textareaElement.confirmType = propValue as String; true }
            PROP_CONFIRM_HOLD -> { textareaElement.confirmHold = toBoolean(propValue); true }
            PROP_NAME -> { textareaElement.name = propValue as String; true }
            CALLBACK_INPUT -> { inputCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_FOCUS -> { focusCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_BLUR -> { blurCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_CONFIRM -> { confirmCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_LINE_CHANGE -> { lineChangeCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_KEYBOARD_HEIGHT_CHANGE -> {
                keyboardHeightChangeCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun toBoolean(value: Any): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value == "1" || value.equals("true", ignoreCase = true)
        else -> false
    }

    private fun toInt(value: Any): Int = when (value) {
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: 0
        else -> 0
    }

    companion object {
        const val VIEW_NAME = "KRWXTextAreaView"

        // Props
        const val PROP_VALUE = "value"
        const val PROP_PLACEHOLDER = "placeholder"
        const val PROP_PLACEHOLDER_STYLE = "placeholderStyle"
        const val PROP_PLACEHOLDER_CLASS = "placeholderClass"
        const val PROP_DISABLED = "disabled"
        const val PROP_MAX_LENGTH = "maxLength"
        const val PROP_AUTO_FOCUS = "autoFocus"
        const val PROP_FOCUS = "focus"
        const val PROP_AUTO_HEIGHT = "autoHeight"
        const val PROP_FIXED = "fixed"
        const val PROP_CURSOR_SPACING = "cursorSpacing"
        const val PROP_CURSOR = "cursor"
        const val PROP_SHOW_CONFIRM_BAR = "showConfirmBar"
        const val PROP_SELECTION_START = "selectionStart"
        const val PROP_SELECTION_END = "selectionEnd"
        const val PROP_ADJUST_POSITION = "adjustPosition"
        const val PROP_HOLD_KEYBOARD = "holdKeyboard"
        const val PROP_DISABLE_DEFAULT_PADDING = "disableDefaultPadding"
        const val PROP_CONFIRM_TYPE = "confirmType"
        const val PROP_CONFIRM_HOLD = "confirmHold"
        const val PROP_NAME = "name"

        // Callbacks
        const val CALLBACK_INPUT = "inputCallback"
        const val CALLBACK_FOCUS = "focusCallback"
        const val CALLBACK_BLUR = "blurCallback"
        const val CALLBACK_CONFIRM = "confirmCallback"
        const val CALLBACK_LINE_CHANGE = "lineChangeCallback"
        const val CALLBACK_KEYBOARD_HEIGHT_CHANGE = "keyboardHeightChangeCallback"

        // Mini-program native event names
        private const val EVENT_INPUT = "input"
        private const val EVENT_FOCUS = "focus"
        private const val EVENT_BLUR = "blur"
        private const val EVENT_CONFIRM = "confirm"
        private const val EVENT_LINE_CHANGE = "linechange"
        private const val EVENT_KEYBOARD_HEIGHT_CHANGE = "keyboardheightchange"

        private const val KEY_DATA = "data"
    }
}
