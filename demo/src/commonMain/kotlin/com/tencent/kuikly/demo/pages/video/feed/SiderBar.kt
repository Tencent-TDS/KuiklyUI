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


package com.tencent.kuikly.demo.pages.video.feed

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable

import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.video.type.VideoItem


internal class SideBarView : ComposeView<SideBarViewAttr, SideBarViewEvent>() {

    override fun createEvent(): SideBarViewEvent {
        return SideBarViewEvent()
    }

    override fun createAttr(): SideBarViewAttr {
        return SideBarViewAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                // 固定于视频页面的右侧侧边
                absolutePosition(bottom = SideBarMarginBottom, right = SideBarMarginRight)
                flexDirectionColumn()
                allCenter()
            }

            // 头像及关注
            View {
                attr {
                    size(74f,84f)
                    alignItemsCenter()
                    paddingTop(10f)
                }
                // 头像
                Image {
                    attr {
                        alignSelfCenter()
                        size(50f, 50f)
                        borderRadius(25f)
                        zIndex(0)       // 使得关注图片可浮层显示于头像上方
                        src(ctx.attr.vitem.avatar)
                        opacity(0.88f)
                    }
                }
                View {
                    attr {
                        overflow(true)
                        absolutePosition(bottom = 0f)
                        size(74f, 39f)
                    }
                    // 关注图标
                    Image {
                        attr {
                            alignSelfCenter()
                            size(20f, 20f)
                            borderRadius(10f)
                            zIndex(1)
                            src(FOLLOW_ICON)
                        }
                    }
                }

            }

            // 点赞
            SideBarIcon {
                attr {
                    status = ctx.attr.likeStatus
                    num = ctx.attr.likeNum
                    iconName = "点赞"
                    iconPath = LIKE_ICON
                    iconSelectedPath = LIKED_ICON
                }
            }

            // 评论
            View {
                attr {
                    paddingBottom(17f)
                }
                Button {
                    attr {
                        size(36f, 36f)
                    }
                    event {
                        click {
                            // 回调弹出评论框
                            ctx.event.commentViewClickHandler?.invoke()
                        }
                    }
                    Image {
                        attr {
                            size(36f, 36f)
                            src(COMMENT_ICON)
                        }
                    }
                }
                // 2.2 评论数量显示
                Text {
                    attr {
                        color(Color.WHITE)
                        alignSelfCenter()
                        height(17f)
                        fontSize(11f)
                        text( if (ctx.attr.commentNum == 0) "评论" else ctx.attr.commentNum.toString())
                    }
                }
            }

            // 收藏
            SideBarIcon {
                attr {
                    status = ctx.attr.favouriteStatus
                    num = ctx.attr.favouriteNum
                    iconName = "收藏"
                    iconPath = FAVORITE_ICON
                    iconSelectedPath = FAVORITED_ICON
                }
            }
            // 转发
            SideBarIcon {
                attr {
                    num = ctx.attr.retweetNum
                    iconName = "转发"
                    iconPath = SHARE_ICON
                    iconSelectedPath = SHARE_ICON
                }
            }
        }
    }
}


internal class SideBarViewAttr : ComposeAttr() {

    lateinit var vitem: VideoItem
    var likeNum by observable(0)
    var retweetNum by observable(0)
    var commentNum by observable(0)
    var favouriteNum by observable(0)
    var likeStatus by observable(false)
    var favouriteStatus by observable(false)
}

internal class SideBarViewEvent : ComposeEvent() {
    // 返回外部可监听的评论区按钮点击事件
    var commentViewClickHandler: (() -> Unit)? = null
    fun backCommentViewClick(handler: () -> Unit) {
        commentViewClickHandler = handler
    }
}

internal fun ViewContainer<*, *>.SideBar(init: SideBarView.() -> Unit) {
    addChild(SideBarView(), init)
}
