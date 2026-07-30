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

package com.tencent.kuikly.demo.pages.demo.wx

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.wx.views.WXWebView
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * Dedicated demo page for the WeChat mini-program native `web-view` component.
 *
 * In WeChat mini-program, `web-view` always occupies the whole page area and
 * visually covers all sibling nodes (including any Kuikly-rendered NavBar),
 * so there is no way to show custom UI on the same page. Instead, this page
 * overrides the global `navigationStyle: custom` via its `index.json` so that
 * the WeChat native navigation bar (with the built-in back button) is shown,
 * and the page body only contains the `web-view` itself.
 *
 * Event details (onLoad / onError / onMessage) are printed to the console
 * via `KLog` for debugging.
 */
@Page("WXWebViewDemoPage")
internal class WXWebViewDemoPage : BasePager() {

    private var webViewUrl by observable(DEFAULT_URL)

    override fun created() {
        super.created()
        val initialUrl = pageData.params.optString(PARAM_URL)
        if (initialUrl.isNotEmpty()) {
            webViewUrl = initialUrl
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(Color.WHITE)
            }

            // The web-view occupies the whole page area. WeChat ignores width/
            // height styles on `web-view`, so no explicit sizing is required.
            WXWebView {
                attr {
                    src(ctx.webViewUrl)
                    showProgress(true)
                    referrerPolicy("no-referrer")
                }
                event {
                    onLoad { detail ->
                        KLog.i(TAG, "web-view onLoad: $detail")
                    }
                    onError { detail ->
                        KLog.i(TAG, "web-view onError: $detail")
                    }
                    onMessage { detail ->
                        KLog.i(TAG, "web-view onMessage: $detail")
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WXWebViewDemoPage"
        const val PARAM_URL = "url"
        private const val DEFAULT_URL =
            "https://developers.weixin.qq.com/miniprogram/dev/component/web-view.html"
    }
}
