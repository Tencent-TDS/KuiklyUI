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

package com.tencent.kuikly.android.demo

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport

class KRWebView(context: Context) : FrameLayout(context), IKuiklyRenderViewExport {
    private val webView: WebView

    init {
        @SuppressLint("SetJavaScriptEnabled")
        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = WebViewClient()
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        }
        addView(webView)
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            "src" -> {
                val url = propValue as String
                if (url.isNotEmpty()) {
                    webView.loadUrl(url)
                }
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.stopLoading()
        webView.destroy()
    }

    companion object {
        const val VIEW_NAME = "KRWebView"
    }
}
