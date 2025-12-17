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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.random.Random

/**
 * 验证observable关联更新，导致的vfor重复渲染问题
 */
@Page("vfor_mod")
internal class VforModify : BasePager() {

    private var obj by observable(0)
    private val list by observableList<String>()

    override fun willInit() {
        super.willInit()
        list.add("🚩")
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "验证vfor关联更新场景"
                }
            }
            Button {
                attr {
                    size(100f, 40f)
                    borderRadius(20f)
                    marginLeft(2f)
                    marginRight(15f)
                    backgroundLinearGradient(
                        Direction.TO_BOTTOM,
                        ColorStop(Color(0xAA23D3FD), 0f),
                        ColorStop(Color(0xAAAD37FE), 1f)
                    )
                    titleAttr {
                        text("item +1")
                        fontSize(17f)
                        color(Color.WHITE)
                    }
                }
                event {
                    click {
                        ctx.list.add(
                            ctx.list.size - 1,
                            Random.nextInt(1000, 9999).toString()
                        )
                    }
                }
            }
            View {
                attr {
                    ctx.obj = ctx.list.size
                }
            }
            View {
                attr {
                    flexDirectionRow()
                    flexWrapWrap()
                }
                vforIndex({ ctx.obj; ctx.list }) { item, index, _ ->
                    View {
                        attr {
                            margin(10f)
                            padding(10f)
                            borderRadius(10f)
                            backgroundColor(Color.GRAY)
                        }
                        Text { attr { text("#$index $item") } }
                    }
                }
            }
            List {
                attr {
                    height(200f)
                }
                vforLazy({ ctx.obj; ctx.list }) { item, index, _ ->
                    View {
                        attr {
                            padding(10f)
                            backgroundColor(if (index % 2 == 0) Color.GRAY else Color.WHITE)
                        }
                        Text { attr { text("Lazy List - #$index $item") } }
                    }
                }
            }
        }
    }
}