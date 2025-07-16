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

package com.tencent.kuikly.demo.pages.app.feed

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.app.common.AppNineGrid
import com.tencent.kuikly.demo.pages.app.model.AppFeedModel
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

internal class AppFeedItemView : ComposeView<AppFeedItemViewAttr, AppFeedItemViewEvent>() {

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

    override fun createAttr(): AppFeedItemViewAttr {
        return AppFeedItemViewAttr()
    }

    override fun createEvent(): AppFeedItemViewEvent {
        return AppFeedItemViewEvent()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(ctx.colorScheme.feedBackground)
            }
            // 作者
            AppFeedItemAuthor {
                attr {
                    userInfo = ctx.attr.item.userInfo
                    tail = ctx.attr.item.tail
                    createTime = ctx.attr.item.createTime.toLong()
                }
            }
            // 文字内容区域
            AppFeedContent {
                attr {
                    content = ctx.attr.item.content
                }
            }
            // 视频区域
            AppFeedVideo {
                attr {
                    videoUrl = ctx.attr.item.videoUrl
                }
            }
            // 九宫图
            AppNineGrid {
                attr {
                    picUrls = ctx.attr.item.picUrl
                }
            }
            // 转发内容
            AppForward {
                attr {
                    containForward = ctx.attr.item.containForward
                    forwardContent = ctx.attr.item.forwardContent
                    forwardNick = ctx.attr.item.forwardNick
                    forwardPicUrl = ctx.attr.item.forwardPicUrl
                    forwardUserId = ctx.attr.item.forwardUserId
                    forwardVideoUrl = ctx.attr.item.forwardVideoUrl
                }
            }
            // 下划线
            View {
                attr {
                    margin(left = 15.0f, right = 15.0f, bottom = 10.0f, top = 0.0f)
                    height(0.5f)
                    backgroundColor(ctx.colorScheme.feedContentDivider)
                }
            }
            // 转发收藏点赞
            AppFeedBottom {
                attr {
                    retweetNum = ctx.attr.item.forwardNum
                    commentNum = ctx.attr.item.commentNum
                    likeNum = ctx.attr.item.likeNum
                    likeStatus = ctx.attr.item.likeStatus
                }
            }
            // 间隔
            View {
                attr {
                    marginTop(10.0f)
                    height(12.0f)
                    backgroundColor(ctx.colorScheme.background)
                }
            }
        }
    }

}

internal class AppFeedItemViewAttr : ComposeAttr() {
    lateinit var item: AppFeedModel
}

internal class AppFeedItemViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.AppFeedItem(init: AppFeedItemView.() -> Unit) {
    addChild(AppFeedItemView(), init)
}