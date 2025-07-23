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
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

internal class TransformExampleAttr: ComposeAttr() {
    var translate by observable(Translate(percentageX = 0f, percentageY = 0f))
    var rotate by observable(Rotate(angle = 0f))
    var scale by observable(Scale(x = 1f, y = 1f))
    var anchor by observable(Anchor(x = 0.5f, y = 0.5f))
}

internal class TransformExampleView: ComposeView<TransformExampleAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                alignItemsCenter()
                justifyContentCenter()
            }
            View {
                attr {
                    border(Border(2f, BorderStyle.SOLID, Color.BLACK))
                    backgroundColor(0xFF7FC9FF)
                    size(width = 200f, height = 200f)
                    transform(
                        translate = ctx.attr.translate,
                        rotate = ctx.attr.rotate,
                        scale = ctx.attr.scale,
                        anchor = ctx.attr.anchor
                    )
                    alignItemsCenter()
                    justifyContentCenter()
                }
                Text {
                    attr {
                        text("ExampleView")
                        fontSize(22f)
                    }
                }
                View {
                    attr {
                        backgroundColor(Color.RED)
                        size(width = 10f, height = 10f)
                        borderRadius(5f)
                        absolutePosition(
                            left = 200f * ctx.attr.anchor.toString().split(" ")[0].toFloat() - 5f,
                            top = 200f * ctx.attr.anchor.toString().split(" ")[1].toFloat() - 5f,
                        )
                    }
                }
            }
        }
    }

    override fun createAttr(): TransformExampleAttr {
        return TransformExampleAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.TransformExampleView(init: TransformExampleView.() -> Unit) {
    addChild(TransformExampleView(), init)
}

@Page("TransformExamplePage")
internal class TransformExamplePage: BasePager() {
    private var rotateValueArray: Array<Rotate> = arrayOf(
        Rotate(0f),
        Rotate(45f),
        Rotate(90f),
        Rotate(135f),
        Rotate(180f),
        Rotate(-135f),
        Rotate(-90f),
        Rotate(-45f),
        Rotate(0f),
        Rotate(xAngle = 45f),
        Rotate(xAngle = 90f),
        Rotate(xAngle = 135f),
        Rotate(xAngle = 180f),
        Rotate(xAngle = -135f),
        Rotate(xAngle = -90f),
        Rotate(xAngle = -45f),
        Rotate(xAngle = 0f),
        Rotate(yAngle = 45f),
        Rotate(yAngle = 90f),
        Rotate(yAngle = 135f),
        Rotate(yAngle = 180f),
        Rotate(yAngle = -135f),
        Rotate(yAngle = -90f),
        Rotate(yAngle = -45f),
        Rotate(yAngle = 0f),
    )
    private var rotateValueIndex: Int = 0

    private var scaleValueArray: Array<Scale> = arrayOf(
        Scale(1f, 1f),
        Scale(1.25f, 1f),
        Scale(1.25f, 0.75f),
        Scale(1f, 0.75f)
    )
    private var scaleValueIndex: Int = 0

    private var translateValueArray: Array<Translate> = arrayOf(
        Translate(0f, 0f),
        Translate(0f, 0.3f),
        Translate(0.3f, 0.3f),
        Translate(0.3f, 0f),
        Translate(0.3f, -0.6f),
        Translate(0f, -0.6f),
        Translate(-0.6f, -0.6f),
        Translate(-0.6f, 0f),
    )
    private var translateValueIndex: Int = 0

    private var anchorValueArray: Array<Anchor> = arrayOf(
        Anchor(0.5f, 0.5f),
        Anchor(0.5f, 1f),
        Anchor(1f, 0.5f),
        Anchor(0.5f, 0f),
        Anchor(0f, 0.5f),
    )
    private var anchorValueIndex: Int = 0

    var translate by observable(Translate(percentageX = 0f, percentageY = 0f))
    var rotate by observable(Rotate(angle = 0f))
    var scale by observable(Scale(x = 1f, y = 1f))
    var anchor by observable(Anchor(x = 0.5f, y = 0.5f))

    override fun body(): ViewBuilder {
        var ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            NavBar { attr { title = "View Transform Example" } }
            View {
                attr { flex(1f) }
                TransformExampleView {
                    attr {
                        flex(1f)
                        translate = ctx.translate
                        rotate = ctx.rotate
                        scale = ctx.scale
                        anchor = ctx.anchor
                    }
                }
            }
            View {
                attr {
                    height(80f)
                    margin(left = 20f, right = 20f)
                }
                Text {
                    attr {
                        text("Rotate(${ctx.rotate}, ${ctx.rotate.toRotateXYString()})\n" +
                            "Scale(${ctx.scale})\n" +
                            "Translate(${ctx.translate})\n" +
                            "Anchor(${ctx.anchor})")
                        fontSize(16f)
                    }
                }
            }
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                    justifyContentSpaceAround()
                    height(160f)
                }
                Button {
                    attr {
                        backgroundColor(Color(0xFFFFCF3F))
                        borderRadius(20f)
                        size(width = 80f, height = 40f)
                        titleAttr {
                            text("Rotate")
                        }
                    }
                    event {
                        click {
                            ctx.rotateValueIndex = (ctx.rotateValueIndex + 1) % ctx.rotateValueArray.size
                            ctx.updateTransform()
                        }
                    }
                }
                Button {
                    attr {
                        backgroundColor(Color(0xFFFFCF3F))
                        borderRadius(20f)
                        size(width = 80f, height = 40f)
                        titleAttr {
                            text("Scale")
                        }
                    }
                    event {
                        click {
                            ctx.scaleValueIndex = (ctx.scaleValueIndex + 1) % ctx.scaleValueArray.size
                            ctx.updateTransform()
                        }
                    }
                }
                Button {
                    attr {
                        backgroundColor(Color(0xFFFFCF3F))
                        borderRadius(20f)
                        size(width = 80f, height = 40f)
                        titleAttr {
                            text("Translate")
                        }
                    }
                    event {
                        click {
                            ctx.translateValueIndex = (ctx.translateValueIndex + 1) % ctx.translateValueArray.size
                            ctx.updateTransform()
                        }
                    }
                }
                Button {
                    attr {
                        backgroundColor(Color(0xFFFFCF3F))
                        borderRadius(20f)
                        size(width = 80f, height = 40f)
                        titleAttr {
                            text("Anchor")
                        }
                    }
                    event {
                        click {
                            ctx.rotateValueIndex = 0
                            ctx.scaleValueIndex = 0
                            ctx.anchorValueIndex = (ctx.anchorValueIndex + 1) % ctx.anchorValueArray.size
                            ctx.updateTransform()
                        }
                    }
                }
            }
        }
    }

    private fun updateTransform() {
        rotate = rotateValueArray[rotateValueIndex]
        scale = scaleValueArray[scaleValueIndex]
        translate = translateValueArray[translateValueIndex]
        anchor = anchorValueArray[anchorValueIndex]
    }
}