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
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.TurboDisplayModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.ScrollerView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * TurboDisplay 测试页面 - Scroller 组件
 * 用于验证 Scroller 的 offset 缓存和恢复
 */
@Page("TBScrollerTestPage")
internal class TBScrollerTestPage : BasePager() {

    companion object {
        private const val TAG = "TBScrollerTestPage"
    }

    private val itemList by observableList<ScrollerItemData>()
    private var scrollerRef: ViewRef<ScrollerView<*, *>>? = null

    // 当前 offset
    private var currentOffsetX by observable(0f)
    private var currentOffsetY by observable(0f)

    // 恢复的 offset
    private var restoredOffsetX = 0f
    private var restoredOffsetY = 0f

    private var extraCacheContent = JSONObject()

    override fun created() {
        super.created()
        // 初始化数据
        for (i in 0 until 50) {
            itemList.add(ScrollerItemData(i, "Scroller Item $i", randomColor()))
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
        } catch (e: Exception) {
            KLog.e(TAG, "【PageData恢复】解析失败: ${e.message}")
        }
    }

    private fun buildExtraCacheContent(): String {
        return """{"${scrollerRef?.nativeRef}":{"viewName":"KRScrollView","contentOffsetX":$currentOffsetX,"contentOffsetY":$currentOffsetY}}"""
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
                    title = "TB Scroller 测试"
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
                    color(Color.GRAY)
                    text("offset: (${ctx.currentOffsetX.toInt()}, ${ctx.currentOffsetY.toInt()})")
                }
            }

            // Scroller 区域
            Scroller {
                ref {
                    ctx.scrollerRef = it
                    val cacheProps = ctx.extraCacheContent.toMap()
                    val scrollerPropsValue = cacheProps[this.nativeRef.toString()]
                    if (scrollerPropsValue != null && scrollerPropsValue.toString() != "null") {
                        try {
                            val props = JSONObject(scrollerPropsValue.toString()).toMap()
                            ctx.restoredOffsetX = (props["contentOffsetX"] as? Number)?.toFloat() ?: 0f
                            ctx.restoredOffsetY = (props["contentOffsetY"] as? Number)?.toFloat() ?: 0f
                            KLog.i(TAG, "【PageData恢复】offset: (${ctx.restoredOffsetX}, ${ctx.restoredOffsetY})")
                        } catch (e: Exception) {
                            KLog.e(TAG, "【PageData恢复】解析失败: ${e.message}")
                        }
                    }
                    ctx.addTaskWhenPagerUpdateLayoutFinish {
                        if (ctx.restoredOffsetY > 0 || ctx.restoredOffsetX > 0) {
                            it.view?.setContentOffset(ctx.restoredOffsetX, ctx.restoredOffsetY, animated = false)
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
                        KLog.i(TAG, "【ScrollEnd自动缓存】$extraContent")
                        getPager().acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                            .setCurrentUIAsFirstScreenForNextLaunch(extraContent)
                    }
                    scroll {
                        ctx.currentOffsetX = it.offsetX
                        ctx.currentOffsetY = it.offsetY
                    }
                }

                vforIndex({ ctx.itemList }) { item, index, _ ->
                    View {
                        attr {
                            height(80f)
                            backgroundColor(item.bgColor)
                            flexDirectionRow()
                            alignItemsCenter()
                            paddingLeft(16f)
                        }
                        View {
                            attr {
                                size(50f, 50f)
                                backgroundColor(Color(0xFF007AFF))
                                borderRadius(25f)
                                allCenter()
                            }
                            Text {
                                attr {
                                    fontSize(18f)
                                    color(Color.WHITE)
                                    text("${item.id}")
                                }
                            }
                        }
                        Text {
                            attr {
                                marginLeft(16f)
                                fontSize(16f)
                                color(Color.WHITE)
                                text(item.title)
                            }
                        }
                    }
                }
            }
        }
    }

    data class ScrollerItemData(
        val id: Int,
        val title: String,
        val bgColor: Color
    )
}
