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
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.base.resumeGC
import com.tencent.kuikly.demo.pages.base.suspendGC
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.concurrent.Volatile

/**
 * GC 测试数据模型
 */
internal class GCTestItem(
    val title: String,
    val subtitle: String,
    val description: String,
    val imageUrl: String
)

/**
 * GC 测试页面
 * 用于验证不同 GC 配置和内存压力下的滚动性能表现。
 *
 * 功能：
 * - 10000 个 item 的长列表，每个 item 含图片和文本
 * - 每个 item 创建时模拟内存消耗（可调节等级）
 * - 滚动时可选择性暂停/恢复 GC
 * - 控制面板：GC 开关、内存等级、滚到顶部/底部
 */
@Page("GCTestPage")
internal class GCTestPage : BasePager() {

    // 数据列表
    private val dataList by observableList<GCTestItem>()
    // 是否启用 GC suspend/resume
    private var gcSuspendEnabled by observable(true)
    // 内存消耗等级（1-5，每级 10MB）
    private var memoryLevel by observable(1)
    // List 引用，用于滚动控制
    private lateinit var listRef: ViewRef<ListView<*, *>>
    // 防止编译器优化掉内存分配，累加读取值
    @Volatile
    private var memorySink: Int = 0

    companion object {
        private const val ITEM_COUNT = 10000
        // 占位图 URL 列表
        private val IMAGE_URLS = listOf(
            "https://picsum.photos/seed/gc1/160/160",
            "https://picsum.photos/seed/gc2/160/160",
            "https://picsum.photos/seed/gc3/160/160",
            "https://picsum.photos/seed/gc4/160/160",
            "https://picsum.photos/seed/gc5/160/160",
            "https://picsum.photos/seed/gc6/160/160",
            "https://picsum.photos/seed/gc7/160/160",
            "https://picsum.photos/seed/gc8/160/160",
            "https://picsum.photos/seed/gc9/160/160",
            "https://picsum.photos/seed/gc10/160/160"
        )
    }

    override fun created() {
        super.created()
        generateDataList(memoryLevel)
    }

    /**
     * 生成数据列表（仅生成数据，不分配模拟内存）
     */
    private fun generateDataList(level: Int) {
        val items = mutableListOf<GCTestItem>()
        for (i in 0 until ITEM_COUNT) {
            val item = GCTestItem(
                title = "Item #$i",
                subtitle = "Subtitle for item $i - Memory Level: $level",
                description = "This is a description text for item $i. It contains some content to simulate a real list item layout.",
                imageUrl = IMAGE_URLS[i % IMAGE_URLS.size]
            )
            items.add(item)
        }
        dataList.clear()
        dataList.addAll(items)
    }

