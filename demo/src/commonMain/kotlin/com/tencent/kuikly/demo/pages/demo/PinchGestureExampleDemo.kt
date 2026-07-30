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
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("PinchGestureExampleDemo")
internal class PinchGestureExampleDemo : BasePager() {
    private var imageWidth by observable(200f)
    private var imageHeight by observable(200f)
    private var scale by observable(1f)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "PinchGestureExampleDemo"
                }
            }

            View {
                attr {
                    size(ctx.pagerData.pageViewWidth, 200f)
                    backgroundColor(Color.YELLOW)
                    alignItems(FlexAlign.CENTER)
                }
                Image {
                    attr {
                        size(ctx.scale * ctx.imageWidth,ctx.scale * ctx.imageHeight)
                        src("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg")
                    }
                    event {
                        pinch {
                            if (it.state != "end") {
                                ctx.scale = it.scale
                            }
                            else {
                                ctx.imageWidth *= it.scale
                                ctx.imageHeight *= it.scale
                                ctx.scale = 1f
                            }
                        }
                    }
                }

            }
            Text {
                attr {
                    text("Text")
                    fontSize(30f)
                }
            }
        }
    }

    override fun viewDidLayout() {
        super.viewDidLayout()

    }

}
