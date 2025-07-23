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
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.PAG
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ExampleSectionHeaderAttr
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

@Page("PAGExamplePage")
internal class PAGExamplePage : BasePager() {
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            NavBar { attr { title = "PAG Demo" } }
            List {
                attr { flex(1f) }
                ViewExampleSectionHeader { attr { title = "Load file from Assets" } }
                PAG {
                    attr {
                        height(200f)
                        src(ImageUri.pageAssets("user_avatar.pag"))
                        repeatCount(0)
                    }
                }

                ViewExampleSectionHeader { attr { title = "Replace Image & Text" } }
                PAG {
                    attr {
                        height(200f)
                        src(ImageUri.pageAssets("user_avatar.pag"))
                        repeatCount(0)
                        replaceTextLayerContent("text_user_note", "Kuikly!")
                        replaceImageLayerContent("img_user_avatar", ImageUri.pageAssets("user_portrait.png"))
                    }
                }

                ViewExampleSectionHeader { attr { title = "To be continue... " } }
            }
        }
    }
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
