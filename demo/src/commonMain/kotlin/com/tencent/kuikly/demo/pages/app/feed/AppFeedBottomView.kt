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
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

internal class AppFeedBottomView: ComposeView<AppFeedBottomViewAttr, AppFeedBottomViewEvent>() {

    private var colorScheme by observable(ThemeManager.colorScheme)
    private lateinit var eventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        eventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                colorScheme = ThemeManager.colorScheme
            }
    }

    override fun viewDestroyed() {
        super.viewDestroyed()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, eventCallbackRef)
    }

    override fun createEvent(): AppFeedBottomViewEvent {
        return AppFeedBottomViewEvent()
    }

    override fun createAttr(): AppFeedBottomViewAttr {
        return AppFeedBottomViewAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionRow()
                allCenter()
            }

            View {
                attr {
                    flex(1.0f)
                    flexDirectionRow()
                    allCenter()
                }

                Image {
                    attr {
                        alignSelfCenter()
                        resizeContain()
                        size(width = 22f, height = 22f)
                        src(ImageUri.pageAssets("ic_home_forward.png"))
                        tintColor(ctx.colorScheme.feedBottomIcon)
                    }
                }

                Text {
                    attr {
                        text("${ctx.attr.retweetNum}")
                        marginLeft(4f)
                        fontSize(13f)
                        color(ctx.colorScheme.feedBottomText)
                    }
                }

            }

            View {
                attr {
                    flex(1.0f)
                    flexDirectionRow()
                    allCenter()
                }

                Image {
                    attr {
                        alignSelfCenter()
                        resizeContain()
                        size(width = 22f, height = 22f)
                        src(ImageUri.pageAssets("ic_home_comment.webp"))
                        tintColor(ctx.colorScheme.feedBottomIcon)
                    }
                }

                Text {
                    attr {
                        text("${ctx.attr.commentNum}")
                        marginLeft(4f)
                        fontSize(13f)
                        color(ctx.colorScheme.feedBottomText)
                    }
                }
            }

            View {
                attr {
                    flex(1.0f)
                    flexDirectionRow()
                    allCenter()
                }

                vbind({ ctx.attr.likeStatus }) {
                    Image {
                        attr {
                            alignSelfCenter()
                            resizeContain()
                            size(width = 22f, height = 22f)
                            if (ctx.attr.likeStatus == 1) {
                                src(ImageUri.pageAssets("ic_home_liked.webp"))
                            } else {
                                src(ImageUri.pageAssets("ic_home_like.webp"))
                            }
                            tintColor(ctx.colorScheme.feedBottomIcon)
                        }
                    }

                    Text {
                        attr {
                            if (ctx.attr.likeStatus == 1) {
                                text("${ctx.attr.likeNum + 1}")
                            } else {
                                text("${ctx.attr.likeNum}")
                            }
                            marginLeft(4f)
                            fontSize(13f)
                            color(ctx.colorScheme.feedBottomText)
                        }
                    }

                }

                event {
                    click {
                        // todo: 动画
                        if (ctx.attr.likeStatus == 0) {
                            ctx.attr.likeStatus = 1
                        } else {
                            ctx.attr.likeStatus = 0
                        }
                    }
                }

            }

        }
    }
}

internal class AppFeedBottomViewAttr : ComposeAttr() {
    var retweetNum = 0
    var commentNum = 0
    var likeStatus = 0
    var likeNum = 0
}

internal class AppFeedBottomViewEvent : ComposeEvent() {
    
}

internal fun ViewContainer<*, *>.AppFeedBottom(init: AppFeedBottomView.() -> Unit) {
    addChild(AppFeedBottomView(), init)
}