/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.kuikly.demo.pages.app

import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

internal class AppEmptyPageView(val title: String): ComposeView<AppEmptyPageViewAttr, AppEmptyPageViewEvent>() {

    private var colorScheme by observable(ThemeManager.colorScheme)
    private lateinit var eventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        eventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify("skinChanged") { _ ->
                colorScheme = ThemeManager.colorScheme
            }
    }

    override fun viewDestroyed() {
        super.viewDestroyed()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify("skinChanged", eventCallbackRef)
    }

    override fun createEvent(): AppEmptyPageViewEvent {
        return AppEmptyPageViewEvent()
    }

    override fun createAttr(): AppEmptyPageViewAttr {
        return AppEmptyPageViewAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
                flex(1f)
                backgroundColor(ctx.colorScheme.background)
            }
            Text {
                attr {
                    text(ctx.title)
                    color(ctx.colorScheme.backgroundElement)
                }
            }
        }
    }
}

internal class AppEmptyPageViewAttr : ComposeAttr()

internal class AppEmptyPageViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.AppEmptyPage(title: String, init: AppEmptyPageView.() -> Unit) {
    addChild(AppEmptyPageView(title), init)
}