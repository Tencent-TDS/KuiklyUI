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

package com.tencent.kuikly.demo.pages.app.feed

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.demo.pages.app.common.AppNineGrid
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

internal class AppForwardView: ComposeView<AppForwardViewAttr, AppForwardViewEvent>() {

    private var theme by observable(ThemeManager.getTheme())
    private lateinit var eventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        eventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                theme = ThemeManager.getTheme()
            }
    }

    override fun viewDestroyed() {
        super.viewDestroyed()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, eventCallbackRef)
    }

    override fun createEvent(): AppForwardViewEvent {
        return AppForwardViewEvent()
    }

    override fun createAttr(): AppForwardViewAttr {
        return AppForwardViewAttr()
    }

    private fun getDisplayContent(): String {
        var displayContent = ""
        if (!attr.forwardNick.isNullOrEmpty()
            && !attr.forwardUserId.isNullOrEmpty()
            && !attr.forwardContent.isNullOrEmpty()) {
            displayContent = "[@${attr.forwardNick}:${attr.forwardUserId}]:${attr.forwardContent}"
        }
        return displayContent
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val displayContent = getDisplayContent()
        return {
            if (ctx.attr.containForward) {
                attr {
                    padding(12.0f)
                    marginTop(5.0f)
                    backgroundColor(ctx.theme.colors.feedContentQuoteBackground)
                }
                if (displayContent.isNotEmpty()) {
                    AppFeedContent {
                        attr {
                            content = displayContent
                        }
                    }
                    ctx.attr.forwardVideoUrl?.let {
                        if (it.isNotEmpty()) {
                            AppFeedVideo {
                                attr {
                                    videoUrl = it
                                }
                            }
                        }
                    }
                    ctx.attr.forwardPicUrl?.let {
                        if (it.isNotEmpty()) {
                            AppNineGrid {
                                attr {
                                    picUrls = it
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class AppForwardViewAttr : ComposeAttr() {
    var containForward: Boolean = false
    var forwardNick: String? = null
    var forwardUserId: String? = null
    var forwardContent: String? = null
    var forwardVideoUrl: String? = null
    var forwardPicUrl: List<String>? = null
}

internal class AppForwardViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.AppForward(init: AppForwardView.() -> Unit) {
    addChild(AppForwardView(), init)
}