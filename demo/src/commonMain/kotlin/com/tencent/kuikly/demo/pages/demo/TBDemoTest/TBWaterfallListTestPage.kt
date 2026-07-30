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
    private lateinit var waterfallListRef: ViewRef<WaterfallListView>

    // 从 pageData 恢复的 offset
    private var restoredOffsetX: Float = 0f
    private var restoredOffsetY: Float = 0f
    private var restoredFirstVisibleIndex: Int = 0
    private var restoredFirstVisibleOffset: Float = 0f

    // 缓存、恢复时服用的Columns属性
    private var listColumns by observable(1)

    // 当前 offset
    private var currentOffsetX by observable(0f)
    private var currentOffsetY by observable(0f)
    private var currentFirstVisibleIndex by observable(0)
    private var currentFirstVisibleOffset by observable(0f)


    private var extraCacheContent = JSONObject()

    override fun created() {
        super.created()
        KLog.e(TAG, "【时序】created() 开始执行")
        // 初始化数据
        for (i in 0 until 100) {
            dataList.add(WaterfallItemData(this).apply {
                title = "瀑布流 $i"
                if (i%3 == 0) {
                    height = 350f
                } else if (i%5 == 0) {
                    height = 250f
                } else {
                    height = 150f
                }
                bgColor = randomColor()
            })
        }
        KLog.e(TAG, "【时序】数据初始化完成，共 ${dataList.size} 条")
        // 恢复缓存
        restoreFromPageData()


        if (extraCacheContent.keySet().size > 0) {
            KLog.i("当前extraContent", extraCacheContent.toString())
            var key = extraCacheContent.keySet().toList()[0].toString()
            val listPropsValue = extraCacheContent.toMap().get(key)
            if (listPropsValue != null && listPropsValue.toString() != "null") {
                try {
                    // 嵌套JSONObject，再解析一层
                    val listProps = JSONObject(listPropsValue.toString()).toMap()
                    restoredOffsetX = (listProps["contentOffsetX"] as? Number)?.toFloat() ?: 0f
                    restoredOffsetY = (listProps["contentOffsetY"] as? Number)?.toFloat() ?: 0f
                    restoredFirstVisibleIndex = (listProps["firstVisibleIndex"] as? Number)?.toInt() ?: 0
                    restoredFirstVisibleOffset = (listProps["firstVisibleOffset"] as? Number)?.toFloat() ?: 0f
                    listColumns = (listProps["columns"] as? Number)?.toInt() ?: 1
                    KLog.e(TAG, "【时序】缓存解析成功: restoredOffsetY=$restoredOffsetY, restoredFirstVisibleIndex=$restoredFirstVisibleIndex")
                } catch (e: Exception) {
                    KLog.e(TAG, "【恢复】解析失败: ${e.message}")
                }

                addTaskWhenPagerUpdateLayoutFinish {
                    getPager().addNextTickTask {
                        if (restoredFirstVisibleIndex >= 0) {
                            KLog.e(
                                TAG,
                                "【执行setContentOffset】offsetX: ${restoredOffsetX}  offsetY: ${restoredOffsetY}"
                            )
                            waterfallListRef.view?.setContentOffset(
                                offsetX = restoredOffsetX,
                                offsetY = restoredOffsetY
                            )
                            currentFirstVisibleIndex = restoredFirstVisibleIndex
                        }
                    }
                }

            }
        }

    }

    // 读取端侧发送过来的强制缓存的内容【公共的写法，没有问题】
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
        val (index, offset) = waterfallListRef?.view?.getFirstVisiblePosition() ?: Pair(0, 0f)
        currentFirstVisibleIndex = index
        currentFirstVisibleOffset = offset
        return """{"${waterfallListRef?.nativeRef}":{"viewName":"KRScrollView","contentOffsetX":$currentOffsetX,"contentOffsetY":$currentOffsetY,"firstVisibleIndex":$currentFirstVisibleIndex,"firstVisibleOffset":$currentFirstVisibleOffset,"columns":$listColumns}}"""
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
                            text("增加一列")
                            color(Color.WHITE)
                            fontSize(13f)
                        }
                    }
                    event {
                        click {
                            ctx.listColumns += 1
                        }
                    }
                }

                Button {
                    attr {
                        size(90f, 40f)
                        backgroundColor(Color(0xFFE91E63))
                        borderRadius(8f)
                        titleAttr {
                            text("减少一列")
                            color(Color.WHITE)
                            fontSize(13f)
                        }
                    }
                    event {
                        click {
                            ctx.listColumns -= 1
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
                    KLog.i(TAG,"Text 输出 offsetX: ${ctx.currentOffsetX}  offsetY: ${ctx.currentOffsetY}")
                    text("offset: (${ctx.currentOffsetX.toInt()}, ${ctx.currentOffsetY.toInt()}), firstVisible: ${ctx.currentFirstVisibleIndex}")
                }
            }

            // WaterfallList 区域
            WaterfallList {
                ref {
                    ctx.waterfallListRef = it
                }
                // @tips：构建自定义缓存时，只需要存储发生交互的用户属性或事件驱动发生变化的属性
                attr {
                    flex(1f)
                    columnCount(ctx.listColumns)        // 交互属性，需要存储
                    listWidth(pagerData.pageViewWidth)
                    lineSpacing(8f)
                    itemSpacing(8f)
                    contentPadding(8f, 8f, 8f, 8f)
                }

                event {
                    contentSizeChanged { width, height ->
                        KLog.e(TAG, "【时序】contentSizeChanged: width=$width, height=$height, currentOffsetY=${ctx.currentOffsetY}")
                    }
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
                        KLog.i(TAG, "【时序】scroll: offsetX=${it.offsetX}, offsetY=${it.offsetY}, contentHeight=${it.contentHeight}")
                    }
                }

                vforIndex({ ctx.dataList }) { item, index, _ ->
                    KLog.i(TAG, "【Item创建】index=$index, title=${item.title}, height=${item.height}")
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