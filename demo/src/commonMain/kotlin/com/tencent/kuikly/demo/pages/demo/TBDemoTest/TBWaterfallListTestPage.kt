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
import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.getFirstVisiblePosition
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.TurboDisplayModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.WaterfallList
import com.tencent.kuikly.core.views.WaterfallListView
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * TurboDisplay 测试页面 - WaterfallList 组件
 * 用于验证瀑布流列表的 offset 缓存和恢复
 */
@Page("TBWaterfallListTestPage")
internal class TBWaterfallListTestPage : BasePager() {

    companion object {
        private const val TAG = "TBWaterfallListTestPage"
    }

    private var dataList by observableList<WaterfallItemData>()
    private var waterfallListRef: ViewRef<WaterfallListView>? = null

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
        for (i in 0 until 100) {
            dataList.add(WaterfallItemData(this).apply {
                title = "瀑布流 $i"
                height = (150..350).random().toFloat()
                bgColor = randomColor()
            })
        }
        // 恢复缓存
        restoreFromPageData()
        getPager().acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME).clearCurrentPageCache()
    }

    private fun restoreFromPageData() {
        val extraCacheContent = getPager().pageData.customFirstScreenTag
        if (extraCacheContent.isNullOrEmpty()) {
            KLog.i(TAG, "【PageData恢复】无 extraCacheContent")
            return
        }
        KLog.i(TAG, "【PageData恢复】extraCacheContent=$extraCacheContent")
        try {
            this.extraCacheContent = JSONObject(extraCacheContent)
            // 方案4：锚点法只需要解析 firstVisibleIndex
            restoredFirstVisibleIndex = this.extraCacheContent.optInt("firstVisibleIndex", -1)
        } catch (e: Exception) {
            KLog.e(TAG, "【PageData恢复】解析失败: ${e.message}")
        }
    }

    private fun buildExtraCacheContent(): String {
        // 关键：使用 firstVisibleIndex 而非 contentOffset 来恢复位置
        val (index, offset) = waterfallListRef?.view?.getFirstVisiblePosition() ?: Pair(0, 0f)
        currentFirstVisibleIndex = index
        currentFirstVisibleOffset = offset
        return """{"${waterfallListRef?.nativeRef}":{"viewName":"KRScrollView","contentOffsetX":$currentOffsetX,"contentOffsetY":$currentOffsetY},"firstVisibleIndex":$currentFirstVisibleIndex,"firstVisibleOffset":$currentFirstVisibleOffset}"""
    }

    private fun randomColor(): Color {
        return Color((50..200).random(), (50..200).random(), (50..200).random(), 1.0f)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF3c6cbd))
                flexDirectionColumn()
            }

            NavBar {
                attr {
                    title = "TB WaterfallList 测试"
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
                            KLog.i(TAG, "【手动刷新缓存】$extraContent")
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
                    color(Color.WHITE)
                    text("offset: (${ctx.currentOffsetX.toInt()}, ${ctx.currentOffsetY.toInt()}), firstVisible: ${ctx.currentFirstVisibleIndex}")
                }
            }

            // WaterfallList 区域
            WaterfallList {
                ref {
                    ctx.waterfallListRef = it
                    // 方案4：锚点法恢复 - 只依赖 firstVisibleIndex，不依赖 offset
                    ctx.addTaskWhenPagerUpdateLayoutFinish {
                        if (ctx.restoredFirstVisibleIndex >= 0) {
                            KLog.i(TAG, "【恢复】锚点法 scrollToPosition: index=${ctx.restoredFirstVisibleIndex}, offset=0")
                            it.view?.scrollToPosition(ctx.restoredFirstVisibleIndex, 0f)
                            // 关键修复：恢复后立即更新 currentFirstVisibleIndex，避免直接退出时缓存丢失
                            ctx.currentFirstVisibleIndex = ctx.restoredFirstVisibleIndex
                            // 恢复后重新缓存当前状态
                            val extraContent = ctx.buildExtraCacheContent()
                            KLog.i(TAG, "【恢复后立即缓存】$extraContent")
                            ctx.acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                                .setCurrentUIAsFirstScreenForNextLaunch(extraContent)
                        }
                    }
                }

                attr {
                    flex(1f)
                    columnCount(2)
                    listWidth(pagerData.pageViewWidth)
                    lineSpacing(8f)
                    itemSpacing(8f)
                    contentPadding(8f, 8f, 8f, 8f)
                }

                event {
                    scrollEnd {
                        ctx.currentOffsetX = it.offsetX
                        ctx.currentOffsetY = it.offsetY
                        val extraContent = ctx.buildExtraCacheContent()
                        KLog.i(TAG, "【ScrollEnd自动缓存】$extraContent")
                        getPager().acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                            .setCurrentUIAsFirstScreenForNextLaunch(extraContent)
                    }
                    scroll {
                        ctx.currentOffsetX = it.offsetX
                        ctx.currentOffsetY = it.offsetY
                    }
                }

                vforIndex({ ctx.dataList }) { item, index, _ ->
                    View {
                        attr {
                            height(item.height)
                            backgroundColor(item.bgColor)
                            borderRadius(8f)
                            allCenter()
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color.WHITE)
                                text(item.title)
                            }
                        }
                    }
                }
            }
        }
    }

    class WaterfallItemData(scope: PagerScope) {
        var title by scope.observable("")
        var height by scope.observable(200f)
        var bgColor by scope.observable(Color.WHITE)
    }
}
