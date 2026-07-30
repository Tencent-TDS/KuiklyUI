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

package com.tencent.kuikly.demo.pages.debug

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("BugReproColorAlphaToStringPage")
internal class BugReproColorAlphaToStringPage : BasePager() {

    private val testCases by observableList<ColorAlphaTestCase>()

    override fun created() {
        super.created()
        testCases.addAll(
            listOf(
                ColorAlphaTestCase("50% 红", "0x80FF0000", Color(0x80FF0000L)),
                ColorAlphaTestCase("75% 蓝", "0xBF0000FF", Color(0xBF0000FFL)),
                ColorAlphaTestCase("99% 白", "RGBA", Color(255, 255, 255, 0.99f)),
                ColorAlphaTestCase("50% 黑", "RGBA", Color(0, 0, 0, 0.5f)),
                ColorAlphaTestCase("25% 绿", "0x3F00FF00", Color(0x3F00FF00L)),
                ColorAlphaTestCase("不透明红", "0xFFFF0000", Color.RED),
                ColorAlphaTestCase("全透明", "0x00000000", Color.TRANSPARENT),
            )
        )
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(Color(0xFFF0F0F0L))
            }
            NavBar {
                attr {
                    title = "Color Alpha ToString"
                }
            }
            List {
                attr {
                    flex(1f)
                }
                View {
                    attr {
                        padding(all = 12f)
                        flexDirectionColumn()
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeight600()
                            color(Color.BLACK)
                            text("Color.toString() 修复前后对比 (#1101)")
                        }
                    }
                    Text {
                        attr {
                            marginTop(6f)
                            fontSize(12f)
                            color(Color(0xFF666666L))
                            text("重点看 toString 数值：修复前 alpha≥128 时为负数")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(Color(0xFFE65100L))
                            text("Android 色块通常一致：\"2164195328\".toLong().toInt() 与 \"-2130771968\".toLong().toInt() 位模式相同")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(Color(0xFF666666L))
                            text("Web 等平台用 toLong() 不截断 32 位，修复前后 Long 值不同（见下方）")
                        }
                    }
                }
                vfor({ ctx.testCases }) { testCase ->
                    View {
                        attr {
                            margin(left = 12f, right = 12f, bottom = 12f)
                            padding(all = 10f)
                            borderRadius(8f)
                            backgroundColor(Color.WHITE)
                            flexDirectionColumn()
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                fontWeight600()
                                color(Color.BLACK)
                                text("${testCase.name}  (${testCase.argbLabel})")
                            }
                        }
                        Text {
                            attr {
                                marginTop(4f)
                                fontSize(11f)
                                color(Color(0xFF2E7D32L))
                                text("修复后: ${testCase.fixedToString}")
                            }
                        }
                        Text {
                            attr {
                                marginTop(2f)
                                fontSize(11f)
                                color(
                                    if (testCase.hasDifference) Color(0xFFC62828L) else Color(0xFF666666L)
                                )
                                text("修复前: ${testCase.brokenToString}")
                            }
                        }
                        Text {
                            attr {
                                marginTop(2f)
                                fontSize(11f)
                                color(
                                    if (testCase.hasWebParseDifference) Color(0xFFC62828L) else Color(0xFF666666L)
                                )
                                text("Web toLong(): 修复后=${testCase.webFixedLong} / 修复前=${testCase.webBrokenLong}")
                            }
                        }
                        Text {
                            attr {
                                marginTop(2f)
                                fontSize(11f)
                                color(
                                    if (testCase.hasDifference) Color(0xFF666666L) else Color(0xFF2E7D32L)
                                )
                                text(
                                    if (testCase.hasDifference) {
                                        "Android 渲染：左右色块通常相同（见顶部说明）"
                                    } else {
                                        "toString 相同"
                                    }
                                )
                            }
                        }
                        View {
                            attr {
                                marginTop(8f)
                                height(72f)
                                borderRadius(6f)
                                backgroundColor(Color(0xFF2196F3L))
                                flexDirectionRow()
                                padding(all = 6f)
                            }
                            View {
                                attr {
                                    flex(1f)
                                    margin(right = 4f)
                                    flexDirectionColumn()
                                }
                                Text {
                                    attr {
                                        fontSize(10f)
                                        color(Color.WHITE)
                                        text("修复后")
                                    }
                                }
                                View {
                                    attr {
                                        flex(1f)
                                        marginTop(4f)
                                        borderRadius(4f)
                                        backgroundColor(testCase.color)
                                    }
                                }
                            }
                            View {
                                attr {
                                    flex(1f)
                                    margin(left = 4f)
                                    flexDirectionColumn()
                                }
                                Text {
                                    attr {
                                        fontSize(10f)
                                        color(Color.WHITE)
                                        text("修复前(bug)")
                                    }
                                }
                                View {
                                    attr {
                                        flex(1f)
                                        marginTop(4f)
                                        borderRadius(4f)
                                        "backgroundColor" with testCase.brokenToString
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private class ColorAlphaTestCase(
    val name: String,
    val argbLabel: String,
    val color: Color,
) {
    val fixedToString: String
        get() = color.toString()

    /** 模拟 #1101 引入的 bug：对 signed Int 直接 toString */
    val brokenToString: String
        get() = color.hexColor.toInt().toString()

    val hasDifference: Boolean
        get() = fixedToString != brokenToString

    /** Web 侧 String.toColor() fallback 为 toLong()，不做 32 位截断 */
    val webFixedLong: String
        get() = fixedToString.toLongOrNull()?.toString() ?: "parse fail"

    val webBrokenLong: String
        get() = brokenToString.toLongOrNull()?.toString() ?: "parse fail"

    val hasWebParseDifference: Boolean
        get() = webFixedLong != webBrokenLong
}
