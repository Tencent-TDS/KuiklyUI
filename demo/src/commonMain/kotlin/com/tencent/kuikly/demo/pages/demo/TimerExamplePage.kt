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
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.Timer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

@Page("TimerExamplePage")
internal class TimerExamplePage : BasePager() {
    private var remainTime by observable(100)
    private lateinit var timer : Timer

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "Timer"
                }
            }
            ViewExampleSectionHeader {
                attr {
                    title = "TimerExample"
                }
            }
            Text {
                attr {
                    text("time:${ctx.remainTime.toString()}")
                }
                event {
                    click {
                        ctx.timer.cancel()
                    }
                }
            }
        }
    }

    override fun created() {
        super.created()
        timer = Timer()
        timer.schedule(0, 16) {
            remainTime--
            getPager().syncFlushUI()
        }
    }
}