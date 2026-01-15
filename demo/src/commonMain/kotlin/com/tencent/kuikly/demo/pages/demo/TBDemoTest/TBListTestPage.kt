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

package com.tencent.kuikly.demo.pages.demo.TBDemoTest

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.getFirstVisiblePosition
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.TurboDisplayModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * TurboDisplay 测试页面 - List 组件
 * 用于验证 List 的 offset 和 firstVisibleIndex 缓存恢复
 */
@Page("TBListTestPage")
internal class TBListTestPage : BasePager() {

    companion object {
        private const val TAG = "TBListTestPage"
    }

    // 列表数据
    private var dataList: ObservableList<ListItemData> by observableList()

    // 删除次数记录
    private var deleteCount: Int by observable(0)

    // List 的引用（用于设置 offset）
    private var listViewRef: ViewRef<ListView<*, *>>? = null
    
    // 从 pageData 恢复的 offset
    private var restoredOffsetX: Float = 0f
    private var restoredOffsetY: Float = 0f
    private var restoredFirstVisibleIndex: Int = -1
    private var restoredFirstVisibleOffset: Float = 0f
    
    // 当前 List 的 offset（用于缓存）
    private var currentOffsetX: Float = 0f
    private var currentOffsetY: Float = 0f
    private var currentFirstVisibleIndex: Int = 0
    private var currentFirstVisibleOffset: Float = 0f

    // 是否已经恢复过 offset（避免重复恢复）
    private var extraCacheContent = JSONObject()
    private var didRestoreOffset: Boolean = false

    override fun created() {
        super.created()

        // 初始化50个item
        for (i in 0 until 50) {
            dataList.add(ListItemData(i, "Item $i"))
        }
        
        // 从 pageData 中恢复 extraCacheContent
        restoreFromPageData()
        getPager().acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME).clearCurrentPageCache()
    }
    
    private fun restoreFromPageData() {
        val extraCacheContent = getPager().pageData.customFirstScreenTag
        if (extraCacheContent.isNullOrEmpty()) return
        KLog.i(TAG, "【PageData恢复】$extraCacheContent")
        try {
            this.extraCacheContent = JSONObject(extraCacheContent)
        } catch (e: Exception) {
            KLog.e(TAG, "【PageData恢复】解析失败: ${e.message}")
        }
    }

    /**
     * 构建 extraCacheContent JSON
     * 格式：{ "tag": { "viewName": "xxx", "contentOffsetX": x, "contentOffsetY": y, "firstVisibleIndex": i, "firstVisibleOffset": o } }
     */
    private fun buildExtraCacheContent(): String {
        val (index, offset) = listViewRef?.view?.getFirstVisiblePosition() ?: Pair(0, 0f)
        currentFirstVisibleIndex = index
        currentFirstVisibleOffset = offset
        return """{"${listViewRef?.nativeRef}":{"viewName":"KRScrollView","contentOffsetX":$currentOffsetX,"contentOffsetY":$currentOffsetY,"firstVisibleIndex":$currentFirstVisibleIndex,"firstVisibleOffset":$currentFirstVisibleOffset}}"""
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }

            // 导航栏
            NavBar {
                attr {
                    title = "TurboDisplay列表测试"
                }
            }
            getPager().pageData

            // 按钮区域
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceAround()
                    flexWrapNoWrap()
                    padding(16f)
                }

