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

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.getFirstVisiblePosition
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
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
import com.tencent.kuikly.demo.pages.base.BridgeModule
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.max
import kotlin.random.Random

@Page("vforlazy")
internal class VforLazyExamplePage : BasePager() {

    private lateinit var inputRef: ViewRef<InputView>
    private lateinit var listRef: ViewRef<ListView<*, *>>
    private val list by observableList<ChatItem>()
    private var bottomSpace by observable(0f)
    private var inputText: String? = null
    private var showPanel by observable(false)
    private var frontAdded = 0

    override fun created() {
        super.created()
        bottomSpace = pageData.safeAreaInsets.bottom
        val tmp = mutableListOf<ChatItem>()
        for (i in 0 until 2000) {
            tmp.add(ChatItem.random(sendType = ChatItem.SEND))
            tmp.add(ChatItem(ChatItem.AUTO_REPLY, ChatItem.RECEIVE))
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
                            flexDirection(if (item.sendType == ChatItem.RECEIVE) FlexDirection.ROW else FlexDirection.ROW_REVERSE)
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
                                src(ImageUri.commonAssets(if (item.sendType == ChatItem.RECEIVE) "penguin2.png" else "panda2.png"))
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
                                if (item.sendType == ChatItem.RECEIVE) {
                                    justifyContentFlexStart()
                                } else {
                                    justifyContentFlexEnd()
                                }
                            }
                            View {
                                attr {
                                    flexDirectionRow()
                                    backgroundColor(if (item.sendType == ChatItem.RECEIVE) 0xFFF3F3F3 else 0xFFA9EA7A)
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
            View {
                attr {
                    absolutePosition(top = ctx.pagerData.statusBarHeight, right = 0f)
                    flexDirectionRow()
                    justifyContentFlexStart()
                }
                View {
                    attr {
                        allCenter()
                        borderRadius(12f, 0f, 12f, 0f)
                        backgroundColor(0x99EEEEEE)
                        size(25f, 25f)
                    }
                    event {
                        click {
                            ctx.showPanel = !ctx.showPanel
                        }
                    }
                    Text {
                        attr {
                            text(if (ctx.showPanel) "→" else "←")
                        }
                    }
                }
                vif({ ctx.showPanel }) {
                    View {
                        attr {
                            width(270f)
                            borderRadius(0f, 10f, 0f, 10f)
                            backgroundColor(0x99EEEEEE)
                        }
                        ctx.debugPanel(this)
                    }
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
        list.add(ChatItem(text, ChatItem.SEND))
        scrollToBottom(true)
        setTimeout(1000) {
            list.add(ChatItem(ChatItem.AUTO_REPLY, ChatItem.RECEIVE))
            scrollToBottom(true)
        }
    }

    private fun scrollToBottom(animate: Boolean) {
        addTaskWhenPagerUpdateLayoutFinish {
            listRef.view?.scrollToPosition(list.size - 1, animate = animate)
        }
    }

    private fun debugPanel(parent: ViewContainer<*, *>) {
        val ctx = this@VforLazyExamplePage
        with(parent) {
            View {
                attr {
                    flexWrapWrap()
                    flexDirectionRow()
                    marginBottom(10f)
                }
                TestCase("goto top") {
                    ctx.listRef.view?.scrollToPosition(0, animate = false)
                }
                TestCase("goto top anim") {
                    ctx.listRef.view?.scrollToPosition(0, animate = true)
                }
                TestCase("goto bottom") {
                    ctx.listRef.view?.scrollToPosition(ctx.list.size - 1, animate = false)
                }
                TestCase("goto bottom anim") {
                    ctx.listRef.view?.scrollToPosition(ctx.list.size - 1, animate = true)
                }
            }
            View {
                attr {
                    flexWrapWrap()
                    flexDirectionRow()
                    marginBottom(10f)
                }
                TestCase("goto 10") {
                    ctx.listRef.view?.scrollToPosition(10, animate = false)
                }
                TestCase("goto 10 anim") {
                    ctx.listRef.view?.scrollToPosition(10, animate = true)
                }
                TestCase("goto 10 - 50dp") {
                    ctx.listRef.view?.scrollToPosition(10, -50f, animate = false)
                }
                TestCase("goto 10 -50dp anim") {
                    ctx.listRef.view?.scrollToPosition(10, -50f, animate = true)
                }
            }
            View {
                attr {
                    flexWrapWrap()
                    flexDirectionRow()
                    marginBottom(10f)
                }
                TestCase("goto 3k") {
                    ctx.listRef.view?.scrollToPosition(3000, animate = false)
                }
                TestCase("goto 3k anim") {
                    ctx.listRef.view?.scrollToPosition(3000, animate = true)
                }
                TestCase("goto 3k - 50dp") {
                    ctx.listRef.view?.scrollToPosition(3000, -50f, animate = false)
                }
                TestCase("goto 3k -50dp anim") {
                    ctx.listRef.view?.scrollToPosition(3000, -50f, animate = true)
                }
            }
            View {
                attr {
                    flexWrapWrap()
                    flexDirectionRow()
                    marginBottom(10f)
                }
                TestCase("goto next 10") {
                    ctx.listRef.view?.scrollToPosition(20, 0f, false)
                    ctx.setTimeout(1000) {
                        ctx.listRef.view?.scrollToPosition(30, 0f, false)
                    }
                }
                TestCase("goto next 20") {
                    ctx.listRef.view?.scrollToPosition(20, 0f, false)
                    ctx.setTimeout(1000) {
                        ctx.listRef.view?.scrollToPosition(40, 0f, false)
                    }
                }
                TestCase("goto 50") {
                    ctx.listRef.view?.scrollToPosition(50, 0f, false)
                }
            }
            View {
                attr {
                    flexWrapWrap()
                    flexDirectionRow()
                }
                TestCase("getFirstVisiblePosition") {
                    val view = ctx.listRef.view ?: return@TestCase
                    val (index, offset) = view.getFirstVisiblePosition()
                    val bridgeModule = ctx.acquireModule<BridgeModule>(BridgeModule.MODULE_NAME)
                    bridgeModule.toast("index=$index, offset=$offset")
                }
                TestCase("front add 50") {
                    val view = ctx.listRef.view ?: return@TestCase
                    ctx.frontAdded += 50
                    val newItems = buildList<ChatItem>(50) {
                        for (i in 0 until 50) {
                            add(ChatItem.random("added front ${ctx.frontAdded - i}"))
                        }
                    }
                    val (index, offset) = view.getFirstVisiblePosition()
                    ctx.list.addAll(0, newItems)
                    ctx.addTaskWhenPagerUpdateLayoutFinish {
                        view.scrollToPosition(index + 50, offset, animate = false)
                    }
                }
            }
        }
    }

}

private class ChatItem(
    val message: String,
    val sendType: SendType
) {
    companion object {
        const val SEND = true
        const val RECEIVE = false
        const val AUTO_REPLY = "你好，我有事不在，稍后再和你联系。"
        val MSG = listOf(
            "在吗？",
            "今天天气真好啊！",
            "你知道吗？我昨天去动物园了。嗯……就是一些常见的动物，没什么特别的。"
        )
        fun random(suffix: String = "", sendType: SendType = Random.nextBoolean()): ChatItem {
            return ChatItem(MSG[Random.nextInt(MSG.size)] + suffix, sendType)
        }
    }
}

private typealias SendType = Boolean

private fun ViewContainer<*, *>.TestCase(title: String, action: () -> Unit) {
    Button {
        attr {
            titleAttr {
                text(title)
                color(Color.WHITE)
            }
            margin(left = 5f, top = 5f)
            padding(5f)
            borderRadius(5f)
            backgroundColor(0x99999999)
        }
        event {
            click {
                action()
            }
        }
    }
}
