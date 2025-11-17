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
import com.tencent.kuikly.core.views.compose.Button

/**
 * SideBarIcon
 * 视频右部悬浮按钮区域内Icon
 */

internal class SideBarIcon: ComposeView<SideBarIconAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                paddingBottom(17f)
            }
            Button {
                attr {
                    size(36f, 36f)
                }
                event {
                    // 随着点击变更收藏数量
                    click {
                        if (ctx.attr.num > 0 && ctx.attr.status) {
                            ctx.attr.status = false
                            ctx.attr.num -= 1
                        } else {
                            ctx.attr.status = true
                            ctx.attr.num += 1
                        }
                    }
                }
                Image {
                    attr {
                        size(36f, 36f)
                        if (ctx.attr.status) {
                            src(ctx.attr.iconSelectedPath)
                        } else {
                            src(ctx.attr.iconPath)
                        }
                        // todo 设置动画
                    }
                }
            }

            Text {
                attr {
                    color(Color.WHITE)
                    alignSelfCenter()
                    height(17f)
                    fontSize(11f)
                    text(if (ctx.attr.num == 0) ctx.attr.iconName else ctx.attr.num.toString())
                }
            }
        }
    }

    override fun createAttr(): SideBarIconAttr {
        return SideBarIconAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal class SideBarIconAttr : ComposeAttr() {
    // 点赞、评论、转发数量
    var num by observable(0)
    var iconName: String = ""
    var status by observable(false)
    var iconPath = ""
    var iconSelectedPath = ""
}


internal fun ViewContainer<*, *>.SideBarIcon(init: SideBarIcon.() -> Unit) {
    addChild(SideBarIcon(), init)
}