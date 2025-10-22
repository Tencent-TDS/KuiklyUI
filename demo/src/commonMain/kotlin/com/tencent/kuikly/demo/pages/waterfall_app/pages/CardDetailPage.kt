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

package com.tencent.kuikly.demo.pages.waterfall_app.pages

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.demo.pages.waterfall_app.models.WaterFallItem
import com.tencent.kuikly.core.log.KLog

/**
 * 卡片详情页面组件
 */
internal class CardDetailPage(
    private val item: WaterFallItem,
    private val pageViewWidth: Float
) : ComposeView<CardDetailPageAttr, CardDetailPageEvent>() {

    private var likeCount by observable((100..9999).random())
    private var collectCount by observable((50..999).random())
    
    override fun createEvent(): CardDetailPageEvent {
        return CardDetailPageEvent()
    }

    override fun createAttr(): CardDetailPageAttr {
        return CardDetailPageAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flex(1f)
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }

            // 顶部导航栏
            View {
                attr {
                    height(88f)
                    backgroundColor(Color.TRANSPARENT)
                    flexDirectionRow()
                    alignItemsCenter()
                    justifyContentSpaceBetween()
                    paddingTop(44f)
                    paddingLeft(16f)
                    paddingRight(16f)
                }

                // 返回按钮
                View {
                    attr {
                        width(32f)
                        height(32f)
                        backgroundColor(Color.WHITE)
                        borderRadius(16f)
                        allCenter()
                    }

                    Text {
                        attr {
                            text("<")
                            fontSize(18f)
                            color(Color.BLACK)
                        }
                    }

                    event {
                        click {
                            ctx.event.onBackClick?.invoke()
                        }
                    }
                }


            }

            // 主内容区域
            Scroller {
                attr {
                    flex(1f)
                    backgroundColor(Color.WHITE)
                }

                View {
                    attr {
                        flexDirectionColumn()
                        alignItemsCenter()
                    }

                    // 主图片
                    Image {
                        attr {
                            src(ctx.item.imageUrl)
                            width(ctx.pageViewWidth)
                            height((ctx.item.imageHeight / ctx.item.imageWidth) * ctx.pageViewWidth)
                            borderRadius(0f)
                        }
                    }

                    // 内容区域
                    View {
                        attr {
                            width(ctx.pageViewWidth)
                            backgroundColor(Color.WHITE)
                            borderRadius(20f, 20f, 0f, 0f)
                            padding(20f)
                            flexDirectionColumn()
                            marginTop(-20f)
                        }

                        // 用户信息区域
                        View {
                            attr {
                                height(50f)
                                flexDirectionRow()
                                alignItemsCenter()
                                justifyContentSpaceBetween()
                                marginBottom(16f)
                            }

                            // 左侧用户信息
                            View {
                                attr {
                                    flexDirectionRow()
                                    alignItemsCenter()
                                    flex(1f)
                                }

                                // 用户头像
                                Image {
                                    attr {
                                        width(40f)
                                        height(40f)
                                        src(ctx.item.userAvatar)
                                        borderRadius(20f)
                                    }
                                }

                                View {
                                    attr {
                                        flexDirectionColumn()
                                        marginLeft(12f)
                                        flex(1f)
                                    }

                                    // 用户昵称
                                    Text {
                                        attr {
                                            text(ctx.item.userNick)
                                            fontSize(16f)
                                            color(Color.BLACK)
                                            fontWeightBold()
                                        }
                                    }

                                    // 发布时间
                                    Text {
                                        attr {
                                            text("2小时前")
                                            fontSize(12f)
                                            color(Color(0xFF999999))
                                            marginTop(2f)
                                        }
                                    }
                                }
                            }

                            // 关注按钮
                            View {
                                attr {
                                    width(60f)
                                    height(32f)
                                    backgroundColor(Color(0xFFFF2442))
                                    borderRadius(16f)
                                    allCenter()
                                }

                                Text {
                                    attr {
                                        text("关注")
                                        fontSize(14f)
                                        color(Color.WHITE)
                                    }
                                }

                                event {
                                    click {
                                        KLog.i("CardDetailPage", "点击关注按钮")
                                    }
                                }
                            }
                        }

                        // 内容文字
                        Text {
                            attr {
                                text(ctx.item.content)
                                fontSize(16f)
                                color(Color.BLACK)
                                lineHeight(24f)
                                marginBottom(20f)
                            }
                        }

                        // 标签区域
                        View {
                            attr {
                                flexDirectionRow()
                                marginBottom(20f)
                            }

                            // 示例标签
                            listOf("生活", "美食", "分享").forEach { tag ->
                                View {
                                    attr {
                                        backgroundColor(Color(0xFFF5F5F5))
                                        borderRadius(12f)
                                        marginRight(8f)
                                        marginBottom(8f)
                                    }

                                    Text {
                                        attr {
                                            text("#$tag")
                                            fontSize(14f)
                                            color(Color(0xFF666666))
                                        }
                                    }
                                }
                            }
                        }

                        // 互动数据
                        View {
                            attr {
                                flexDirectionRow()
                                alignItemsCenter()
                                marginBottom(40f)
                            }

                            Text {
                                attr {
                                    text("${ctx.likeCount}次点赞 · ${ctx.collectCount}次收藏")
                                    fontSize(14f)
                                    color(Color(0xFF999999))
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

internal class CardDetailPageAttr : ComposeAttr()

internal class CardDetailPageEvent : ComposeEvent() {
    var onBackClick: (() -> Unit)? = null
}

internal fun ViewContainer<*, *>.CardDetailPage(
    item: WaterFallItem,
    pageViewWidth: Float,
    init: CardDetailPage.() -> Unit
) {
    addChild(CardDetailPage(item, pageViewWidth), init)
}