    /**
     * 模拟内存消耗
     * 在 item 渲染时调用，模拟滚动触发创建新 item 时消耗较多内存的场景。
     * 每级 10MB，用 1MB ByteArray 块，仅分配内存不做 CPU 密集操作。
     * 通过对每个 ByteArray 读写字节并累加到 @Volatile 字段，确保不被编译器优化掉。
     */
    private fun simulateMemoryConsumption(level: Int) {
        val chunkSize = 1 * 1024 * 1024 // 1MB
        val chunkCount = level * 10 // level * 10MB / 1MB
        var sum = 0
        repeat(chunkCount) { idx ->
            val chunk = ByteArray(chunkSize)
            // 写入一个字节，产生副作用
            chunk[idx % chunkSize] = idx.toByte()
            // 读取该字节并累加，防止分配被优化掉
            sum += chunk[idx % chunkSize].toInt()
        }
        // 赋值给 @Volatile 字段，确保编译器无法消除整个分配链
        memorySink = sum
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFFF5F5F5))
            }

            // 导航栏
            NavBar {
                attr {
                    title = "GC Test Page"
                }
            }

            // 控制面板
            View {
                attr {
                    backgroundColor(Color.WHITE)
                    padding(10f)
                }

                // 第一行：GC Suspend 开关 + 内存等级
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        alignItems(FlexAlign.CENTER)
                        marginBottom(8f)
                    }

                    // GC Suspend 开关按钮
                    Button {
                        attr {
                            height(36f)
                            paddingLeft(12f)
                            paddingRight(12f)
                            borderRadius(18f)
                            backgroundColor(if (ctx.gcSuspendEnabled) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                            titleAttr {
                                text("GC Suspend: ${if (ctx.gcSuspendEnabled) "ON" else "OFF"}")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                ctx.gcSuspendEnabled = !ctx.gcSuspendEnabled
                            }
                        }
                    }

                    // 内存等级按钮
                    Button {
                        attr {
                            height(36f)
                            paddingLeft(12f)
                            paddingRight(12f)
                            borderRadius(18f)
                            marginLeft(10f)
                            backgroundColor(Color(0xFF2196F3))
                            titleAttr {
                                text("内存压力: Lv.${ctx.memoryLevel} (${ctx.memoryLevel * 10}MB)")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                // 循环切换 1→2→3→4→5→1
                                ctx.memoryLevel = (ctx.memoryLevel % 5) + 1
                                ctx.generateDataList(ctx.memoryLevel)
                            }
                        }
                    }
                }

                // 第二行：滚到顶部 + 滚到底部
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        alignItems(FlexAlign.CENTER)
                    }

                    // 滚到顶部按钮
                    Button {
                        attr {
                            height(36f)
                            paddingLeft(12f)
                            paddingRight(12f)
                            borderRadius(18f)
                            backgroundColor(Color(0xFFFF9800))
                            titleAttr {
                                text("⬆ 滚到顶部")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                ctx.listRef.view?.scrollToPosition(0, animate = true)
                            }
                        }
                    }

                    // 滚到底部按钮
                    Button {
                        attr {
                            height(36f)
                            paddingLeft(12f)
                            paddingRight(12f)
                            borderRadius(18f)
                            marginLeft(10f)
                            backgroundColor(Color(0xFFFF5722))
                            titleAttr {
                                text("⬇ 滚到底部")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                ctx.listRef.view?.scrollToPosition(
                                    ctx.dataList.size - 1,
                                    animate = true
                                )
                            }
                        }
                    }
                }
            }

            // 列表区域
            List {
                ref { ctx.listRef = it }
                attr {
                    flex(1f)
                    backgroundColor(Color(0xFFF5F5F5))
                }
                event {
                    dragBegin {
                        if (ctx.gcSuspendEnabled) {
                            suspendGC()
                        }
                    }
                    scrollEnd {
                        if (ctx.gcSuspendEnabled) {
                            resumeGC()
                        }
                    }
                }

                vforLazy({ ctx.dataList }) { item, index, _ ->
                    // 每个 item 创建时模拟内存消耗
                    ctx.simulateMemoryConsumption(ctx.memoryLevel)

                    // 每个 item 的布局
                    View {
                        attr {
                            flexDirection(FlexDirection.ROW)
                            backgroundColor(Color.WHITE)
                            marginLeft(10f)
                            marginRight(10f)
                            marginTop(if (index == 0) 10f else 5f)
                            marginBottom(5f)
                            borderRadius(8f)
                            padding(10f)
                            alignItems(FlexAlign.CENTER)
                        }

                        // 左侧图片
                        Image {
                            attr {
                                size(80f, 80f)
                                borderRadius(8f)
                                backgroundColor(Color(0xFFE0E0E0))
                                src(item.imageUrl)
                            }
                        }

                        // 右侧文本区域
                        View {
                            attr {
                                flex(1f)
                                marginLeft(12f)
                            }

                            // 标题
                            Text {
                                attr {
                                    text(item.title)
                                    fontSize(16f)
                                    fontWeightBold()
                                    color(Color(0xFF212121))
                                }
                            }

                            // 副标题
                            Text {
                                attr {
                                    text(item.subtitle)
                                    fontSize(13f)
                                    color(Color(0xFF757575))
                                    marginTop(4f)
                                }
                            }

                            // 描述
                            Text {
                                attr {
                                    text(item.description)
                                    fontSize(12f)
                                    color(Color(0xFF9E9E9E))
                                    marginTop(4f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
