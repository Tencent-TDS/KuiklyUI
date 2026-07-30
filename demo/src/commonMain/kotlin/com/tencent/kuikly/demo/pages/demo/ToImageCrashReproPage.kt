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
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 复现 KRImageView::LoadFromBase64 中 SIGSEGV crash 的页面。
 *
 * 根因分析：
 * 1. toImage(CACHE_KEY) 截图后，native 层会缓存 ArkUI_DrawableDescriptor* 并返回 cacheKey
 * 2. cacheKey 对应的文件 uri 写入完成前，Image.src 会优先使用缓存中的 drawableDescriptor
 * 3. drawableDescriptor 是从 ArkTS PixelMapDrawableDescriptor NAPI 对象转出的 raw pointer；
 *    如果对象生命周期已经结束或 descriptor 已失效，ArkUI 在 GetPixelMap 时会触发 SIGSEGV
 *
 * 复现方式：
 * - 对一个大 View 截图获取 cacheKey，拖慢异步写文件速度
 * - cacheKey 返回后立刻设置 Image.src，并在 0~100ms 内多次重设
 * - 或者快速连续截图，使多个 cacheKey 的 drawableDescriptor 生命周期交错
 */
@Page("ToImageCrashRepro")
internal class ToImageCrashReproPage : BasePager() {

    companion object {
        private const val TAG = "ToImageCrashRepro"
    }

    private var viewRef: ViewRef<DivView>? = null
    private var imageSrc by observable("")
    private var statusText by observable("点击按钮开始复现")
    private var counter by observable(0)
    private var lastCacheKey by observable("")

    private fun log(message: String) {
        println("[$TAG] $message")
        KLog.i(TAG, message)
    }

    private fun scheduleSetImageSrc(cacheKey: String, delays: IntArray, label: String) {
        delays.forEach { delay ->
            setTimeout(delay) {
                log("$label set Image.src after ${delay}ms, cacheKey=$cacheKey")
                imageSrc = ""
                setTimeout(0) {
                    imageSrc = cacheKey
                    statusText = "$label: ${delay}ms 设置 Image.src"
                }
            }
        }
    }

    private fun snapshotAndSchedule(delays: IntArray, label: String, sampleSize: Int = 1) {
        counter++
        statusText = "$label: 截图中..."
        viewRef?.view?.toImage(DeclarativeBaseView.ImageType.CACHE_KEY, sampleSize) { result ->
            val code = result?.optInt("code") ?: -1
            val data = result?.optString("data") ?: ""
            log("$label toImage result: code=$code, data=$data")
            if (code == 0 && data.isNotEmpty()) {
                lastCacheKey = data
                statusText = "$label: cacheKey已返回，立即设置src"
                imageSrc = data
                scheduleSetImageSrc(data, delays, label)
            } else {
                statusText = "$label: 截图失败 code=$code"
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar { attr { title = "ToImage Crash 复现" } }

            Scroller {
                attr {
                    flex(1f)
                    alignSelfStretch()
                    paddingBottom(30f)
                }

                // 被截图的目标 View：尽量做大并包含多层子节点，拖慢 cacheKey 对应文件落盘
                View {
                    ref { ctx.viewRef = it }
                    attr {
                        margin(10f)
                        padding(10f)
                        height(720f)
                        alignSelfStretch()
                        backgroundColor(Color(0xFF4CAF50))
                        borderRadius(8f)
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            color(Color.WHITE)
                            text("大截图目标区域 counter=${ctx.counter}")
                        }
                    }
                    repeat(18) { index ->
                        View {
                            attr {
                                marginTop(8f)
                                height(28f)
                                alignSelfStretch()
                                backgroundColor(Color(0xFF00695C + index * 0x000505))
                                borderRadius(4f)
                            }
                            Text {
                                attr {
                                    marginLeft(8f)
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("snapshot row $index / counter=${ctx.counter}")
                                }
                            }
                        }
                    }
                }

                // 显示截图结果的 Image
                View {
                    attr {
                        margin(10f)
                        height(150f)
                        allCenter()
                        backgroundColor(Color(0xFFEEEEEE))
                        borderRadius(8f)
                    }
                    Image {
                        attr {
                            size(260f, 130f)
                            src(ctx.imageSrc)
                        }
                    }
                }

                // 状态文本
                Text {
                    attr {
                        margin(10f)
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text(ctx.statusText)
                    }
                }
                Text {
                    attr {
                        marginLeft(10f)
                        marginRight(10f)
                        marginBottom(8f)
                        fontSize(12f)
                        color(Color(0xFF999999))
                        text("lastCacheKey=${ctx.lastCacheKey}")
                    }
                }

                // 方式1：cacheKey 返回后在 URI 替换前多窗口设置 Image.src
                View {
                    attr {
                        margin(10f)
                        padding(15f)
                        allCenter()
                        backgroundColor(Color(0xFFFF5722))
                        borderRadius(8f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            color(Color.WHITE)
                            text("方式1: 返回后0/16/32/64/90ms设置src")
                        }
                    }
                    event {
                        click {
                            ctx.snapshotAndSchedule(intArrayOf(0, 16, 32, 64, 90), "短窗口")
                        }
                    }
                }

                // 方式2：快速连续截图并使用每次返回的 cacheKey
                View {
                    attr {
                        margin(10f)
                        padding(15f)
                        allCenter()
                        backgroundColor(Color(0xFF2196F3))
                        borderRadius(8f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            color(Color.WHITE)
                            text("方式2: 连续8次截图 + 多窗口设置src")
                        }
                    }
                    event {
                        click {
                            ctx.statusText = "连续截图压测中..."
                            repeat(8) { i ->
                                setTimeout(i * 24) {
                                    ctx.snapshotAndSchedule(intArrayOf(0, 16, 48, 96), "连续$i")
                                }
                            }
                        }
                    }
                }

                // 方式3：复用同一个 cacheKey 高频重设 Image.src
                View {
                    attr {
                        margin(10f)
                        padding(15f)
                        allCenter()
                        backgroundColor(Color(0xFF9C27B0))
                        borderRadius(8f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            color(Color.WHITE)
                            text("方式3: 单个cacheKey高频重设40次")
                        }
                    }
                    event {
                        click {
                            ctx.counter++
                            ctx.statusText = "高频重设准备截图中..."
                            ctx.viewRef?.view?.toImage(DeclarativeBaseView.ImageType.CACHE_KEY, 1) { result ->
                                val code = result?.optInt("code") ?: -1
                                val key = result?.optString("data") ?: ""
                                ctx.log("高频重设 toImage result: code=$code, data=$key")
                                if (code == 0 && key.isNotEmpty()) {
                                    ctx.lastCacheKey = key
                                    ctx.imageSrc = key
                                    repeat(40) { i ->
                                        setTimeout(i * 8) {
                                            ctx.log("高频重设[$i], cacheKey=$key")
                                            ctx.imageSrc = ""
                                            setTimeout(0) {
                                                ctx.imageSrc = key
                                                ctx.statusText = "高频重设第${i}次"
                                            }
                                        }
                                    }
                                } else {
                                    ctx.statusText = "高频重设截图失败 code=$code"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
