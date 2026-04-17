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
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.PAG
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("PAGApiTestPage")
internal class PAGApiTestPage : BasePager() {

    // ===== 响应式状态 =====
    // 文字替换区域
    private var textIndex by observable(0)
    private var currentText by observable("Hello Kuikly")
    private val textOptions = listOf("Hello Kuikly", "PAG动画测试", "文字已更新!", "KuiklyUI框架")

    // 图片替换区域
    private var useImage by observable(false)

    // 组合替换区域
    private var comboTextIndex by observable(0)
    private var comboUseImage by observable(false)
    private val comboTextOptions = listOf("组合默认文字", "组合替换测试", "动态切换中", "PAG真强大")

    // 图层点击检测区域
    private var layerClickInfo by observable("点击PAG动画区域，查看图层信息 ▶")

    // 状态日志
    private var statusLog by observable("点击按钮开始交互 ▶")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            NavBar {
                attr {
                    title = "PAG Interactive Demo"
                }
            }

            Scroller {
                attr {
                    flex(1f)
                }

                // ===== 状态栏 =====
                View {
                    attr {
                        marginTop(12f)
                        marginLeft(16f)
                        marginRight(16f)
                        backgroundColor(Color(0xFF1A73E8L))
                        borderRadius(8f)
                        padding(12f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            color(Color.WHITE)
                            text(ctx.statusLog)
                        }
                    }
                }

                // ========== 1. replaceTextByIndex - 点击切换文字 ==========
                View {
                    attr {
                        marginTop(20f)
                        marginLeft(16f)
                        marginRight(16f)
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            fontWeightBold()
                            color(Color(0xFF333333L))
                            text("1. 点击切换文字")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(Color(0xFF999999L))
                            text("当前: \"${ctx.currentText}\"")
                        }
                    }
                }

                PAG {
                    attr {
                        marginTop(8f)
                        marginLeft(16f)
                        marginRight(16f)
                        height(200f)
                        backgroundColor(Color(0xFFF5F5F5L))
                        borderRadius(12f)
                        src(ImageUri.pageAssets("user_avatar.pag"))
                        repeatCount(0)
                        autoPlay(true)
                        replaceTextByIndex(0, ctx.currentText)
                    }
                    event {
                        animationStart {
                            KLog.d(TAG, "replaceTextByIndex PAG animationStart")
                        }
                        animationEnd {
                            KLog.d(TAG, "replaceTextByIndex PAG animationEnd")
                        }
                    }
                }

