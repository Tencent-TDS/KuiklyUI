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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Span

/**
 * Created by kam on 2022/7/28.
 */
@Page("HelloWorldPage")
internal class HelloWorldPage : Pager() {

    var dataList: ObservableList<String> by observableList()

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun body(): ViewBuilder {

        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
                autoDarkEnable(false)
            }
            RichText {
                attr {
                    marginTop(30f)
                    lines(3)
                    textOverFlowTail()
                    color(Color.BLACK)
                    fontSize(16f)
//                    lineBreakMargin(20f)
                }
                Span {
                    text("我是第一个文本我是第一个文本")
                }
                Span {
                    color(Color.RED)
                    fontSize(16f)
                    text("这是第二个文本")
                    fontWeightBold()
                    textDecorationLineThrough()
                }
                Span {
                    color(Color.RED)
                    fontSize(16f)
                    text("这是第三个文这是第三个这是第三个文这是第三个这是第三个文这是第三个这是第三个文这是第三个这是第三个文这是第三个这是第三个文这是第三个这是第三个文这是第三个这是第三个文这是第三个")
                    fontWeightMedium()
                    fontStyleItalic()
                    textDecorationUnderLine()
                }
            }

        }
    }
}