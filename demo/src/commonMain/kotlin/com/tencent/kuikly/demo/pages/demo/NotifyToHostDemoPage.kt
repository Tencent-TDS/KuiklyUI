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
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * Demo: Kuikly 向 Web 宿主发送事件
 * 
 * Web 宿主监听方式 (已在 h5App/Main.kt 中实现):
 * window.addEventListener('kuikly_notify', (e) => console.log(e.detail))
 */
@Page("NotifyToHostDemo")
internal class NotifyToHostDemoPage : BasePager() {

    private var sendCount: Int by observable(0)

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
                padding(20f)
            }

            Text {
                attr {
                    fontSize(20f)
                    fontWeightBold()
                    color(Color.BLACK)
                    text("Kuikly -> Web Host 事件示例")
                    marginBottom(20f)
                }
            }

            Text {
                attr {
                    fontSize(14f)
                    color(Color(0xFF666666))
                    text("点击按钮发送事件，请打开浏览器控制台查看")
                    marginBottom(20f)
                }
            }

            // 发送按钮
            View {
                attr {
                    backgroundColor(Color(0xFF007AFF))
                    borderRadius(8f)
                    padding(12f)
                    alignSelfCenter()
                }
                event {
                    click {
                        ctx.sendCount++
                        val data = JSONObject().apply {
                            put("message", "Hello from Kuikly!")
                            put("count", ctx.sendCount)
                        }
                        ctx.acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
                            .postNotify("kuikly_to_host_event", data)
                    }
                }

                Text {
                    attr {
                        fontSize(16f)
                        color(Color.WHITE)
                        text("发送事件")
                    }
                }
            }

            Text {
                attr {
                    fontSize(14f)
                    color(Color(0xFF007AFF))
                    text("已发送: ${ctx.sendCount} 次")
                    marginTop(16f)
                    alignSelfCenter()
                }
            }
        }
    }
}
