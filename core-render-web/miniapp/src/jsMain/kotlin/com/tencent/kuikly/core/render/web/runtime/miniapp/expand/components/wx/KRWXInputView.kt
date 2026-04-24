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
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.wx.MiniWXInputViewElement
import org.w3c.dom.Element

/**
 * Render view export for the WeChat mini-program native `input` component.
 *
 * Delegates property setting and event listening to [MiniWXInputViewElement].
 * This class is registered as `KRWXInputView` inside
 * [com.tencent.kuikly.core.render.web.runtime.miniapp.expand.KuiklyRenderViewDelegator].
 */
class KRWXInputView : IKuiklyRenderViewExport {
    private val inputElement = MiniWXInputViewElement()

    override val ele: Element
        get() = inputElement.unsafeCast<Element>()

    // Event callbacks
    private var inputCallback: KuiklyRenderCallback? = null
    private var focusCallback: KuiklyRenderCallback? = null
    private var blurCallback: KuiklyRenderCallback? = null
    private var confirmCallback: KuiklyRenderCallback? = null
    private var keyboardHeightChangeCallback: KuiklyRenderCallback? = null
    private var nickNameReviewCallback: KuiklyRenderCallback? = null

    init {
        inputElement.addEventListener(EVENT_INPUT, createEventForwarder { inputCallback })
        inputElement.addEventListener(EVENT_FOCUS, createEventForwarder { focusCallback })
        inputElement.addEventListener(EVENT_BLUR, createEventForwarder { blurCallback })
        inputElement.addEventListener(EVENT_CONFIRM, createEventForwarder { confirmCallback })
        inputElement.addEventListener(EVENT_KEYBOARD_HEIGHT_CHANGE, createEventForwarder { keyboardHeightChangeCallback })
        inputElement.addEventListener(EVENT_NICKNAME_REVIEW, createEventForwarder { nickNameReviewCallback })
    }

    /**
     * Build an [EventHandler] that forwards the mini-program native event `detail`
     * (as a JSON-serialized string) to the callback returned by [callbackSupplier].
     */
    private fun createEventForwarder(callbackSupplier: () -> KuiklyRenderCallback?): EventHandler {
        return { event: dynamic ->
            val data: dynamic = JSON.stringify(event.detail)
            callbackSupplier()?.invoke(mapOf<String, Any>(KEY_DATA to data.unsafeCast<String>()))
        }
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_VALUE -> {
                inputElement.value = propValue as String
                true
            }
            PROP_TYPE -> {
                inputElement.type = propValue as String
                true
            }
            PROP_PASSWORD -> {
                inputElement.password = toBoolean(propValue)
                true
            }
            PROP_PLACEHOLDER -> {
                inputElement.placeholder = propValue as String
                true
            }
            PROP_PLACEHOLDER_STYLE -> {
                inputElement.placeholderStyle = propValue as String
                true
            }
            PROP_PLACEHOLDER_CLASS -> {
                inputElement.placeholderClass = propValue as String
                true
            }
            PROP_DISABLED -> {
                inputElement.disabled = toBoolean(propValue)
                true
            }
            PROP_MAX_LENGTH -> {
                inputElement.maxLength = toInt(propValue)
                true
            }
            PROP_FOCUS -> {
                inputElement.focus = toBoolean(propValue)
                true
            }
            PROP_CONFIRM_TYPE -> {
                inputElement.confirmType = propValue as String
                true
            }
            PROP_CONFIRM_HOLD -> {
                inputElement.confirmHold = toBoolean(propValue)
                true
            }
            PROP_ADJUST_POSITION -> {
                inputElement.adjustPosition = toBoolean(propValue)
                true
            }
            PROP_HOLD_KEYBOARD -> {
                inputElement.holdKeyboard = toBoolean(propValue)
                true
            }
            PROP_CURSOR_SPACING -> {
                inputElement.cursorSpacing = toInt(propValue)
                true
            }
            PROP_CURSOR -> {
                inputElement.cursor = toInt(propValue)
                true
            }
            PROP_SELECTION_START -> {
                inputElement.selectionStart = toInt(propValue)
                true
            }
            PROP_SELECTION_END -> {
                inputElement.selectionEnd = toInt(propValue)
                true
            }
            PROP_NAME -> {
                inputElement.name = propValue as String
                true
            }
            CALLBACK_INPUT -> {
                inputCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_FOCUS -> {
                focusCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_BLUR -> {
                blurCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_CONFIRM -> {
                confirmCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_KEYBOARD_HEIGHT_CHANGE -> {
                keyboardHeightChangeCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_NICKNAME_REVIEW -> {
                nickNameReviewCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
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
        const val VIEW_NAME = "KRWXInputView"

        // Props
        const val PROP_VALUE = "value"
        const val PROP_TYPE = "type"
        const val PROP_PASSWORD = "password"
        const val PROP_PLACEHOLDER = "placeholder"
        const val PROP_PLACEHOLDER_STYLE = "placeholderStyle"
        const val PROP_PLACEHOLDER_CLASS = "placeholderClass"
        const val PROP_DISABLED = "disabled"
        const val PROP_MAX_LENGTH = "maxLength"
        const val PROP_FOCUS = "focus"
        const val PROP_CONFIRM_TYPE = "confirmType"
        const val PROP_CONFIRM_HOLD = "confirmHold"
        const val PROP_ADJUST_POSITION = "adjustPosition"
        const val PROP_HOLD_KEYBOARD = "holdKeyboard"
        const val PROP_CURSOR_SPACING = "cursorSpacing"
        const val PROP_CURSOR = "cursor"
        const val PROP_SELECTION_START = "selectionStart"
        const val PROP_SELECTION_END = "selectionEnd"
        const val PROP_NAME = "name"

        // Callbacks
        const val CALLBACK_INPUT = "inputCallback"
        const val CALLBACK_FOCUS = "focusCallback"
        const val CALLBACK_BLUR = "blurCallback"
        const val CALLBACK_CONFIRM = "confirmCallback"
        const val CALLBACK_KEYBOARD_HEIGHT_CHANGE = "keyboardHeightChangeCallback"
        const val CALLBACK_NICKNAME_REVIEW = "nickNameReviewCallback"

        // Mini-program native event names (lowercase as dispatched by mini-program runtime)
        private const val EVENT_INPUT = "input"
        private const val EVENT_FOCUS = "focus"
        private const val EVENT_BLUR = "blur"
        private const val EVENT_CONFIRM = "confirm"
        private const val EVENT_KEYBOARD_HEIGHT_CHANGE = "keyboardheightchange"
        private const val EVENT_NICKNAME_REVIEW = "nicknamereview"

        private const val KEY_DATA = "data"
    }
}
