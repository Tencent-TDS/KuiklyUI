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

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.event.Event

internal class SimpleWebViewAttr : Attr() {
    fun src(url: String) {
        "src" with url
    }
}

internal class SimpleWebView : DeclarativeBaseView<SimpleWebViewAttr, Event>() {
    override fun createAttr() = SimpleWebViewAttr()
    override fun createEvent() = Event()
    override fun viewName() = "KRWebView"
}

@Composable
fun SimpleWebViewCompose(
    url: String,
    modifier: Modifier = Modifier,
) {
    MakeKuiklyComposeNode<SimpleWebView>(
        factory = { SimpleWebView() },
        modifier = modifier,
        viewInit = {
            getViewAttr().src(url)
        },
        viewUpdate = {
            it.getViewAttr().src(url)
        },
    )
}
