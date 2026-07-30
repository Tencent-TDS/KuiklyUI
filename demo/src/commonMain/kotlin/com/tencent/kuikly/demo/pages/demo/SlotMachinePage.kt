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
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.*
/**
 * 仿老虎机动画
 */
@Page("SlotMachinePage")
internal class SlotMachinePage : Pager() {

    var animationIndex by observable(0)
    var animationLoop by observable(0)

    var imageRef: ImageView? = null

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun body(): ViewBuilder {

        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                allCenter()
            }
            View {
                attr {
                    width(124f)
                    height(124f)
                    overflow(true)
                }
                Image {
                    ctx.imageRef = this
                    attr {
                        width(124f)
                        height(1112f)
                        src(ImageUri.commonAssets("weig.png"))

//                        if (ctx.animationIndex == 0) {
//                            transform(Translate(0f, 0f))
//                            animate(Animation.linear(5f, key = "second").repeatForever(true), value = ctx.animationIndex)
////                            animate(Animation.linear(2f, key = "first"), value = ctx.animationIndex)
//                        } else {
//                            transform(Translate(0f, -0.9f))
//                        }

                        if (ctx.animationIndex == 0) {
                            transform(Translate(0f, 0f))
                            animate(Animation.linear(2f, key = "first"), value = ctx.animationIndex)
                        } else if (ctx.animationIndex == 1) {
                            transform(Translate(0f, -0.3f))
                            animate(Animation.linear(2f, key = "second"), value = ctx.animationIndex)
                        } else if (ctx.animationIndex == 2) {
                            transform(Translate(0f, -0.9f))
                            animate(Animation.linear(0f, key = "third"), value = ctx.animationIndex)
                        } else if (ctx.animationIndex == 3) {
                            transform(Translate(0f, 0.0f))
                            animate(Animation.linear(2f, key = "four").repeatForever(true), value = ctx.animationIndex)
                        } else if (ctx.animationIndex == 4) {
                            transform(Translate(0f, -0.9f))
                            animate(Animation.linear(2f, key = "five"), value = ctx.animationIndex)
                        }else if (ctx.animationIndex == 5) {
                            transform(Translate(0f, -0.6f)) // 根据后台回包来设置值
                            animate(Animation.linear(2f, key = "first"), value = ctx.animationIndex)
                        }
                    }
                    event {
                        animationCompletion {
                            if (it.animationKey == "first") {
                                ctx.animationIndex = 2      // 进入第二阶段
                            }
                            if (it.animationKey == "second") {
                                ctx.animationIndex = 3      // 进入第三阶段
                            }
                            if (it.animationKey == "third") {
                                ctx.animationIndex = 4
                            }
                        }
                    }
                }
            }
            Text {
                attr {
                    text("start")
                    marginTop(50f)
                }
                event {
                    click {
                        ctx.animationIndex = 1
                    }
                }
            }
            Text {
                attr {
                    text("回包")
                    marginTop(50f)
                }
                event {
                    click {
                        ctx.animationIndex = 5
//                        ctx.imageRef?.getViewAttr()?.transform(translate = Translate(0f, 0f))
                    }
                }
            }
        }
    }
}