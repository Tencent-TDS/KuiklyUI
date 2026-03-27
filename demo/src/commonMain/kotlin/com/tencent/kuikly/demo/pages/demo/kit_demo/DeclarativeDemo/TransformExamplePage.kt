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
    var skew by observable(Skew(0f, 0f))
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
                        anchor = ctx.attr.anchor,
                        skew = ctx.attr.skew
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
    private var rotateValueArray: Array<Float> = arrayOf(
        0f, 45f, 90f, 135f, 180f, -135f, -90f, -45f
    )
    private var rotate3DValueArray: Array<Float> = arrayOf(
        0f, 30f, 45f, 60f, 89f, -89f, -60f, -45f, -30f
    )
    private var rotateValueIndex: Int = 0
    private var rotateXValueIndex: Int = 0
    private var rotateYValueIndex: Int = 0

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

    private var skewValueArray: Array<Float> = arrayOf(
        0f, 30f, 45f, 60f, 89f, -89f, -60f, -45f, -30f
    )
    private var skewXValueIndex: Int = 0
    private var skewYValueIndex: Int = 0

    var translate by observable(Translate(percentageX = 0f, percentageY = 0f))
    var rotate by observable(Rotate(angle = 0f))
    var scale by observable(Scale(x = 1f, y = 1f))
    var anchor by observable(Anchor(x = 0.5f, y = 0.5f))
    var skew by observable(Skew(0f, 0f))

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
                        skew = ctx.skew
                    }
                }
            }
            View {
                attr {
                    margin(left = 20f, right = 20f)
                }
                Text {
                    attr {
                        text("Rotate(${ctx.rotate} ${ctx.rotate.toRotateXYString()})\n" +
                                "Scale(${ctx.scale})\n" +
                                "Translate(${ctx.translate})\n" +
                                "Anchor(${ctx.anchor})\n" +
                                "Skew(${ctx.skew})")
                        fontSize(16f)
                    }
                }
            }
            View {
                attr {
                    flexDirectionRow()
                    flexWrapWrap()
                    margin(left = 20f, right = 20f)
                }
                Button {
                    attr {
                        margin(5f)
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
                        margin(5f)
                        backgroundColor(Color(0xFFFFCF3F))
                        borderRadius(20f)
                        size(width = 80f, height = 40f)
                        titleAttr {
                            text("RotateX")
                        }
                    }
                    event {
                        click {
                            ctx.rotateXValueIndex = (ctx.rotateXValueIndex + 1) % ctx.rotate3DValueArray.size
                            ctx.updateTransform()
                        }
                    }
                }
                Button {
                    attr {
                        margin(5f)
                        backgroundColor(Color(0xFFFFCF3F))
                        borderRadius(20f)
                        size(width = 80f, height = 40f)
                        titleAttr {
                            text("RotateY")
                        }
                    }
                    event {
                        click {
                            ctx.rotateYValueIndex = (ctx.rotateYValueIndex + 1) % ctx.rotate3DValueArray.size
                            ctx.updateTransform()
                        }
                    }
                }
                Button {
                    attr {
                        margin(5f)
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
                        margin(5f)
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
                        margin(5f)
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
                            ctx.rotateXValueIndex = 0
                            ctx.rotateYValueIndex = 0
                            ctx.scaleValueIndex = 0
                            ctx.translateValueIndex = 0
                            ctx.skewXValueIndex = 0
                            ctx.skewYValueIndex = 0
                            ctx.anchorValueIndex = (ctx.anchorValueIndex + 1) % ctx.anchorValueArray.size
                            ctx.updateTransform()
                        }
                    }
                }
                Button {
                    attr {
                        margin(5f)
                        backgroundColor(Color(0xFFFFCF3F))
                        borderRadius(20f)
                        size(width = 80f, height = 40f)
                        titleAttr {
                            text("SkewX")
                        }
                    }
                    event {
                        click {
                            ctx.skewXValueIndex = (ctx.skewXValueIndex + 1) % ctx.skewValueArray.size
                            ctx.updateTransform()
                        }
                    }
                }
                Button {
                    attr {
                        margin(5f)
                        backgroundColor(Color(0xFFFFCF3F))
                        borderRadius(20f)
                        size(width = 80f, height = 40f)
                        titleAttr {
                            text("SkewY")
                        }
                    }
                    event {
                        click {
                            ctx.skewYValueIndex = (ctx.skewYValueIndex + 1) % ctx.skewValueArray.size
                            ctx.updateTransform()
                        }
                    }
                }
            }
        }
    }

    private fun updateTransform() {
        rotate = Rotate(rotateValueArray[rotateValueIndex], rotate3DValueArray[rotateXValueIndex], rotate3DValueArray[rotateYValueIndex])
        scale = scaleValueArray[scaleValueIndex]
        translate = translateValueArray[translateValueIndex]
        anchor = anchorValueArray[anchorValueIndex]
        skew = Skew(skewValueArray[skewXValueIndex], skewValueArray[skewYValueIndex])
    }
}