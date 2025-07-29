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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.Rotate
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.ImageSpan
import com.tencent.kuikly.core.views.ImageView
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.View

/**
 * Created by kam on 2022/6/22.
 */

internal class CardView : ComposeView<CardData, ComposeEvent>() {

    var cardData: CardData = CardData()
    var imageRef: ViewRef<ImageView>? = null
    var spanTitle: String by observable("富文本第一段2")
    var spanColor: Color by observable(Color.RED)
    var animated: Boolean by observable(false)
    var pHeight by observable(1f)

    class Item {
        var title = ""
        var subTitle = ""
        var detailInfo = ""
        var avatarUrl = ""
        var pictures = arrayListOf<String>()
        var index: Int = 0
    }

    fun header(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    allCenter()
                }
                Image {
                    ref {
                        ctx.imageRef = it
                    }
                    attr {
                        margin(16f, 16f, 16f, 16f)
                        size(50f, 50f)

                        if (ctx.animated) {
                            transform(translate = Translate(6f, 0f), rotate = Rotate(360f))
                        } else {
                            transform(translate = Translate(0f, 0f), rotate = Rotate(0f))
                        }
                        if ( ctx.cardData.item.index == 0) {
                            animation(Animation.easeInOut(1f), ctx.animated)

                        } else if ( ctx.cardData.item.index == 1) {
                            animation(Animation.easeIn(1f), ctx.animated)

                        } else if ( ctx.cardData.item.index == 2) {
                            animation(Animation.easeOut(1f), ctx.animated)

                        } else {
                            animation(Animation.easeInOut(1f), ctx.animated)
                        }
                        borderRadius(25f)
                        border(Border(2f, BorderStyle.SOLID, Color.BLACK))
                        backgroundColor(0xFF999999)
                        resizeCover()
                        src(ctx.cardData.item.avatarUrl)
                    }
                    event {
                        click {
                            ctx.animated = !ctx.animated
                        }

                    }
                }
                Image {
                    attr {
                        src("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg?test=1")
                        width(50f)
                        height(ctx.pHeight)
                    }
                    event {
                        loadResolution {
                            ctx.pHeight = 50f
                            KLog.i("2", "imageWidth: ${it.width}, iamgeHeight:${it.height}")
                        }
                    }

                }
                View {
                    attr {
                        flex(1f)
                        justifyContentCenter()
                    }
                    RichText {
                        Span {
                            fontSize(15f)
                            fontWeightMedium()
                            color(ctx.spanColor)
                            text(ctx.cardData.item.title)
                        }
                    }

                    RichText {
                        attr {
                            marginTop(4f)
                        }
                        Span {
                            fontSize(13f)
                            fontWeightNormal()
                            color(Color.BLACK)
                            value("2022-06-18 15:05")
                        }
                    }
                }
                Image {
                    attr {
                        size(10f, 17f)
                        marginRight(16f)
                        src("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII=")
                    }
                }
            }
        }
    }
    override fun body(): ViewBuilder {
        val ctx = this
        KLog.i("222", "333")
        return {
            attr {
                backgroundColor(Color.WHITE)
                marginBottom(8f)
            }
            event {
                click {
                    ctx.animated = true
                }
            }

            apply(ctx.header())

            View {
                attr {
                    marginLeft(82f)
                    marginRight(16f)
                }
                // richText
                View {
                    RichText {
                        attr {
//                            fontWeightBold()
                            color(Color.BLUE)
                            backgroundColor(Color.YELLOW)

                        }
                        Span {
                            fontSize(15f)
                            color(ctx.spanColor)
                            text(ctx.spanTitle)
                            fontWeightMedium()
                        }
                        ImageSpan {
                            size(60f, 40f)
                            src("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59ef6918.gif")
                            borderRadius(5f)
                        }
                        Span {
                            fontSize(15f)
                            color(Color.BLACK)
                            value("哈哈哈哈哈哈哈哈哈哈哈富文本第二段 index:" + ctx.cardData.item.index)
                            fontWeightBold()
                        }
                    }
                }

                // detailText
                RichText {
                    attr {
                        marginTop(15f)
                        marginRight(50f)

                        marginBottom(16f)
                    }
                    Span {
                        fontSize(14f)
                        fontWeightNormal()
                        color(0xFF999999)
                        value(ctx.cardData.item.detailInfo)
                    }
                }

                // pictures
                if (!ctx.cardData.item.pictures.isEmpty()) {
                    List {
                        attr {
                            flexDirectionRow()
                            height(92f)
                            marginBottom(16f)
                        }
                        ctx.cardData.item.pictures.forEach {
                            Image {
                                attr {
                                    size(92f, 92f)
                                    marginRight(8f)
                                    backgroundColor(0xFF999999)
                                    borderRadius(4f)
                                    src(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun createAttr(): CardData {
        return cardData
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.Card(init: CardView.() -> Unit) {
    addChild(CardView(), init)
}

internal class CardData : ComposeAttr() {
    lateinit var item: CardView.Item
}