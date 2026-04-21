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
 * DOM wrapper for the WeChat mini-program native `web-view` component.
 *
 * Registered under node name `wx-web-view`, mapped to `tmpl_0_83` in `base.wxml` via
 * `componentsAlias._num = '83'`. The actual output tag is still the native `<web-view>`.
 */
class MiniWXWebViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {

    @JsName("src")
    var src: String = ""
        set(value) { setAttribute("src", value); field = value }

    @JsName("bindReaderMode")
    var bindReaderMode: Boolean = false
        set(value) { setAttribute("bindReaderMode", value); field = value }

    @JsName("showProgress")
    var showProgress: Boolean = true
        set(value) { setAttribute("showProgress", value); field = value }

    @JsName("referrerPolicy")
    var referrerPolicy: String = "no-referrer"
        set(value) { setAttribute("referrerPolicy", value); field = value }

    @JsName("forceDebug")
    var forceDebug: Boolean = false
        set(value) { setAttribute("forceDebug", value); field = value }

    companion object {
        /** Internal node name. Mini-program output tag is `web-view`, see template `tmpl_0_83`. */
        const val NODE_NAME = "wx-web-view"

        /** Component alias. `_num = '83'` matches `tmpl_0_83` in `base.wxml`. */
        val componentsAlias: dynamic = js(
            """
            {
                _num: '83',
                class: 'cl',
                animation: 'p0',
                src: 'src',
                bindReaderMode: 'bindReaderMode',
                showProgress: 'showProgress',
                referrerPolicy: 'referrerPolicy',
                forceDebug: 'forceDebug'
            }
            """
        )
    }
}
