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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.VforLazyExamplePage.ChatItem.Companion.AUTO_REPLY
import com.tencent.kuikly.demo.pages.demo.VforLazyExamplePage.ChatItem.Companion.MSG
import com.tencent.kuikly.demo.pages.demo.VforLazyExamplePage.ChatItem.Companion.RECEIVE
import com.tencent.kuikly.demo.pages.demo.VforLazyExamplePage.ChatItem.Companion.SEND
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.max
import kotlin.random.Random

@Page("vforlazy")
internal class VforLazyExamplePage : BasePager() {

    private class ChatItem(
        val message: String,
        val sendType: SendType
    ) {
        companion object {
            const val SEND = true
            const val RECEIVE = false
            const val AUTO_REPLY = "你好，我有事不在，稍后再和你联系。"
            val MSG = listOf("在吗？", "今天天气真好啊！", "你知道吗？我昨天去动物园了。嗯……就是一些常见的动物，没什么特别的。")
        }
    }

    private lateinit var inputRef: ViewRef<InputView>
    private lateinit var listRef: ViewRef<ListView<*, *>>
    private val list by observableList<ChatItem>()
    private var bottomSpace by observable(0f)
    private var inputText: String? = null

    override fun created() {
        super.created()
        bottomSpace = pageData.safeAreaInsets.bottom
        val tmp = mutableListOf<ChatItem>()
        for (i in 0 until 2000) {
            tmp.add(ChatItem(MSG[Random.nextInt(MSG.size)], SEND))
            tmp.add(ChatItem(AUTO_REPLY, RECEIVE))
        }
        list.addAll(tmp)
        scrollToBottom(false)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    backDisable = false
                    title = "🟢 在线"
                }
            }
            List {
                ref {
                    ctx.listRef = it
                }
                attr {
                    flex(1f)
                }
                vforLazy({ ctx.list }) { item, index, count ->
                    View {
                        attr {
                            flexDirection(if (item.sendType == RECEIVE) FlexDirection.ROW else FlexDirection.ROW_REVERSE)
                            alignItemsFlexStart()
                            margin(10f)
                            padding(10f)
                        }
                        Text {
                            attr {
                                absolutePosition(right = 0f)
                                color(Color.RED)
                                fontSize(9f)
                                text(index.toString())
                            }
                        }
                        Image {
                            attr {
                                src(ImageUri.commonAssets(if (item.sendType == RECEIVE) "penguin2.png" else "panda2.png"))
                                size(45f, 45f)
                                borderRadius(22.5f)
                                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                            }
                        }
                        View {
                            attr {
                                margin(left = 10f, right = 10f)
                                flex(1f)
                                flexDirectionRow()
                                if (item.sendType == RECEIVE) {
                                    justifyContentFlexStart()
                                } else {
                                    justifyContentFlexEnd()
                                }
                            }
                            View {
                                attr {
                                    flexDirectionRow()
                                    backgroundColor(if (item.sendType == RECEIVE) 0xFFF3F3F3 else 0xFFA9EA7A)
                                    padding(10f)
                                    borderRadius(10f)
                                }
                                Text {
                                    attr {
                                        text(item.message)
                                        maxWidth(ctx.pageData.pageViewWidth - 120f)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            View {
                attr {
                    flexDirectionRow()
                    margin(10f)
                }
                View {
                    attr {
                        margin(left = 10f, right = 10f)
                        height(40f)
                        flex(1f)
                        borderRadius(5f)
                    }
                    View {
                        attr {
                            absolutePositionAllZero()
                            backgroundLinearGradient(
                                Direction.TO_LEFT,
                                ColorStop(Color(0xFF23D3FD), 0f),
                                ColorStop(Color(0xFFAD37FE), 1f)
                            )
                        }
                        View {
                            attr {
                                absolutePosition(top = 1f, left = 1f, right = 1f, bottom = 1f)
                                backgroundColor(Color.WHITE)
                                borderRadius(5f)
                            }
                        }
                    }
                    Input {
                        ref {
                            ctx.inputRef = it
                        }
                        attr {
                            flex(1f)
                            fontSize(15f)
                            color(Color(0xFFAD37FE))
                            marginLeft(10f)
                            marginRight(10f)
                        }
                        event {
                            textDidChange {
                                ctx.inputText = it.text
                            }
                            inputReturn {
                                ctx.send()
                            }
                            keyboardHeightChange {
                                ctx.bottomSpace = max(it.height, ctx.pageData.safeAreaInsets.bottom)
                            }
                        }
                    }
                }
                Button {
                    attr {
                        size(80f, 40f)
                        borderRadius(20f)
                        marginLeft(2f)
                        marginRight(10f)
                        backgroundLinearGradient(
                            Direction.TO_BOTTOM,
                            ColorStop(Color(0xAA23D3FD), 0f),
                            ColorStop(Color(0xAAAD37FE), 1f)
                        )

                        titleAttr {
                            text("发送")
                            fontSize(17f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.send()
                        }
                    }
                }

            }
            View {
                attr {
                    height(ctx.bottomSpace)
                }
            }
        }
    }

    private fun send() {
        val text = inputText
        if (text.isNullOrEmpty()) {
            return
        }
        inputRef.view?.setText("")
        list.add(ChatItem(text, SEND))
        scrollToBottom(true)
        setTimeout(1000) {
            list.add(ChatItem(AUTO_REPLY, RECEIVE))
            scrollToBottom(true)
        }
    }

    private fun scrollToBottom(animate: Boolean) {
        addTaskWhenPagerUpdateLayoutFinish {
            listRef.view?.scrollToPosition(list.size - 1, animate = animate)
        }
    }
}

private typealias SendType = Boolean