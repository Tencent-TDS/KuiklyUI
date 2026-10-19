/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Rotate
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Uses the legacy Kuikly DSL animation path to reproduce the same transform endpoints as
 * NativeAnimationDemo case 8.1. The old API cannot express an arbitrary cubic-bezier curve, so
 * EaseOut is the closest behavioral comparison and Linear is provided to isolate transform-path
 * interpolation from the timing curve.
 */
@Page("KuiklyDslTransformAnimationDemo")
internal class KuiklyDslTransformAnimationDemo : BasePager() {
    private var targetRef: ViewRef<DivView>? = null
    private var atEnd = false
    private var status by observable("初态：translate(-5,-3) scale(0.72) rotate(-15°)")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr { title = "Kuikly DSL Transform 对照" }
            }
            View {
                attr {
                    flex(1f)
                    flexDirectionColumn()
                    alignItemsCenter()
                    backgroundColor(Color(0xFFF3F5F8))
                    paddingTop(24f)
                }
                Text {
                    attr {
                        width(pagerData.pageViewWidth - 32f)
                        text(
                            "与 Compose Native 8.1 使用相同起终值和 1 秒时长。" +
                                "旧 DSL 不支持任意 cubic-bezier，因此提供 EaseOut 和 Linear 两种。"
                        )
                        fontSize(14f)
                        color(Color(0xFF444444))
                    }
                }
                View {
                    attr {
                        marginTop(36f)
                        size(150f, 126f)
                        allCenter()
                    }
                    View {
                        ref { ctx.targetRef = it }
                        attr {
                            size(82f, 82f)
                            backgroundColor(Color(0xFF7E57C2))
                            borderRadius(8f)
                            allCenter()
                            transform(
                                rotate = Rotate(angle = -15f),
                                scale = Scale(0.72f, 0.72f),
                                translate = Translate(
                                    percentageX = 0f,
                                    percentageY = 0f,
                                    offsetX = -5f,
                                    offsetY = -3f
                                )
                            )
                        }
                        Text {
                            attr {
                                text("DSL")
                                fontSize(13f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
                Text {
                    attr {
                        marginTop(18f)
                        width(pagerData.pageViewWidth - 32f)
                        text(ctx.status)
                        fontSize(13f)
                        color(Color(0xFF555555))
                        textAlignCenter()
                    }
                }
                View {
                    attr {
                        marginTop(24f)
                        flexDirectionRow()
                        justifyContentCenter()
                    }
                    DemoButton("EaseOut") {
                        ctx.runTransform(Animation.easeOut(1f), "EaseOut")
                    }
                    DemoButton("Linear") {
                        ctx.runTransform(Animation.linear(1f), "Linear")
                    }
                    DemoButton("重置") {
                        ctx.resetTransform()
                    }
                }
                Text {
                    attr {
                        marginTop(24f)
                        width(pagerData.pageViewWidth - 32f)
                        text(
                            "重点观察：点击后是否先向内缩一下；可连续点击测试中断连续性。"
                        )
                        fontSize(13f)
                        color(Color(0xFF777777))
                        textAlignCenter()
                    }
                }
            }
        }
    }

    private fun runTransform(animation: Animation, curveName: String) {
        val targetEnd = !atEnd
        atEnd = targetEnd
        status = "$curveName：${if (targetEnd) "播放到终态" else "播放回初态"}"
        targetRef?.view?.animateToAttr(
            animation = animation,
            attrBlock = {
                transform(
                    rotate = Rotate(angle = if (targetEnd) 70f else -15f),
                    scale = if (targetEnd) Scale(1.18f, 1.18f) else Scale(0.72f, 0.72f),
                    translate = Translate(
                        percentageX = 0f,
                        percentageY = 0f,
                        offsetX = if (targetEnd) 10f else -5f,
                        offsetY = if (targetEnd) 6f else -3f
                    )
                )
            },
            completion = { finished ->
                status = "$curveName：回调 finished=$finished，当前逻辑态=${if (atEnd) "终态" else "初态"}"
            }
        )
    }

    private fun resetTransform() {
        atEnd = false
        targetRef?.view?.animateToAttr(
            animation = Animation.linear(0f),
            attrBlock = {
                transform(
                    rotate = Rotate(angle = -15f),
                    scale = Scale(0.72f, 0.72f),
                    translate = Translate(0f, 0f, offsetX = -5f, offsetY = -3f)
                )
            }
        )
        status = "已重置到初态"
    }
}

private fun com.tencent.kuikly.core.base.ViewContainer<*, *>.DemoButton(
    title: String,
    onClick: () -> Unit
) {
    Text {
        attr {
            marginLeft(6f)
            marginRight(6f)
            width(92f)
            height(42f)
            borderRadius(8f)
            backgroundColor(Color(0xFF1976D2))
            color(Color.WHITE)
            fontSize(14f)
            text(title)
            textAlignCenter()
            lineHeight(42f)
        }
        event {
            click { onClick() }
        }
    }
}
