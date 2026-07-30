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

package com.tme.kuikly

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.views.layout.Column
import com.tencent.kuikly.core.views.layout.Row
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("4444")
internal class AndroidInputBorderBugFixPager : BasePager() {

    lateinit var inputRef: ViewRef<InputView>

    lateinit var decodeRef: ViewRef<InputView>

    var keyboardHeight: Float by observable(0f)

    private var jumpText: String = ""

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(Color.WHITE)
                padding(16f)
            }
            View {
                attr {
                    height(74f)
                    flexDirectionColumn()
                    justifyContentCenter()
                }
                Text {
                    attr {
                        text("Bridge 测试页面")
                        fontSize(32f)
                    }
                }
            }
            Column {
                Text {
                    attr {
                        text("jump")
                        fontSize(26f)
                    }
                }
                Row {
                    attr {
                        alignItemsCenter()
                        marginTop(8f)
                    }
                    Input {
                        ref {
                            ctx.inputRef = it
                        }
                        attr {
                            fontSize(20f)
                            fontWeightBold()
                            flex(1f)
                            height(40f)
                            //  keyboardTypeNumber()
                            returnKeyTypeNext()
                            placeholder("输入 scheme ")
                            placeholderColor(Color.GRAY)
                            color(Color.BLACK)
                            autofocus(true)
                            backgroundColor(Color.WHITE)
                            borderRadius(allBorderRadius = 5f)
                            boxShadow(BoxShadow(10f, 10f, 10f, Color.BLACK))
                            border(Border(0.5f, BorderStyle.SOLID, Color.GRAY))
                        }

                        event {
                            textDidChange {
                                // Utils.logToNative(pagerId, "test_textDidChange" + it.toString())
                                ctx.jumpText = it.text
                            }

                            inputBlur {
                                // Utils.logToNative(pagerId, "test_inputBlur" + it.toString())
                            }

                            inputFocus {
                                // Utils.logToNative(pagerId, "test_inputFocus" + it.toString())
                            }

                            keyboardHeightChange {
                                val height = it.height
                                // Utils.logToNative(pagerId, "test_keyboardHeightChange" + it.toString())
                                ctx.keyboardHeight = height
                            }
                        }
                    }
                    Button {
                        attr {
                            titleAttr {
                                text("jump")
                                fontSize(20f)
                                fontWeightBold()
                                color(Color.WHITE)
                            }
                            marginLeft(8f)
                            height(40f)
                            backgroundColor(Color(68, 105, 43, 1f))
                            borderRadius(allBorderRadius = 5f)
                            paddingLeft(24f)
                            paddingRight(24f)
                        }
                        event {
                            click {
//                                ctx.bridge.jump(ctx.jumpText)
                            }
                        }
                    }
                }
            }
        }
    }
}