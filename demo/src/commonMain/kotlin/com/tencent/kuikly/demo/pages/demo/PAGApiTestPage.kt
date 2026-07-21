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
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
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

    // 点击图层后，通过 observable 驱动 replaceByIndex 的交互示例
    private var clickReplaceTextStep by observable(-1)
    private var clickReplaceImageStep by observable(-1)
    private var clickReplaceTextEditableIndex by observable(-1)
    private var clickReplaceImageEditableIndex by observable(-1)
    private var clickReplaceTextContent by observable("点击 PAG 中的文字后会替换")
    private val clickReplaceTextOptions = listOf(
        "点击文字后已替换",
        "再次点击文字继续切换",
        "click 驱动 replaceTextByIndex",
        "observable 已重新下发"
    )
    private val clickReplaceImageAssets = listOf("cat1.png", "panda.png", "penguin2.png")

    // 图层点击检测区域
    private var layerClickInfo by observable("请直接点击 PAG 内的文字或图片本体 ▶")

    // 状态日志
    private var statusLog by observable("点击按钮或 PAG 本体开始交互 ▶")

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

                // ========== 4. 点击 PAG 本体后，通过 observable 执行 replace ==========
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
                            text("4. 点击 PAG 内文字/图片直接替换")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(Color(0xFF999999L))
                            text("请直接点击 PAG 中可编辑的文字或图片本体。")
                        }
                    }
                    Text {
                        attr {
                            marginTop(2f)
                            fontSize(12f)
                            color(Color(0xFF999999L))
                            text("click 会返回 layerName、editableIndex 与 editableType。")
                        }
                    }
                    Text {
                        attr {
                            marginTop(2f)
                            fontSize(12f)
                            color(Color(0xFF999999L))
                            text("业务按 editableType + editableIndex 写入 observable，再由 attr 中的 replace*ByIndex 响应式下发。")
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
                        if (ctx.clickReplaceTextEditableIndex >= 0) {
                            replaceTextByIndex(ctx.clickReplaceTextEditableIndex, ctx.clickReplaceTextContent)
                        }
                        if (ctx.clickReplaceImageEditableIndex >= 0 && ctx.clickReplaceImageStep >= 0) {
                            replaceImageByIndex(
                                ctx.clickReplaceImageEditableIndex,
                                ImageUri.commonAssets(ctx.clickReplaceImageAssets[ctx.clickReplaceImageStep])
                            )
                        }
                    }
                    event {
                        click { it ->
                            val json = it.params as? JSONObject
                            val layersJson = json?.optString("layers", "[]") ?: "[]"
                            KLog.d(TAG, "interactive click layersJson=$layersJson")
                            val layersArray = try {
                                JSONArray(layersJson)
                            } catch (e: Exception) {
                                KLog.e("pagTest", "Failed to parse layers json: $layersJson")
                                JSONArray()
                            }
                            if (layersArray.length() > 0) {
                                var handled = false
                                val sb = StringBuilder()
                                sb.append("命中 ${layersArray.length()} 个图层:\n")
                                for (i in 0 until layersArray.length()) {
                                    val layerObj = layersArray.optJSONObject(i)
                                    val name = layerObj?.optString("layerName") ?: ""
                                    val index = layerObj?.optInt("editableIndex") ?: -1
                                    val editableType = layerObj?.optString("editableType") ?: ""
                                    sb.append("  [$i] type=\"$editableType\", name=\"$name\", editableIndex=$index\n")
                                    if (handled || index < 0) {
                                        continue
                                    }
                                    when (editableType) {
                                        "text" -> {
                                            val nextTextStep = (ctx.clickReplaceTextStep + 1) % ctx.clickReplaceTextOptions.size
                                            ctx.clickReplaceTextStep = nextTextStep
                                            ctx.clickReplaceTextEditableIndex = index
                                            ctx.clickReplaceTextContent = ctx.clickReplaceTextOptions[nextTextStep]
                                            ctx.statusLog = "📝 点击文字图层后，已把 editableIndex=$index 替换为 \"${ctx.clickReplaceTextContent}\""
                                            handled = true
                                            KLog.d(TAG, "click replace text, editableType=$editableType, layerName=$name, editableIndex=$index, text=${ctx.clickReplaceTextContent}")
                                        }

                                        "image" -> {
                                            val nextImageStep = (ctx.clickReplaceImageStep + 1) % ctx.clickReplaceImageAssets.size
                                            ctx.clickReplaceImageStep = nextImageStep
                                            ctx.clickReplaceImageEditableIndex = index
                                            ctx.statusLog = "🖼 点击图片图层后，已把 editableIndex=$index 替换为 ${ctx.clickReplaceImageAssets[nextImageStep]}"
                                            handled = true
                                            KLog.d(TAG, "click replace image, editableType=$editableType, layerName=$name, editableIndex=$index, asset=${ctx.clickReplaceImageAssets[nextImageStep]}")
                                        }
                                    }
                                }
                                ctx.layerClickInfo = sb.toString().trimEnd()
                                if (!handled) {
                                    ctx.statusLog = "🎯 已拿到图层，但当前示例未识别 editableType"
                                    KLog.d(TAG, "interactive click not handled: ${sb.toString().trimEnd()}")
                                } else {
                                    KLog.d(TAG, "interactive click handled: ${sb.toString().trimEnd()}")
                                }
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
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(Color(0xFFB0BEC5L))
                            text(
                                "响应式状态：textIndex=${if (ctx.clickReplaceTextEditableIndex >= 0) ctx.clickReplaceTextEditableIndex else "未命中"} -> \"${ctx.clickReplaceTextContent}\" | imageIndex=${if (ctx.clickReplaceImageEditableIndex >= 0) ctx.clickReplaceImageEditableIndex else "未命中"} -> ${if (ctx.clickReplaceImageStep >= 0) ctx.clickReplaceImageAssets[ctx.clickReplaceImageStep] else "未替换"}"
                            )
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
