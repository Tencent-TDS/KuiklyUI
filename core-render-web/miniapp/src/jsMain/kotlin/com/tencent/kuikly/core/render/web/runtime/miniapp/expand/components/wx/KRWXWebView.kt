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
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.wx.MiniWXWebViewElement
import org.w3c.dom.Element

/**
 * Render view export for the WeChat mini-program native `web-view` component.
 *
 * Delegates property setting and event forwarding to [MiniWXWebViewElement]. Native
 * events' `detail` is serialized as a JSON string and forwarded under the `data` key.
 */
class KRWXWebView : IKuiklyRenderViewExport {
    private val webViewElement = MiniWXWebViewElement()

    override val ele: Element
        get() = webViewElement.unsafeCast<Element>()

    // Event callbacks
    private var messageCallback: KuiklyRenderCallback? = null
    private var loadCallback: KuiklyRenderCallback? = null
    private var errorCallback: KuiklyRenderCallback? = null
    private var readerModeCallback: KuiklyRenderCallback? = null

    init {
        webViewElement.addEventListener(EVENT_MESSAGE, createEventForwarder { messageCallback })
        webViewElement.addEventListener(EVENT_LOAD, createEventForwarder { loadCallback })
        webViewElement.addEventListener(EVENT_ERROR, createEventForwarder { errorCallback })
        webViewElement.addEventListener(
            EVENT_READER_MODE,
            createEventForwarder { readerModeCallback }
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
            PROP_SRC -> { webViewElement.src = propValue as String; true }
            PROP_BIND_READER_MODE -> { webViewElement.bindReaderMode = toBoolean(propValue); true }
            PROP_SHOW_PROGRESS -> { webViewElement.showProgress = toBoolean(propValue); true }
            PROP_REFERRER_POLICY -> { webViewElement.referrerPolicy = propValue as String; true }
            PROP_FORCE_DEBUG -> { webViewElement.forceDebug = toBoolean(propValue); true }
            CALLBACK_MESSAGE -> {
                messageCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_LOAD -> { loadCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_ERROR -> { errorCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_READER_MODE -> {
                readerModeCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
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

    companion object {
        const val VIEW_NAME = "KRWXWebView"

        // Props
        const val PROP_SRC = "src"
        const val PROP_BIND_READER_MODE = "bindReaderMode"
        const val PROP_SHOW_PROGRESS = "showProgress"
        const val PROP_REFERRER_POLICY = "referrerPolicy"
        const val PROP_FORCE_DEBUG = "forceDebug"

        // Callbacks
        const val CALLBACK_MESSAGE = "messageCallback"
        const val CALLBACK_LOAD = "loadCallback"
        const val CALLBACK_ERROR = "errorCallback"
        const val CALLBACK_READER_MODE = "readerModeCallback"

        // Mini-program native event names
        private const val EVENT_MESSAGE = "message"
        private const val EVENT_LOAD = "load"
        private const val EVENT_ERROR = "error"
        private const val EVENT_READER_MODE = "readermode"

        private const val KEY_DATA = "data"
    }
}
