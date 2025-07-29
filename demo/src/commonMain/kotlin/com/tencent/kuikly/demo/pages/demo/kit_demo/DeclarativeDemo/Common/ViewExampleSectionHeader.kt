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

package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.Text

class ExampleSectionHeaderAttr: ComposeAttr() {
    var title = ""
}

internal class ViewExampleSectionHeader: ComposeView<ExampleSectionHeaderAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                backgroundColor(Color(0xFFC9C9C9L))
                height(32f)
                paddingLeft(16f)
                paddingRight(16f)
            }
            Text {
                attr {
                    color(Color.BLACK)
                    fontSize(14f)
                    text(ctx.attr.title)
                }
            }
        }
    }

    override fun createAttr(): ExampleSectionHeaderAttr {
        return ExampleSectionHeaderAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.ViewExampleSectionHeader(init: ViewExampleSectionHeader.() -> Unit) {
    addChild(ViewExampleSectionHeader(), init)
}
