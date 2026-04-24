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

package com.tencent.kuikly.core.render.web.runtime.miniapp.dom.wx

import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElementUtil

/**
 * DOM wrapper for the WeChat mini-program native `input` component.
 *
 * This element finally renders to the mini-program native `<input/>` component
 * through the template located in `base.wxml` (template id `tmpl_0_77`).
 */
class MiniWXInputViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {

    /**
     * Current value of the input.
     */
    @JsName("value")
    var value: String = ""
        set(value) {
            setAttribute("value", value)
            field = value
        }

    /**
     * Input type, value: `text` / `number` / `idcard` / `digit` / `safe-password` / `nickname`.
     */
    @JsName("type")
    var type: String = ""
        set(value) {
            setAttribute("type", value)
            field = value
        }

    /**
     * Whether the input is a password field.
     */
    @JsName("password")
    var password: Boolean = false
        set(value) {
            setAttribute("password", value)
            field = value
        }

    /**
     * Placeholder text shown when value is empty.
     */
    @JsName("placeholder")
    var placeholder: String = ""
        set(value) {
            setAttribute("placeholder", value)
            field = value
        }

    /**
     * Inline style string applied to the placeholder.
     */
    @JsName("placeholderStyle")
    var placeholderStyle: String = ""
        set(value) {
            setAttribute("placeholderStyle", value)
            field = value
        }

    /**
     * CSS class applied to the placeholder.
     */
    @JsName("placeholderClass")
    var placeholderClass: String = ""
        set(value) {
            setAttribute("placeholderClass", value)
            field = value
        }

    /**
     * Whether the input is disabled.
     */
    @JsName("disabled")
    var disabled: Boolean = false
        set(value) {
            setAttribute("disabled", value)
            field = value
        }

    /**
     * Maximum length of the input value.
     */
    @JsName("maxLength")
    var maxLength: Int = 140
        set(value) {
            setAttribute("maxLength", value)
            field = value
        }

    /**
     * Whether to auto focus the input.
     */
    @JsName("focus")
    var focus: Boolean = false
        set(value) {
            setAttribute("focus", value)
            field = value
        }

    /**
     * Text of the "done" button on the soft keyboard.
     */
    @JsName("confirmType")
    var confirmType: String = ""
        set(value) {
            setAttribute("confirmType", value)
            field = value
        }

    /**
     * Whether to keep the keyboard open after "done" is pressed.
     */
    @JsName("confirmHold")
    var confirmHold: Boolean = false
        set(value) {
            setAttribute("confirmHold", value)
            field = value
        }

    /**
     * Whether to adjust the page when the keyboard is up.
     */
    @JsName("adjustPosition")
    var adjustPosition: Boolean = true
        set(value) {
            setAttribute("adjustPosition", value)
            field = value
        }

    /**
     * Whether to hold the keyboard even if user taps outside the input.
     */
    @JsName("holdKeyboard")
    var holdKeyboard: Boolean = false
        set(value) {
            setAttribute("holdKeyboard", value)
            field = value
        }

    /**
     * Distance between cursor and keyboard.
     */
    @JsName("cursorSpacing")
    var cursorSpacing: Int = 0
        set(value) {
            setAttribute("cursorSpacing", value)
            field = value
        }

    /**
     * Cursor position.
     */
    @JsName("cursor")
    var cursor: Int = -1
        set(value) {
            setAttribute("cursor", value)
            field = value
        }

    /**
     * Selection start index.
     */
    @JsName("selectionStart")
    var selectionStart: Int = -1
        set(value) {
            setAttribute("selectionStart", value)
            field = value
        }

    /**
     * Selection end index.
     */
    @JsName("selectionEnd")
    var selectionEnd: Int = -1
        set(value) {
            setAttribute("selectionEnd", value)
            field = value
        }

    /**
     * Input display name used by the form.
     */
    @JsName("name")
    var name: String = ""
        set(value) {
            setAttribute("name", value)
            field = value
        }

    companion object {
        /**
         * WeChat mini-program native tag name, must be `input`.
         */
        const val NODE_NAME = "wx-input"

        /**
         * Component alias mapping for the wrapped mini-program `input`.
         *
         * `_num` is the template id and must match the template `tmpl_0_77` defined in
         * `base.wxml`. Unlike the built-in framework template for `input` (`tmpl_0_29`,
         * which uses `pN` aliases), the `WX` wrapper template uses the property name
         * directly to keep implementation simple and aligned with [MiniWXButtonViewElement].
         */
        val componentsAlias: dynamic = js(
            """
            {
                _num: '77',
                class: 'cl',
                animation: 'p0',
                value: 'value',
                type: 'type',
                password: 'password',
                placeholder: 'placeholder',
                placeholderStyle: 'placeholderStyle',
                placeholderClass: 'placeholderClass',
                disabled: 'disabled',
                maxLength: 'maxLength',
                focus: 'focus',
                confirmType: 'confirmType',
                confirmHold: 'confirmHold',
                adjustPosition: 'adjustPosition',
                holdKeyboard: 'holdKeyboard',
                cursorSpacing: 'cursorSpacing',
                cursor: 'cursor',
                selectionStart: 'selectionStart',
                selectionEnd: 'selectionEnd',
                name: 'name'
            }
            """
        )
    }
}
