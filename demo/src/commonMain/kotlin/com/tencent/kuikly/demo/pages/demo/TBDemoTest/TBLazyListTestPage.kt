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
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.TurboDisplayModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
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
 * TurboDisplay 测试页面 - LazyList (vforLazy)
 * 用于验证懒加载列表的 offset 和 firstVisibleIndex 缓存恢复
 */
@Page("TBLazyListTestPage")
internal class TBLazyListTestPage : BasePager() {

    companion object {
        private const val TAG = "TBLazyListTestPage"
    }

    private var dataList by observableList<LazyListItemData>()
    private var listViewRef: ViewRef<ListView<*, *>>? = null

    // 当前 offset
    private var currentOffsetX by observable(0f)
    private var currentOffsetY by observable(0f)
    private var currentFirstVisibleIndex by observable(0)
    private var currentFirstVisibleOffset by observable(0f)

    // 恢复的数据（方案4：锚点法只需要 firstVisibleIndex）
    private var restoredFirstVisibleIndex = -1

    private var extraCacheContent = JSONObject()

    override fun created() {
        super.created()
        // 初始化数据
        for (i in 0 until 200) {
            dataList.add(LazyListItemData(i, "LazyList Item $i", randomColor()))
        }
        // 恢复缓存
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

    private fun buildExtraCacheContent(): String {
        val (index, offset) = listViewRef?.view?.getFirstVisiblePosition() ?: Pair(0, 0f)
        currentFirstVisibleIndex = index
        currentFirstVisibleOffset = offset
        return """{"${listViewRef?.nativeRef}":{"viewName":"KRScrollView","contentOffsetX":$currentOffsetX,"contentOffsetY":$currentOffsetY,"firstVisibleIndex":$currentFirstVisibleIndex,"firstVisibleOffset":$currentFirstVisibleOffset}}"""
    }

    private fun randomColor(): Color {
        return Color((50..200).random(), (50..200).random(), (50..200).random(), 1.0f)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }

            NavBar {
                attr {
                    title = "TB LazyList 测试"
                }
            }

            // 按钮区域
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceAround()
                    padding(12f)
                }

                Button {
                    attr {
                        size(90f, 40f)
                        backgroundColor(Color(0xFFFF5722))
                        borderRadius(8f)
                        titleAttr {
                            text("刷新缓存")
                            color(Color.WHITE)
                            fontSize(13f)
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

                Button {
                    attr {
                        size(90f, 40f)
                        backgroundColor(Color(0xFFE91E63))
                        borderRadius(8f)
                        titleAttr {
                            text("清除缓存")
                            color(Color.WHITE)
                            fontSize(13f)
                        }
                    }
                    event {
                        click {
                            ctx.acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                                .clearCurrentPageCache()
                        }
                    }
                }
            }

            // 状态提示
            Text {
                attr {
                    margin(12f)
                    fontSize(13f)
                    color(Color.GRAY)
                    text("offset: (${ctx.currentOffsetX.toInt()}, ${ctx.currentOffsetY.toInt()}), firstVisible: ${ if (ctx.currentFirstVisibleIndex == -1) ctx.restoredFirstVisibleIndex else ctx.currentFirstVisibleIndex}")
                }
            }

            // LazyList 区域
            List {
                ref {
                    ctx.listViewRef = it
                    val cacheProps = ctx.extraCacheContent.toMap()
                    val listPropsValue = cacheProps[this.nativeRef.toString()]
                    if (listPropsValue != null && listPropsValue.toString() != "null") {
                        try {
                            val listProps = JSONObject(listPropsValue.toString()).toMap()
                            ctx.restoredFirstVisibleIndex = (listProps["firstVisibleIndex"] as? Number)?.toInt() ?: -1
                            KLog.i(TAG, "【恢复】index=${ctx.restoredFirstVisibleIndex}")
                        } catch (e: Exception) {
                            KLog.e(TAG, "【恢复】解析失败: ${e.message}")
                        }
                    }
                    ctx.addTaskWhenPagerUpdateLayoutFinish {
                        if (ctx.restoredFirstVisibleIndex >= 0) {
                            it.view?.scrollToPosition(ctx.restoredFirstVisibleIndex, 0f)
                            ctx.currentFirstVisibleIndex = ctx.restoredFirstVisibleIndex
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

                vforLazy({ ctx.dataList }) { item, index, count ->
                    View {
                        attr {
                            height(70f)
                            backgroundColor(item.bgColor)
                            flexDirectionRow()
                            alignItemsCenter()
                            paddingLeft(16f)
                        }
                        View {
                            attr {
                                size(45f, 45f)
                                backgroundColor(Color(0xFF4CAF50))
                                borderRadius(22.5f)
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
                        Text {
                            attr {
                                marginLeft(16f)
                                fontSize(15f)
                                color(Color.WHITE)
                                text(item.title)
                            }
                        }
                    }
                }
            }
        }
    }

    data class LazyListItemData(
        val id: Int,
        val title: String,
        val bgColor: Color
    )
}
