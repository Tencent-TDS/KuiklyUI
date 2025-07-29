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
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("FlatLayoutBugPage")
internal class FlatLayoutBugPage: BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {

            }
            NavBar {
                attr {
                    title = "2222"
                }
            }
            List {
                 attr {
                     flex(1f)
                 }

                View {
                    attr {
                        height(200f)
                      //  backgroundColor(Color.BLUE)

                      //  alignSelfFlexStart()
                        alignItemsFlexStart()
                      //  justifyContentCenter()
                    }
                    View {
                        attr {
                            marginLeft(100f)
                        }
                        FlatCard {
                            attr {

                            }
                        }
                    }

                }
            }
        }
    }
}

internal class FlatCardView : ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {

                attr {
                    absolutePosition(0f, 0f)
                    width(100f)
                    height(100f)
                    backgroundColor(Color.YELLOW)
                }
            }

            event {
                layoutFrameDidChange {
                  //  throwRuntimeError("frame:" + it.toString())
                   // KLog.i("", )
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.FlatCard(init: FlatCardView.() -> Unit) {
    addChild(FlatCardView(), init)
}