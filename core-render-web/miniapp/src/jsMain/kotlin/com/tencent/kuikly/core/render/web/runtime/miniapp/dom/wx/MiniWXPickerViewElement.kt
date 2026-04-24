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
 * DOM wrapper for the WeChat mini-program native `picker` component.
 *
 * The mini-program output tag is `picker`. We keep the internal node name `wx-picker` so
 * it does not conflict with any other `picker` alias. Template `tmpl_0_79` in `base.wxml`
 * renders the actual `<picker>` with its children.
 *
 * Because `picker` has attributes whose semantic depends on `mode` (e.g. `value` is an index
 * in `selector` mode and an array of indices in `multiSelector` mode), all typed values are
 * passed as strings from Kotlin side and the WXML template uses the raw values directly.
 */
class MiniWXPickerViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {

    @JsName("mode")
    var mode: String = "selector"
        set(value) { setAttribute("mode", value); field = value }

    @JsName("disabled")
    var disabled: Boolean = false
        set(value) { setAttribute("disabled", value); field = value }

    @JsName("name")
    var name: String = ""
        set(value) { setAttribute("name", value); field = value }

    /**
     * Range attribute. Always passed as a JSON string and unwrapped by the WXML template
     * through `JSON.parse` (see `tmpl_0_79`).
     */
    @JsName("range")
    var range: String = "[]"
        set(value) { setAttribute("range", value); field = value }

    @JsName("rangeKey")
    var rangeKey: String = ""
        set(value) { setAttribute("rangeKey", value); field = value }

    /**
     * Value attribute. Passed as a string; template parses with `JSON.parse` for list-valued
     * modes (`multiSelector`, `region`) and uses raw string for `time`/`date` and int for
     * `selector`.
     */
    @JsName("value")
    var value: String = ""
        set(value) { setAttribute("value", value); field = value }

    @JsName("start")
    var start: String = ""
        set(value) { setAttribute("start", value); field = value }

    @JsName("end")
    var end: String = ""
        set(value) { setAttribute("end", value); field = value }

    @JsName("fields")
    var fields: String = "day"
        set(value) { setAttribute("fields", value); field = value }

    @JsName("headerText")
    var headerText: String = ""
        set(value) { setAttribute("headerText", value); field = value }

    @JsName("fixed")
    var fixed: Boolean = false
        set(value) { setAttribute("fixed", value); field = value }

    companion object {
        /**
         * Internal node name. The mini-program output tag is `picker`, see template `tmpl_0_79`.
         */
        const val NODE_NAME = "wx-picker"

        /** Alias mapping. `_num = '79'` matches `tmpl_0_79` in `base.wxml`. */
        val componentsAlias: dynamic = js(
            """
            {
                _num: '79',
                class: 'cl',
                animation: 'p0',
                mode: 'mode',
                disabled: 'disabled',
                name: 'name',
                range: 'range',
                rangeKey: 'rangeKey',
                value: 'value',
                start: 'start',
                end: 'end',
                fields: 'fields',
                headerText: 'headerText',
                fixed: 'fixed'
            }
            """
        )
    }
}
