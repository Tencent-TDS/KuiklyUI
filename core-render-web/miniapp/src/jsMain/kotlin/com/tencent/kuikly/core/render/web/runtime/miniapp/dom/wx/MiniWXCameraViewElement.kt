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
 * DOM wrapper for the WeChat mini-program native `camera` component.
 *
 * Registered under node name `wx-camera`, mapped to `tmpl_0_81` in `base.wxml` via
 * `componentsAlias._num = '81'`. The actual output tag is still the native `<camera>`.
 */
class MiniWXCameraViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {

    @JsName("mode")
    var mode: String = "normal"
        set(value) { setAttribute("mode", value); field = value }

    @JsName("resolution")
    var resolution: String = "medium"
        set(value) { setAttribute("resolution", value); field = value }

    @JsName("devicePosition")
    var devicePosition: String = "back"
        set(value) { setAttribute("devicePosition", value); field = value }

    @JsName("flash")
    var flash: String = "auto"
        set(value) { setAttribute("flash", value); field = value }

    @JsName("frameSize")
    var frameSize: String = "medium"
        set(value) { setAttribute("frameSize", value); field = value }

    companion object {
        /** Internal node name. Mini-program output tag is `camera`, see template `tmpl_0_81`. */
        const val NODE_NAME = "wx-camera"

        /** Component alias. `_num = '81'` matches `tmpl_0_81` in `base.wxml`. */
        val componentsAlias: dynamic = js(
            """
            {
                _num: '81',
                class: 'cl',
                animation: 'p0',
                mode: 'mode',
                resolution: 'resolution',
                devicePosition: 'devicePosition',
                flash: 'flash',
                frameSize: 'frameSize'
            }
            """
        )
    }
}
