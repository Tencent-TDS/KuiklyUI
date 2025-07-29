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
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout

@Page("reactive")
internal class ReactiveDemoPage: BasePager() {

    var isSelected by observable(false)
    var pageObservableList by observableList<ListItemExample>()

    override fun viewDidLoad() {
        super.viewDidLoad()
        for (i in 0..2) {
            val item = ListItemExample()
            item.title = "标题 index :" + i
            pageObservableList.add(item)
        }

        for (i in 0..10) {
            setTimeout(i * 1000) {
                if (i > 0) {
                    isSelected = true
                }
                val item = ListItemExample()
                item.title = "标题 index :" + pageObservableList.count()
                pageObservableList.add(item)

                if (i == 9) {
                    pageObservableList = ObservableList()
                    for (i in 0..10) {
                        setTimeout(i * 1000) {

                            val item = ListItemExample()
                            item.title = "标题 index :" + pageObservableList.count()
                            pageObservableList.add(item)

                            if (i == 9) {
                                pageObservableList = ObservableList()

                            }
                        }
                    }
                }
            }
        }
    }
    override fun created() {
        super.created()

    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF3c6cbdL))

            }

            ExampleCard {
                attr {
                    if (ctx.isSelected) {
                        cardObservableList = ctx.pageObservableList
                    }
                }
            }

            vif({ctx.isSelected}) {
                View {
                    attr {
                        size(100f, 100f)
                        backgroundColor(Color.BLUE)
                    }
                }
            }
        }
    }

}

internal class ListItemExample : BaseObject() {
    var title by observable("")

}

internal class ExampleCardAttr : ComposeAttr() {
    var cardObservableList by observableList<ListItemExample>()
}

internal class ExampleCardView : ComposeView<ExampleCardAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {

        val ctx = this
        return {
            attr {
                flex(1f)
            }
            List {
                attr {
                    flex(1f)
                }
                vfor({ctx.attr.cardObservableList}) { item ->
                    Text {
                        attr {
                            margin(20f)
                            backgroundColor(Color.YELLOW)
                            text(item.title)
                            fontSize(20f)
                            color(Color.BLUE)
                        }
                    }
                }
            }
        }
    }

    override fun createAttr(): ExampleCardAttr {
        return ExampleCardAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.ExampleCard(init: ExampleCardView.() -> Unit) {
    addChild(ExampleCardView(), init)
}
