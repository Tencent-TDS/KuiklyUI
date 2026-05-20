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
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("BridgeCallStressTestPage")
internal class BridgeCallStressTestPage : BasePager() {
    private var resultText by observable("点击按钮开始压力测试")
    private var isRunning by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "Bridge Call 压力测试" } }
            List {
                attr { flex(1f) }

                // 说明区域
                View {
                    attr {
                        margin(left = 16f, top = 16f, right = 16f, bottom = 8f)
                    }
                    Text {
                        attr {
                            text("此页面用于测试 Kotlin 到 Native 的桥接调用性能。\n点击按钮后将循环调用 100,000 次 BridgeManager.callModuleMethod，并统计总耗时。")
                            fontSize(14f)
                            color(Color(0xFF666666))
                        }
                    }
                }

                // 测试按钮
                View {
                    attr {
                        alignItemsCenter()
                        marginTop(20f)
                    }
                    Button {
                        attr {
                            titleAttr {
                                text(if (ctx.isRunning) "测试中..." else "开始压力测试 (100,000次)")
                                color(Color.WHITE)
                            }
                            backgroundColor(if (ctx.isRunning) Color.GRAY else Color(0xFF007AFF))
                            size(width = 280f, height = 48f)
                            borderRadius(8f)
                        }
                        event {
                            click {
                                if (!ctx.isRunning) {
                                    ctx.isRunning = true
                                    ctx.resultText = "测试中，请稍候..."

                                    val begin = DateTime.currentTimestamp()
                                    for (i in 0 until 100000) {
                                        BridgeManager.callModuleMethod(
                                            BridgeManager.currentPageId,
                                            "KRLogModule",
                                            "",
                                            "msg",
                                            null,
                                            1
                                        )
                                    }
                                    val duration = DateTime.currentTimestamp() - begin
                                    KLog.d("callnative time", "it takes $duration milliseconds to call native")
                                    ctx.resultText = "调用 100,000 次 callModuleMethod\n耗时: ${duration} 毫秒"
                                    ctx.isRunning = false
                                }
                            }
                        }
                    }
                }

                // 结果展示
                View {
                    attr {
                        margin(left = 16f, top = 24f, right = 16f, bottom = 16f)
                        backgroundColor(Color(0xFFF5F5F5))
                        borderRadius(8f)
                        padding(all = 16f)
                    }
                    Text {
                        attr {
                            text(ctx.resultText)
                            fontSize(16f)
                            color(Color(0xFF333333))
                        }
                    }
                }
            }
        }
    }
}
