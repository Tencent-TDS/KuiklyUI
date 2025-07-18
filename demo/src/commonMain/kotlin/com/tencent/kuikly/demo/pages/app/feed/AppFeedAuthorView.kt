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

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.CalendarModule
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.app.model.AppUserInfo
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

internal class AppFeedItemAuthorView: ComposeView<AppFeedItemAuthorViewAttr, AppFeedItemAuthorViewEvent>() {

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

    override fun createEvent(): AppFeedItemAuthorViewEvent {
        return AppFeedItemAuthorViewEvent()
    }

    override fun createAttr(): AppFeedItemAuthorViewAttr {
        return AppFeedItemAuthorViewAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirection(FlexDirection.ROW)
                paddingLeft(15.0f)
                paddingTop(10.0f)
                paddingRight(15.0f)
                paddingBottom(2.0f)
            }
            // 头像
            View {
                Image {
                    attr {
                        alignSelfCenter()
                        resizeContain()
                        size(width = 40f, height = 40f)
                        borderRadius(20f)
                        src(ctx.attr.userInfo.headUrl)
                    }
                }
                // 实名认证
                vif({ctx.attr.userInfo.isVerify != 0}) {
                    Image {
                        attr {
                            alignSelfCenter()
                            resizeContain()
                            size(width = 15f, height = 15f)
                            if (ctx.attr.userInfo.isVerify == 1) {
                                src(ImageUri.pageAssets("home_verify.webp"))
                            } else {
                                src(ImageUri.pageAssets("home_verify2.webp"))
                            }
                            absolutePosition(right = 0f, bottom = 0f)
                        }
                    }
                }
            }

            View {
                attr {
                    marginLeft(5f)
                    flex(1.0f)
                }
                // 昵称
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                    }
                    Text {
                        attr {
                            marginLeft(6f)
                            text(ctx.attr.userInfo.nick)
                            fontSize(15f)
                            if (ctx.attr.userInfo.isMember == 0) {
                                color(ctx.theme.colors.feedUserNameNormal)
                            } else {
                                color(ctx.theme.colors.feedUserNameMember)
                            }
                        }
                    }
                    vif({ctx.attr.userInfo.isMember != 0}) {
                        Image {
                            attr {
                                marginLeft(3f)
                                marginTop(3f)
                                size(15f, 13f)
                                src(ImageUri.pageAssets("home_member.webp"))
                            }
                        }
                    }
                }
                // 签名
                View {
                    attr {
                        padding(left = 6f, top = 5f)
                    }
                    vif({ctx.attr.tail.isEmpty()}) {
                        Text {
                            attr {
                                text(ctx.attr.userInfo.desc)
                                color(ctx.theme.colors.feedUserSignature)
                                fontSize(11.0f)
                            }
                        }
                    }
                    vif({ctx.attr.tail.isNotEmpty()}) {
                        View {
                            attr {
                                flexDirection(FlexDirection.ROW)
                            }
                            Text {
                                val datetime =
                                    acquireModule<CalendarModule>(CalendarModule.MODULE_NAME).formatTime(
                                        ctx.attr.createTime,
                                        "yyyy-MM-dd HH:mm:ss"
                                    )
                                attr {
                                    text(datetime)
                                    color(ctx.theme.colors.feedUserSignature)
                                    fontSize(11.0f)
                                }
                            }
                            Text {
                                attr {
                                    margin(left = 7.0f, right = 7.0f)
                                    text("来自")
                                    color(ctx.theme.colors.feedUserSignature)
                                    fontSize(11.0f)
                                }
                            }
                            Text {
                                attr {
                                    text(ctx.attr.tail)
                                    color(ctx.theme.colors.feedUserDevice)
                                    fontSize(11.0f)
                                }
                            }
                        }
                    }
                }
            }
            // 关注按钮
            View {
                attr {
                    justifyContentFlexEnd()
                    alignItemsCenter()
                    padding(top = 4.0f, bottom = 4.0f, left = 8.0f, right = 8.0f)
                    borderRadius(12.0f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = ctx.theme.colors.feedUserFollowButton))
                }
                Text {
                    attr {
                        text("+ 关注")
                        color(ctx.theme.colors.feedUserFollowButton)
                        fontSize(12.0f)
                    }
                }
            }

        }
    }
}

internal class AppFeedItemAuthorViewAttr : ComposeAttr() {
    lateinit var userInfo: AppUserInfo
    lateinit var tail: String
    var createTime: Long = 0L
}

internal class AppFeedItemAuthorViewEvent : ComposeEvent() {
    
}

internal fun ViewContainer<*, *>.AppFeedItemAuthor(init: AppFeedItemAuthorView.() -> Unit) {
    addChild(AppFeedItemAuthorView(), init)
}