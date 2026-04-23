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

package com.tencent.kuikly.core.wx.views

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Cross-platform Compose-style wrapper of the WeChat mini-program native `web-view` component.
 *
 * On mini-program platform (`pageData.params.is_miniprogram == "1"`) it renders the native
 * `<web-view/>` via `KRWXWebView`. On other platforms it falls back to a plain view so the
 * compose tree still renders (the webview capability is silently ignored).
 *
 * Notes:
 * - `web-view` is a full-screen embedded browser; the host mini-program must be an
 *   individual/企业 account with the business domain whitelisted in order to load URLs.
 * - The component takes the whole page by default in the native runtime; here we don't
 *   force sizing so callers may size/position it via layout attrs.
 */
class WXWebViewView : ComposeView<WXWebViewAttr, WXWebViewEvent>() {

    override fun createEvent(): WXWebViewEvent = WXWebViewEvent()

    override fun createAttr(): WXWebViewAttr = WXWebViewAttr()

    override fun viewName(): String {
        val pageData = getPager().pageData
        if (pageData.params.optString(IS_MINI_PROGRAM) == "1") {
            return VIEW_NAME_MINI_PROGRAM
        }
        return ViewConst.TYPE_VIEW
    }

    override fun body(): ViewBuilder {
        return {
            // Native <web-view> renders itself; no children expected.
        }
    }

    companion object {
        internal const val IS_MINI_PROGRAM = "is_miniprogram"
        internal const val VIEW_NAME_MINI_PROGRAM = "KRWXWebView"
    }
}

/**
 * Attributes for [WXWebViewView]. Mirrors native `web-view` attributes.
 */
class WXWebViewAttr : ComposeAttr() {

    /** Page URL to load. Must be in the business domain whitelist. */
    fun src(v: String): WXWebViewAttr { PROP_SRC with v; return this }

    /**
     * Whether to enable reader mode. Default false.
     *
     * When enabled, long articles will be rendered in a clean reader view on supported platforms.
     */
    fun bindReaderMode(v: Boolean): WXWebViewAttr { PROP_BIND_READER_MODE with v; return this }

    /**
     * Whether to show the default loading progress bar. Default true.
     */
    fun showProgress(v: Boolean): WXWebViewAttr { PROP_SHOW_PROGRESS with v; return this }

    /**
     * Referrer policy. Typical values: `no-referrer` / `origin`.
     */
    fun referrerPolicy(v: String): WXWebViewAttr { PROP_REFERRER_POLICY with v; return this }

    /**
     * Whether to force-enable debug console (only effective in dev tools). Default false.
     */
    fun forceDebug(v: Boolean): WXWebViewAttr { PROP_FORCE_DEBUG with v; return this }

    companion object {
        internal const val PROP_SRC = "src"
        internal const val PROP_BIND_READER_MODE = "bindReaderMode"
        internal const val PROP_SHOW_PROGRESS = "showProgress"
        internal const val PROP_REFERRER_POLICY = "referrerPolicy"
        internal const val PROP_FORCE_DEBUG = "forceDebug"
    }
}

/**
 * Events for [WXWebViewView]. All callbacks receive a [JSONObject] whose `data` field is the
 * JSON-serialized native `detail` payload.
 */
class WXWebViewEvent : ComposeEvent() {

    /**
     * Called when the embedded web page calls `wx.miniProgram.postMessage`.
     *
     * The mini-program native event is only dispatched on specific timings (e.g. page
     * navigation / share / fullscreen changes) and the accumulated messages will be delivered
     * all at once under `detail.data` (an array).
     */
    fun onMessage(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_MESSAGE, handler)

    /** Called when the web page finishes loading. */
    fun onLoad(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_LOAD, handler)

    /** Called when the web page fails to load. */
    fun onError(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_ERROR, handler)

    /** Called when reader mode is toggled on/off (if `bindReaderMode` is enabled). */
    fun onReaderMode(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_READER_MODE, handler)

    private fun registerJsonCallback(eventName: String, handler: (JSONObject) -> Unit) {
        register(eventName) { handler(it as JSONObject) }
    }

    companion object {
        internal const val CALLBACK_MESSAGE = "messageCallback"
        internal const val CALLBACK_LOAD = "loadCallback"
        internal const val CALLBACK_ERROR = "errorCallback"
        internal const val CALLBACK_READER_MODE = "readerModeCallback"
    }
}

/**
 * DSL builder for [WXWebViewView].
 */
fun ViewContainer<*, *>.WXWebView(init: WXWebViewView.() -> Unit) {
    addChild(WXWebViewView(), init)
}
