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

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.PerformanceModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("PerformancePage")
internal class PerformancePage : BasePager() {

    companion object {
        private const val TAG = "PerformancePage"
    }

    private var performanceData : String by observable("")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar { attr { title = "Performance API" } }
            View {
                attr {
                    padding(5.0f)
                    margin(10.0f)
                    borderRadius(12.0f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFFFB8C00)))
                    allCenter()
                    height(50.0f)
                }
                Text {
                    attr {
                        fontSize(18.0f)
                        color(Color(0xFFFB8C00))
                        text("获取性能数据")
                    }
                }
                event {
                    click {
                        PagerManager.getCurrentPager().acquireModule<PerformanceModule>(PerformanceModule.MODULE_NAME).getPerformanceData {
                            ctx.performanceData = "$it"
                        }
                    }
                }
            }

            Text {
                attr {
                    margin(10.0f)
                    text(ctx.performanceData)
                }
            }
        }
    }
}