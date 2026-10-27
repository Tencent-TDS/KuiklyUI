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

package com.tencent.kuikly.demo.pages.fold

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 页面只声明标题、返回和操作。安全区与遮挡由 FoldChrome 处理。
 */
@Page("FoldAppNav")
internal class FoldAppNavPage : BasePager() {
    var note by observable("还没点过分享")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flex(1f)
                backgroundColor(FoldPalette.bg)
            }
            FoldChrome(
                title = "自定义Nav",
                onBack = {
                    ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                },
                actions = listOf(
                    FoldNavAction("分享", "square.and.arrow.up") {
                        ctx.note = "刚把这段路分享出去"
                    },
                ),
            ) {
                Scroller {
                    attr {
                        flex(1f)
                        paddingTop(20f)
                    }
                    View {
                        attr {
                            paddingLeft(16f)
                            paddingRight(16f)
                            paddingBottom(16f)
                        }
                        Text {
                            attr {
                                text("傍晚的江面")
                                fontSize(22f)
                                fontWeightSemiBold()
                                color(FoldPalette.ink)
                            }
                        }
                        Text {
                            attr {
                                marginTop(6f)
                                text("外白渡桥到金陵东路，慢慢走就好")
                                fontSize(14f)
                                color(FoldPalette.tertiary)
                            }
                        }
                        foldParagraph("风从江面贴着栏杆过来。楼群的灯还没全亮，桥洞里先看见一艘很小的船。")
                        foldParagraph("再往前，海关大楼的钟和沿江的石库门挨在一起。人不多的时候，这一段适合看，不适合赶。")
                        foldParagraph("走到金陵东路，天色通常已经暗下来。回头还能看见刚才那一排窗户，灯光贴着江水，路就短了一截。")
                        View {
                            attr {
                                marginTop(16f)
                                padding(16f)
                                borderRadius(16f)
                                backgroundColor(FoldPalette.card)
                            }
                            factLine(this, "起点", "外白渡桥", top = 0f)
                            factLine(this, "终点", "金陵东路")
                            factLine(this, "时长", "四十分钟")
                            factLine(this, "光线", "灯先于天黑")
                        }
                        Text {
                            attr {
                                marginTop(16f)
                                text(ctx.note)
                                fontSize(13f)
                                lineHeight(18f)
                                color(FoldPalette.tertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun factLine(host: ViewContainer<*, *>, label: String, value: String, top: Float = 10f) {
    host.View {
        attr {
            marginTop(top)
            flexDirectionRow()
            alignItemsCenter()
        }
        Text {
            attr {
                width(52f)
                text(label)
                fontSize(14f)
                color(FoldPalette.tertiary)
            }
        }
        Text {
            attr {
                flex(1f)
                text(value)
                fontSize(15f)
                color(FoldPalette.ink)
            }
        }
    }
}
