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
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

expect fun dumpMemory()

@Page("MemoryDumpExamplePage")
internal class MemoryDumpExamplePage: BasePager() {
    val data = mutableListOf("")
    fun leak(){
        for (i in  data.size ..  data.size + 1000){
            data.add("The lazy brown frog jump over a fox $i".repeat(1000))
        }
    }
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "Memory Dump Example Page" } }
            View {
                attr {
                    flexDirectionColumn()
                    alignItemsCenter()
                    justifyContentSpaceAround()
                }
                Button {
                    attr {
                        titleAttr {
                            text("Create And Retain 1000 Items")
                        }
                        highlightBackgroundColor(Color(0xFFFFFA00))
                        backgroundColor(Color(0xFFFFD700))
                        size(width = 300f, height = 60f)
                    }
                    event {
                        click {
                            ctx.leak()
                        }
                    }
                }
                Button {
                    attr {
                        titleAttr {
                            text("Create And Retain 1000 Items")
                        }
                        highlightBackgroundColor(Color(0xFF00EFFF))
                        backgroundColor(Color(0xFF00BFFF))
                        size(width = 300f, height = 60f)
                    }
                    event {
                        click {
                            ctx.leak()
                        }
                    }
                }
                Button {
                    attr {
                        titleAttr {
                            text("Dump Memory")
                        }
                        highlightBackgroundColor(Color(0xFFE9E9FA))
                        backgroundColor(Color(0xFFE6E6FA))
                        size(width = 300f, height = 60f)
                    }
                    event {
                        click {
                            dumpMemory()
                        }
                    }
                }
            }
        }
    }
}
