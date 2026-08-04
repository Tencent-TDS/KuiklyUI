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

package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Hover
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager

internal class HoverComposeListContainer : ComposeView<ComposeAttr, ComposeEvent>() {
    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun body(): ViewBuilder {
        return {
            List {
                attr {
                    flex(1f)
                    backgroundColor(Color.GRAY)
                }
                Text {
                    attr {
                        marginTop(100f)
                        size(pagerData.pageViewWidth, 2000f)
                        text("Placeholder block for scrolling")
                        fontSize(20f)
                        color(Color.BLACK)
                    }
                }
                Hover {
                    attr {
                        absolutePosition(top = 300f, left = 0f, right = 0f)
                        height(50f)
                        backgroundColor(Color.RED)
                    }
                    Text {
                        attr {
                            size(pagerData.pageViewWidth, 50f)
                            textAlignCenter()
                            lineHeight(50f)
                            text("Hover in ComposeView-wrapped List")
                            color(Color.WHITE)
                            fontSize(14f)
                        }
                    }
                }
                Hover {
                    attr {
                        absolutePosition(top = 600f, left = 0f, right = 0f)
                        height(50f)
                        backgroundColor(Color.BLUE)
                    }
                    Text {
                        attr {
                            size(pagerData.pageViewWidth, 50f)
                            textAlignCenter()
                            lineHeight(50f)
                            text("Second Hover in wrapped List")
                            color(Color.WHITE)
                            fontSize(14f)
                        }
                    }
                }
            }
        }
    }
}

internal fun ViewContainer<*, *>.HoverComposeListContainer(init: HoverComposeListContainer.() -> Unit) {
    addChild(HoverComposeListContainer(), init)
}

@Page("HoverComposeListExamplePage")
internal class HoverComposeListExamplePage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            HoverComposeListContainer {
                attr {
                    flex(1f)
                }
            }
        }
    }
}