                // 切换文字按钮
                View {
                    attr {
                        marginTop(10f)
                        marginLeft(16f)
                        marginRight(16f)
                        height(44f)
                        backgroundColor(Color(0xFF4CAF50L))
                        borderRadius(22f)
                        justifyContentCenter()
                        alignItemsCenter()
                    }
                    event {
                        click {
                            ctx.textIndex = (ctx.textIndex + 1) % ctx.textOptions.size
                            ctx.currentText = ctx.textOptions[ctx.textIndex]
                            ctx.statusLog = "✅ 文字已切换为: \"${ctx.currentText}\""
                            KLog.d(TAG, "Text switched to: ${ctx.currentText}")
                        }
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            fontWeightBold()
                            color(Color.WHITE)
                            text("点击切换文字 →  下一个: \"${ctx.textOptions[(ctx.textIndex + 1) % ctx.textOptions.size]}\"")
                        }
                    }
                }

                // ========== 2. replaceImageByIndex - 点击切换图片 ==========
                View {
                    attr {
                        marginTop(24f)
                        marginLeft(16f)
                        marginRight(16f)
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            fontWeightBold()
                            color(Color(0xFF333333L))
                            text("2. 点击切换图片")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(Color(0xFF999999L))
                            text(if (ctx.useImage) "当前: 已替换为自定义图片" else "当前: 使用PAG原始图片")
                        }
                    }
                }

                PAG {
                    attr {
                        marginTop(8f)
                        marginLeft(16f)
                        marginRight(16f)
                        height(200f)
                        backgroundColor(Color(0xFFF5F5F5L))
                        borderRadius(12f)
                        src(ImageUri.pageAssets("user_avatar.pag"))
                        repeatCount(0)
                        autoPlay(true)
                        if (ctx.useImage) {
                            replaceImageByIndex(0, ImageUri.pageAssets("user_portrait.png"))
                        }
                    }
                    event {
                        animationStart {
                            KLog.d(TAG, "replaceImageByIndex PAG animationStart")
                        }
                        animationEnd {
                            KLog.d(TAG, "replaceImageByIndex PAG animationEnd")
                        }
                    }
                }

                // 切换图片按钮
                View {
                    attr {
                        marginTop(10f)
                        marginLeft(16f)
                        marginRight(16f)
                        height(44f)
                        backgroundColor(if (ctx.useImage) Color(0xFFFF5722L) else Color(0xFF2196F3L))
                        borderRadius(22f)
                        justifyContentCenter()
                        alignItemsCenter()
                    }
                    event {
                        click {
                            ctx.useImage = !ctx.useImage
                            ctx.statusLog = if (ctx.useImage) "🖼 图片已替换为自定义图片" else "🖼 图片已恢复为PAG原始图片"
                            KLog.d(TAG, "Image toggled: useImage=${ctx.useImage}")
                        }
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            fontWeightBold()
                            color(Color.WHITE)
                            text(if (ctx.useImage) "恢复原始图片" else "替换为自定义图片")
                        }
                    }
                }

                // ========== 3. 组合: 点击分别切换文字和图片 ==========
                View {
                    attr {
                        marginTop(24f)
                        marginLeft(16f)
                        marginRight(16f)
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            fontWeightBold()
                            color(Color(0xFF333333L))
                            text("3. 组合: 文字+图片独立切换")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(Color(0xFF999999L))
                            text("文字: \"${ctx.comboTextOptions[ctx.comboTextIndex]}\" | 图片: ${if (ctx.comboUseImage) "已替换" else "原始"}")
                        }
                    }
                }

                PAG {
                    attr {
                        marginTop(8f)
                        marginLeft(16f)
                        marginRight(16f)
                        height(200f)
                        backgroundColor(Color(0xFFF5F5F5L))
                        borderRadius(12f)
                        src(ImageUri.pageAssets("user_avatar.pag"))
                        repeatCount(0)
                        autoPlay(true)
                        replaceTextByIndex(0, ctx.comboTextOptions[ctx.comboTextIndex])
                        if (ctx.comboUseImage) {
                            replaceImageByIndex(0, ImageUri.pageAssets("user_portrait.png"))
                        }
                    }
                    event {
                        animationStart {
                            KLog.d(TAG, "combo PAG animationStart")
                        }
                        animationEnd {
                            KLog.d(TAG, "combo PAG animationEnd")
                        }
                    }
                }

                // 组合操作按钮行
                View {
                    attr {
                        marginTop(10f)
                        marginLeft(16f)
                        marginRight(16f)
                        flexDirectionRow()
                    }

                    // 切换文字按钮
                    View {
                        attr {
                            flex(1f)
                            height(44f)
                            marginRight(8f)
                            backgroundColor(Color(0xFF9C27B0L))
                            borderRadius(22f)
                            justifyContentCenter()
                            alignItemsCenter()
                        }
                        event {
                            click {
                                ctx.comboTextIndex = (ctx.comboTextIndex + 1) % ctx.comboTextOptions.size
                                ctx.statusLog = "🔤 组合文字切换为: \"${ctx.comboTextOptions[ctx.comboTextIndex]}\""
                                KLog.d(TAG, "Combo text switched to: ${ctx.comboTextOptions[ctx.comboTextIndex]}")
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                fontWeightBold()
                                color(Color.WHITE)
                                text("切换文字")
                            }
                        }
                    }

                    // 切换图片按钮
                    View {
                        attr {
                            flex(1f)
                            height(44f)
                            marginLeft(8f)
                            backgroundColor(if (ctx.comboUseImage) Color(0xFFFF5722L) else Color(0xFFFF9800L))
                            borderRadius(22f)
                            justifyContentCenter()
                            alignItemsCenter()
                        }
                        event {
                            click {
                                ctx.comboUseImage = !ctx.comboUseImage
                                ctx.statusLog = if (ctx.comboUseImage) "🖼 组合图片已替换" else "🖼 组合图片已恢复"
                                KLog.d(TAG, "Combo image toggled: comboUseImage=${ctx.comboUseImage}")
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                fontWeightBold()
                                color(Color.WHITE)
                                text(if (ctx.comboUseImage) "恢复图片" else "替换图片")
                            }
                        }
                    }
                }

                // ========== 4. 点击PAG检测图层 ==========
               View {
                   attr {
                       marginTop(24f)
                       marginLeft(16f)
                       marginRight(16f)
                   }
                   Text {
                       attr {
                           fontSize(16f)
                           fontWeightBold()
                           color(Color(0xFF333333L))
                           text("4. 点击PAG检测图层")
                       }
                   }
                   Text {
                       attr {
                           marginTop(4f)
                           fontSize(12f)
                           color(Color(0xFF999999L))
                           text("点击动画区域，查看命中的图层名称和 editableIndex")
                       }
                   }
               }

               PAG {
                   attr {
                       marginTop(8f)
                       marginLeft(16f)
                       marginRight(16f)
                       height(200f)
                       backgroundColor(Color(0xFFF5F5F5L))
                       borderRadius(12f)
                       src(ImageUri.pageAssets("user_avatar.pag"))
                       repeatCount(0)
                       autoPlay(true)
                   }
                   event {
                       click {
                           val json = it.params as? JSONObject
                           val layersArray = json?.optJSONArray("layers")
                           if (layersArray != null && layersArray.length() > 0) {
                               val sb = StringBuilder()
                               sb.append("命中 ${layersArray.length()} 个图层:\n")
                               for (i in 0 until layersArray.length()) {
                                   val layerObj = layersArray.optJSONObject(i)
                                   val name = layerObj?.optString("layerName") ?: ""
                                   val index = layerObj?.optInt("editableIndex") ?: -1
                                   sb.append("  [$i] name=\"$name\", editableIndex=$index\n")
                               }
                               ctx.layerClickInfo = sb.toString().trimEnd()
                               ctx.statusLog = "🎯 点击检测到 ${layersArray.length()} 个图层"
                               KLog.d(TAG, "Layer hit test: ${sb.toString().trimEnd()}")
                           } else {
                               ctx.layerClickInfo = "未命中任何图层 (x=${it.x}, y=${it.y})"
                               ctx.statusLog = "🎯 点击位置未检测到图层"
                               KLog.d(TAG, "Layer hit test: no layers at (${it.x}, ${it.y})")
                           }
                       }
                   }
               }

               // 图层检测结果展示
               View {
                   attr {
                       marginTop(10f)
                       marginLeft(16f)
                       marginRight(16f)
                       backgroundColor(Color(0xFF263238L))
                       borderRadius(8f)
                       padding(12f)
                   }
                   Text {
                       attr {
                           fontSize(13f)
                           color(Color(0xFF4FC3F7L))
                           text(ctx.layerClickInfo)
                       }
                   }
               }

                // 底部间距
                View {
                    attr {
                        height(60f)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PAGApiTestPage"
    }
}