//                getViewWithNativeRef(1).getViewAttr().setPropsToRenderView()

                // 按钮1：删除前5个item
                Button {
                    attr {
                        size(100f, 44f)
                        backgroundColor(Color(0xFF007AFF))
                        borderRadius(8f)
                        margin(4f)
                        titleAttr {
                            text("删除前5项")
                            color(Color.WHITE)
                            fontSize(14f)
                        }
                    }
                    event {
                        click {
                            // 删除前5个item
                            val removeCount = minOf(5, ctx.dataList.size)
                            for (i in 0 until removeCount) {
                                if (ctx.dataList.isNotEmpty()) {
                                    ctx.dataList.removeAt(0)
                                }
                            }
                            ctx.deleteCount++
                        }
                    }
                }

                Button {
                    attr {
                        size(100f, 44f)
                        backgroundColor(Color(0xFFFF5722))
                        borderRadius(8f)
                        margin(4f)
                        titleAttr {
                            text("刷新缓存")
                            color(Color.WHITE)
                            fontSize(14f)
                        }
                    }
                    event {
                        click {
                            val extraContent = ctx.buildExtraCacheContent()
                            KLog.i(TAG, "【手动缓存】$extraContent")
                            ctx.acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                                .setCurrentUIAsFirstScreenForNextLaunch(extraContent)
                        }
                    }
                }

                // 按钮3：清除当前页面缓存
                Button {
                    attr {
                        size(100f, 44f)
                        backgroundColor(Color(0xFFE91E63))
                        borderRadius(8f)
                        margin(4f)
                        titleAttr {
                            text("清除缓存")
                            color(Color.WHITE)
                            fontSize(14f)
                        }
                    }
                    event {
                        click {
                            ctx.acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                                .clearCurrentPageCache()
                        }
                    }
                }

                // 按钮4：清除所有缓存
                Button {
                    attr {
                        size(100f, 44f)
                        backgroundColor(Color(0xFF9C27B0))
                        borderRadius(8f)
                        margin(4f)
                        titleAttr {
                            text("清除全部")
                            color(Color.WHITE)
                            fontSize(14f)
                        }
                    }
                    event {
                        click {
                            ctx.acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                                .clearAllCache()
                        }
                    }
                }
            }

            // 状态提示
            Text {
                attr {
                    margin(16f)
                    fontSize(14f)
                    color(Color.GRAY)
                    text("删除次数: ${ctx.deleteCount}, 剩余项数: ${ctx.dataList.size}, offset: (${ctx.currentOffsetX.toInt()}, ${ctx.currentOffsetY.toInt()})")
                }
            }
            
            // 恢复的 offset 提示
            if (ctx.restoredOffsetY > 0) {
                Text {
                    attr {
                        marginLeft(16f)
                        marginBottom(8f)
                        fontSize(12f)
                        color(Color(0xFF4CAF50))
                        text("已从缓存恢复 offset: (${ctx.restoredOffsetX.toInt()}, ${ctx.restoredOffsetY.toInt()})")
                    }
                }
            }

            // 列表区域
            List {
                ref {
                    ctx.listViewRef = it
                    val cacheProps = ctx.extraCacheContent.toMap()
                    val listPropsValue = cacheProps[this.nativeRef.toString()]
                    if (listPropsValue != null && listPropsValue.toString() != "null") {
                        try {
                            val listProps = JSONObject(listPropsValue.toString()).toMap()
                            ctx.restoredOffsetX = (listProps["contentOffsetX"] as? Number)?.toFloat() ?: 0f
                            ctx.restoredOffsetY = (listProps["contentOffsetY"] as? Number)?.toFloat() ?: 0f
                            ctx.restoredFirstVisibleIndex = (listProps["firstVisibleIndex"] as? Number)?.toInt() ?: -1
                            ctx.restoredFirstVisibleOffset = (listProps["firstVisibleOffset"] as? Number)?.toFloat() ?: 0f
                            KLog.i(TAG, "【恢复】index=${ ctx.restoredFirstVisibleIndex}, offset=(${ctx.restoredOffsetX}, ${ctx.restoredOffsetY})")
                        } catch (e: Exception) {
                            KLog.e(TAG, "【恢复】解析失败: ${e.message}")
                        }
                    }
                    ctx.addTaskWhenPagerUpdateLayoutFinish {
                        if (ctx.restoredFirstVisibleIndex >= 0) {
                            it.view?.scrollToPosition(ctx.restoredFirstVisibleIndex, ctx.restoredFirstVisibleOffset)
                        }
                    }
                }

                attr {
                    flex(1f)
                }
                
                event {
                    scrollEnd {
                        ctx.currentOffsetX = it.offsetX
                        ctx.currentOffsetY = it.offsetY
                        val extraContent = ctx.buildExtraCacheContent()
                        KLog.i(TAG, "【ScrollEnd缓存】$extraContent")
                        getPager().acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                            .setCurrentUIAsFirstScreenForNextLaunch(extraContent)
                    }
                    scroll {
                        ctx.currentOffsetX = it.offsetX
                        ctx.currentOffsetY = it.offsetY
                    }
                }

                vfor({ ctx.dataList }) { item ->
                    View {
                        attr {
                            height(60f)
                            backgroundColor(Color(0xFFF5F5F5))
                            borderRadius(8f)
                            flexDirectionRow()
                            alignItemsCenter()
                        }

                        // 序号
                        View {
                            attr {
                                size(40f, 40f)
                                backgroundColor(Color(0xFF007AFF))
                                borderRadius(20f)
                                allCenter()
                            }
                            Text {
                                attr {
                                    fontSize(16f)
                                    color(Color.WHITE)
                                    text("${item.id}")
                                }
                            }
                        }

                        // 标题
                        Text {
                            attr {
                                marginLeft(16f)
                                fontSize(16f)
                                color(Color.BLACK)
                                text(item.title)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 列表项数据类
     */
    data class ListItemData(
        val id: Int,
        val title: String
    )
}
