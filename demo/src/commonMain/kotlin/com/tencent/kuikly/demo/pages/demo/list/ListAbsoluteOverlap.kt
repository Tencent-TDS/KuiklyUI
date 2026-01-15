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

package com.tencent.kuikly.demo.pages.demo.list

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.coroutines.delay
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.views.layout.Row
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 演示列表中间有两个部分重叠的绝对布局的列表元素
 */
@Page("listOverlap")
internal class ListAbsoluteOverlap : BasePager() {

    private lateinit var listRef: ViewRef<ListView<*, *>>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "列表绝对布局重叠测试"
                }
            }
            Row {
                Button {
                    attr {
                        titleAttr {
                            text("从上往下滚动")
                            color(Color.WHITE)
                        }
                        width(120f)
                        height(40f)
                        margin(10f)
                        borderRadius(10f)
                        backgroundColor(Color(0xFF007AFF))
                        highlightBackgroundColor(Color(0x99FFFFFF))
                    }
                    event {
                        click {
                            val list = ctx.listRef.view ?: return@click
                            ctx.lifecycleScope.launch {
                                list.setContentOffset(0f, 0f, false)
                                delay(500)
                                list.setContentOffset(0f, 900f, true)
                            }
                        }
                    }
                }
                Button {
                    attr {
                        titleAttr {
                            text("从上往下滚动")
                            color(Color.WHITE)
                        }
                        width(120f)
                        height(40f)
                        margin(10f)
                        borderRadius(10f)
                        backgroundColor(Color(0xFF00997A))
                        highlightBackgroundColor(Color(0x99FFFFFF))
                    }
                    event {
                        click {
                            val list = ctx.listRef.view ?: return@click
                            ctx.lifecycleScope.launch {
                                val end = list.contentView!!.frame.height - list.frame.height - 1f
                                list.setContentOffset(0f, end, false)
                                delay(500)
                                list.setContentOffset(0f, 900f, true)
                            }
                        }
                    }
                }
            }
            List {
                ref { ref -> ctx.listRef = ref }

                attr {
                    flex(1f)
                    preloadViewDistance(1f)
                }

                // 列表顶部元素
                for (i in 1..10) {
                    View {
                        attr {
                            height(300f)
                            backgroundColor(if (i % 2 == 0) Color.WHITE else Color.GRAY)
                        }
                        Text {
                            attr {
                                margin(16f)
                                text("列表项 $i")
                            }
                        }
                    }
                }

                // 第一个绝对定位的元素（红色，左上角）
                View {
                    attr {
                        positionAbsolute()
                        top(950f)
                        left(30f)
                        right(100f)
                        height(150f)
                        backgroundColor(Color.RED)
                        allCenter()
                    }
                    Text {
                        attr {
                            text("绝对布局\n元素1")
                            fontSize(20f)
                            color(Color.WHITE)
                        }
                    }
                }

                // 第二个绝对定位的元素（蓝色，与第一个部分重叠）
                View {
                    attr {
                        positionAbsolute()
                        top(1000f)
                        left(100f)
                        right(30f)
                        height(150f)
                        backgroundColor(Color.BLUE)
                        allCenter()
                    }
                    Text {
                        attr {
                            text("绝对布局\n元素2")
                            fontSize(20f)
                            color(Color.WHITE)
                        }
                    }
                }
            }
        }
    }
}
