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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Mask
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("mask_demo")
internal class MaskDemoPage : BasePager() {

    private var maskSize by observable(200f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }

            Scroller {
                attr {
                    flex(1f)
                    flexDirectionColumn()
                }

                // 页面标题
                Text {
                    attr {
                        marginTop(50f)
                        marginLeft(16f)
                        fontSize(18f)
                        color(Color.BLACK)
                        text("Mask 遮罩组件测试")
                    }
                }

                // 1. 网络图片遮罩
                Text {
                    attr {
                        marginTop(20f)
                        marginLeft(16f)
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text("1. 图片为遮罩，view为内容")
                    }
                }
                Mask(
                    {
                        attr { size(351f, 400f); allCenter() }
                        Image {
                            attr {
                                size(351f, 400f)
                                resizeStretch()
                                src("https://tianquan.gtimg.cn/shoal/qqvip/f3cb664e-b2cb-451a-9c00-f8cb9d8d302e.png")
                            }
                        }
                    },
                    {
                        View {
                            attr { size(351f, 400f); backgroundColor(Color.BLUE) }
                        }
                    }
                )

                // 2. 本地图片遮罩
                Text {
                    attr {
                        marginTop(20f)
                        marginLeft(16f)
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text("2. 图片为遮罩和内容")
                    }
                }
                Mask(
                    {
                        attr { size(200f, 200f); allCenter() }
                        Image {
                            attr {
                                size(200f, 200f)
                                resizeStretch()
                                src(ImageUri.commonAssets("panda.png"))
                            }
                        }
                    },
                    {
                        Image {
                            attr {
                                size(200f, 200f)
                                resizeStretch()
                                src("https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg")
                            }
                        }
                    }
                )

                // 3. 圆形遮罩
                Text {
                    attr {
                        marginTop(20f)
                        marginLeft(16f)
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text("3. View为遮罩，图片为内容")
                    }
                }
                Mask(
                    {
                        attr { size(150f, 150f); allCenter() }
                        View {
                            attr {
                                size(150f, 150f)
                                backgroundColor(Color.BLACK)
                                borderRadius(75f)
                            }
                        }
                    },
                    {
                        Image {
                            attr {
                                size(150f, 150f)
                                resizeStretch()
                                src("https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg")
                            }
                        }
                    }
                )

                // 4. 文字遮罩 - 1
                Text {
                    attr {
                        marginTop(20f)
                        marginLeft(16f)
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text("4. Text为遮罩，View为内容")
                    }
                }
                Mask(
                    {
                        attr { size(300f, 100f); allCenter() }
                        Text {
                            attr {
                                fontSize(60f)
                                color(Color.BLACK)
                                text("MASK")
                            }
                        }
                    },
                    {
                        View {
                            attr {
                                size(300f, 100f)
                                backgroundLinearGradient(
                                    Direction.TO_RIGHT,
                                    ColorStop(Color.RED, 0f),
                                    ColorStop(Color.YELLOW, 0.5f),
                                    ColorStop(Color.GREEN, 1f)
                                )
                            }
                        }
                    }
                )
                // 5. 文字遮罩 - 2
                Text {
                    attr {
                        marginTop(20f)
                        marginLeft(16f)
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text("5. Text为遮罩，image为内容")
                    }
                }
                Mask(
                    {
                        attr { size(300f, 100f); allCenter() }
                        Text {
                            attr {
                                fontSize(60f)
                                color(Color.BLACK)
                                text("MASK")
                            }
                        }
                    },
                    {
                        Image {
                            attr {
                                size(300f, 100f)
                                resizeStretch()
                                src("https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg")
                            }
                        }
                    }
                )


                // 6. 渐变透明遮罩
                Text {
                    attr {
                        marginTop(20f)
                        marginLeft(16f)
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text("6. 渐变透明背景色View遮罩，图片为内容")
                    }
                }
                Mask(
                    {
                        attr { size(200f, 200f); allCenter() }
                        View {
                            attr {
                                size(200f, 200f)
                                backgroundLinearGradient(
                                    Direction.TO_BOTTOM,
                                    ColorStop(Color(0xFF000000), 0f),
                                    ColorStop(Color(0x00000000), 1f)
                                )
                            }
                        }
                    },
                    {
                        Image {
                            attr {
                                size(200f, 200f)
                                resizeStretch()
                                src("https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg")
                            }
                        }
                    }
                )


                // 6. 动态尺寸
                Text {
                    attr {
                        marginTop(20f)
                        marginLeft(16f)
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text("6. View嵌套遮罩&View尺寸可点击变化")
                    }
                }
                View {
                    attr {
                        height(220f)
                        alignItemsCenter()
                        justifyContentCenter()
                    }
                    event {
                        click { ctx.maskSize = if (ctx.maskSize == 200f) 150f else 200f }
                    }
                    Mask(
                        {
                            attr { size(ctx.maskSize, ctx.maskSize); allCenter() }
                            View {
                                attr {
                                    size(ctx.maskSize, ctx.maskSize)
                                    backgroundColor(Color.BLACK)
                                    borderRadius(ctx.maskSize / 2)
                                }
                            }
                        },
                        {
                            View {
                                attr {
                                    size(ctx.maskSize, ctx.maskSize)
                                    backgroundColor(Color.RED)
                                }
                            }
                        }
                    )
                }


                // 底部间距
                View { attr { height(50f) } }
            }
        }
    }
}