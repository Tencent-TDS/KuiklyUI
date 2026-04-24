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
 * DOM wrapper for the WeChat mini-program native `textarea` component.
 *
 * The underlying mini-program tag is `textarea`; we use `wx-textarea` as the internal node
 * name to avoid conflicting with the framework's built-in `textarea` alias (`tmpl_0_71`).
 * The template `tmpl_0_78` in `base.wxml` directly outputs `<textarea/>`.
 */
class MiniWXTextAreaViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {

    @JsName("value")
    var value: String = ""
        set(value) { setAttribute("value", value); field = value }

    @JsName("placeholder")
    var placeholder: String = ""
        set(value) { setAttribute("placeholder", value); field = value }

    @JsName("placeholderStyle")
    var placeholderStyle: String = ""
        set(value) { setAttribute("placeholderStyle", value); field = value }

    @JsName("placeholderClass")
    var placeholderClass: String = ""
        set(value) { setAttribute("placeholderClass", value); field = value }

    @JsName("disabled")
    var disabled: Boolean = false
        set(value) { setAttribute("disabled", value); field = value }

    @JsName("maxLength")
    var maxLength: Int = 140
        set(value) { setAttribute("maxLength", value); field = value }

    @JsName("autoFocus")
    var autoFocus: Boolean = false
        set(value) { setAttribute("autoFocus", value); field = value }

    @JsName("focus")
    var focus: Boolean = false
        set(value) { setAttribute("focus", value); field = value }

    @JsName("autoHeight")
    var autoHeight: Boolean = false
        set(value) { setAttribute("autoHeight", value); field = value }

    @JsName("fixed")
    var fixed: Boolean = false
        set(value) { setAttribute("fixed", value); field = value }

    @JsName("cursorSpacing")
    var cursorSpacing: Int = 0
        set(value) { setAttribute("cursorSpacing", value); field = value }

    @JsName("cursor")
    var cursor: Int = -1
        set(value) { setAttribute("cursor", value); field = value }

    @JsName("showConfirmBar")
    var showConfirmBar: Boolean = true
        set(value) { setAttribute("showConfirmBar", value); field = value }

    @JsName("selectionStart")
    var selectionStart: Int = -1
        set(value) { setAttribute("selectionStart", value); field = value }

    @JsName("selectionEnd")
    var selectionEnd: Int = -1
        set(value) { setAttribute("selectionEnd", value); field = value }

    @JsName("adjustPosition")
    var adjustPosition: Boolean = true
        set(value) { setAttribute("adjustPosition", value); field = value }

    @JsName("holdKeyboard")
    var holdKeyboard: Boolean = false
        set(value) { setAttribute("holdKeyboard", value); field = value }

    @JsName("disableDefaultPadding")
    var disableDefaultPadding: Boolean = false
        set(value) { setAttribute("disableDefaultPadding", value); field = value }

    @JsName("confirmType")
    var confirmType: String = "return"
        set(value) { setAttribute("confirmType", value); field = value }

    @JsName("confirmHold")
    var confirmHold: Boolean = false
        set(value) { setAttribute("confirmHold", value); field = value }

    @JsName("name")
    var name: String = ""
        set(value) { setAttribute("name", value); field = value }

    companion object {
        /**
         * Internal node name. The mini-program output tag is `textarea`, see template `tmpl_0_78`.
         */
        const val NODE_NAME = "wx-textarea"

        /**
         * Component alias for the WX textarea. `_num = '78'` matches `tmpl_0_78` in `base.wxml`.
         */
        val componentsAlias: dynamic = js(
            """
            {
                _num: '78',
                class: 'cl',
                animation: 'p0',
                value: 'value',
                placeholder: 'placeholder',
                placeholderStyle: 'placeholderStyle',
                placeholderClass: 'placeholderClass',
                disabled: 'disabled',
                maxLength: 'maxLength',
                autoFocus: 'autoFocus',
                focus: 'focus',
                autoHeight: 'autoHeight',
                fixed: 'fixed',
                cursorSpacing: 'cursorSpacing',
                cursor: 'cursor',
                showConfirmBar: 'showConfirmBar',
                selectionStart: 'selectionStart',
                selectionEnd: 'selectionEnd',
                adjustPosition: 'adjustPosition',
                holdKeyboard: 'holdKeyboard',
                disableDefaultPadding: 'disableDefaultPadding',
                confirmType: 'confirmType',
                confirmHold: 'confirmHold',
                name: 'name'
            }
            """
        )
    }
}
