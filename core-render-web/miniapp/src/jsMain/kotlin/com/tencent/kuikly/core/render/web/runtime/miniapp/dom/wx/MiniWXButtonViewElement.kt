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
 * DOM wrapper for the WeChat mini-program native `button` component.
 *
 * This element finally renders to the mini-program native `<button/>` component
 * through the template located in `base.wxml` (template id `tmpl_0_76`).
 */
class MiniWXButtonViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {

    /**
     * Mini-program button size, value: `default` / `mini`.
     */
    @JsName("size")
    var size: String = ""
        set(value) {
            setAttribute("size", value)
            field = value
        }

    /**
     * Mini-program button type, value: `primary` / `default` / `warn`.
     */
    @JsName("type")
    var type: String = ""
        set(value) {
            setAttribute("type", value)
            field = value
        }

    /**
     * Whether the button is hollow.
     */
    @JsName("plain")
    var plain: Boolean = false
        set(value) {
            setAttribute("plain", value)
            field = value
        }

    /**
     * Whether the button is disabled.
     */
    @JsName("disabled")
    var disabled: Boolean = false
        set(value) {
            setAttribute("disabled", value)
            field = value
        }

    /**
     * Whether the button shows loading state.
     */
    @JsName("loading")
    var loading: Boolean = false
        set(value) {
            setAttribute("loading", value)
            field = value
        }

    /**
     * Form type used when the button is placed inside a `form`, value: `submit` / `reset`.
     */
    @JsName("formType")
    var formType: String = ""
        set(value) {
            setAttribute("formType", value)
            field = value
        }

    /**
     * Mini-program open-type capability, e.g. `getPhoneNumber`, `openSetting`, `contact`, etc.
     */
    @JsName("openType")
    var openType: String = ""
        set(value) {
            setAttribute("openType", value)
            field = value
        }

    /**
     * Button display name.
     */
    @JsName("name")
    var name: String = ""
        set(value) {
            setAttribute("name", value)
            field = value
        }

    /**
     * Button language, value: `en` / `zh_CN` / `zh_TW`.
     */
    @JsName("lang")
    var lang: String = ""
        set(value) {
            setAttribute("lang", value)
            field = value
        }

    /**
     * Session source used when `openType="contact"`.
     */
    @JsName("sessionFrom")
    var sessionFrom: String = ""
        set(value) {
            setAttribute("sessionFrom", value)
            field = value
        }

    /**
     * Parameters passed to the app, only used when `openType="launchApp"`.
     */
    @JsName("appParameter")
    var appParameter: String = ""
        set(value) {
            setAttribute("appParameter", value)
            field = value
        }

    /**
     * Whether to show the session inner message card.
     */
    @JsName("showMessageCard")
    var showMessageCard: Boolean = false
        set(value) {
            setAttribute("showMessageCard", value)
            field = value
        }

    /**
     * Customer service message business id.
     */
    @JsName("businessId")
    var businessId: String = ""
        set(value) {
            setAttribute("businessId", value)
            field = value
        }

    companion object {
        /**
         * Internal node name used to distinguish this element from the framework's built-in
         * `button` (which, on non-mini-program web, maps to DOM `<button>`). The mini-program
         * output tag is still `button` — that is controlled by the template `tmpl_0_76` in
         * `base.wxml`, bound via [componentsAlias]'s `_num: '76'`.
         */
        const val NODE_NAME = "wx-button"

        /**
         * Component alias mapping for mini-program `button`.
         *
         * `_num` is the template id, must match the template `tmpl_0_76` defined in `base.wxml`.
         * Other fields define the property name mapping used to optimize `setData` payload size.
         */
        val componentsAlias: dynamic = js(
            """
            {
                _num: '76',
                class: 'cl',
                animation: 'p0',
                openType: 'openType',
                formType: 'formType',
                type: 'type',
                size: 'size',
                plain: 'plain',
                disabled: 'disabled',
                loading: 'loading',
                name: 'name',
                lang: 'lang',
                sessionFrom: 'sessionFrom',
                appParameter: 'appParameter',
                showMessageCard: 'showMessageCard',
                businessId: 'businessId'
            }
            """
        )
    }
}
