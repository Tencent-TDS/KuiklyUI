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
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.TurboDisplayModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * TurboDisplay 测试页面 - 轮播图 (Banner/Carousel)
 * 用于验证 Banner 的 pageIndex 缓存恢复
 * 
 * 缓存策略：
 * - 端侧（iOS）：恢复 contentOffset（通过 KRScrollView 的 applyTurboDisplayExtraCacheContent）
 * - 跨端侧（Kotlin）：恢复 pageIndex（业务逻辑层）
 * 
 * 缓存格式：
 * { "bannerTag": {"viewName":"KRScrollView","contentOffsetX":750,"contentOffsetY":0,"pageIndex":2} }
 */
@Page("TBBannerTestPage")
internal class TBBannerTestPage : BasePager() {

    companion object {
        private const val TAG = "TBBannerTestPage"
        private const val BANNER_COUNT = 5
    }

    // 当前页索引
    private var currentPageIndex by observable(0)
    // 当前 offset
    private var currentOffsetX by observable(0f)
    private var currentOffsetY by observable(0f)
    
    // Banner 引用
    private var bannerRef: ViewRef<PageListView<*, *>>? = null
    
    // 从缓存恢复的数据
    private var restoredPageIndex = 0
    private var extraCacheContent = JSONObject()
    
    // Banner 数据
    private val bannerColors = listOf(
        Color(0xFFE91E63),  // Pink
        Color(0xFF9C27B0),  // Purple
        Color(0xFF3F51B5),  // Indigo
        Color(0xFF2196F3),  // Blue
        Color(0xFF4CAF50)   // Green
    )

    override fun created() {
        super.created()
        restoreFromPageData()
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
     * 格式：{ "bannerTag": {"viewName":"KRScrollView","contentOffsetX":x,"contentOffsetY":y,"pageIndex":n} }
     * 
     * 端侧使用：contentOffsetX, contentOffsetY - iOS 侧通过 applyTurboDisplayExtraCacheContent 恢复 offset
     * 跨端使用：pageIndex - Kotlin 侧通过 scrollToPageIndex 恢复业务状态
     * 
     * 注意：offset 使用 pageIndex * pageItemWidth 计算，而不是当前 scroll offset
     *      这样可以确保恢复时精确对齐到页面边界，避免露出上一页尾巴
     */
    private fun buildExtraCacheContent(): String {
        val tag = bannerRef?.nativeRef ?: return "{}"
        val pageItemWidth = pagerData.pageViewWidth - 24f  // 与 attr 中的 pageItemWidth 保持一致
        val preciseOffsetX = currentPageIndex * pageItemWidth
        return """{"$tag":{"viewName":"KRScrollView","contentOffsetX":$preciseOffsetX,"contentOffsetY":0,"pageIndex":$currentPageIndex}}"""
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
                    title = "TB 轮播图测试"
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
                    fontSize(14f)
                    color(Color.GRAY)
                    text("当前页: ${ctx.currentPageIndex + 1}/${BANNER_COUNT}, offset: (${ctx.currentOffsetX.toInt()}, ${ctx.currentOffsetY.toInt()})")
                }
            }

            // Banner 轮播图
            View {
                attr {
                    height(200f)
                    margin(12f)
                    borderRadius(12f)
                    overflow(true)
                }
                
                PageList {
                    ref {
                        ctx.bannerRef = it
                        // 解析恢复数据
                        val cacheProps = ctx.extraCacheContent.toMap()
                        val bannerPropsValue = cacheProps[this.nativeRef.toString()]
                        if (bannerPropsValue != null && bannerPropsValue.toString() != "null") {
                            try {
                                val props = JSONObject(bannerPropsValue.toString()).toMap()
                                ctx.restoredPageIndex = (props["pageIndex"] as? Number)?.toInt() ?: 0
                                KLog.i(TAG, "【恢复Banner】pageIndex=${ctx.restoredPageIndex}")
                            } catch (e: Exception) {
                                KLog.e(TAG, "【恢复Banner】解析失败: ${e.message}")
                            }
                        }
                        
                        // 恢复 pageIndex（跨端侧恢复）
                        ctx.addTaskWhenPagerUpdateLayoutFinish {
                            if (ctx.restoredPageIndex > 0) {
                                it.view?.scrollToPageIndex(ctx.restoredPageIndex, animated = false)
                                ctx.currentPageIndex = ctx.restoredPageIndex
                            }
                        }
                    }
                    
                    attr {
                        flex(1f)
                        pageDirection(true)  // 横向
                        pageItemWidth(pagerData.pageViewWidth - 24f)  // 减去两边 margin
                        showScrollerIndicator(false)
                    }
                    
                    event {
                        pageIndexDidChanged {
                            ctx.currentPageIndex = (it as JSONObject).optInt("index")
                            // 自动缓存
                            val extraContent = ctx.buildExtraCacheContent()
                            KLog.i(TAG, "【PageIndex缓存】index=${ctx.currentPageIndex}")
                            getPager().acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                                .setCurrentUIAsFirstScreenForNextLaunch(extraContent)
                        }
                        scroll {
                            ctx.currentOffsetX = it.offsetX
                            ctx.currentOffsetY = it.offsetY
                        }
                    }
                    
                    // Banner 页面
                    for (i in 0 until BANNER_COUNT) {
                        View {
                            attr {
                                flex(1f)
                                backgroundColor(ctx.bannerColors[i])
                                allCenter()
                            }
                            Text {
                                attr {
                                    fontSize(32f)
                                    fontWeightBold()
                                    color(Color.WHITE)
                                    text("Banner ${i + 1}")
                                }
                            }
                        }
                    }
                }
            }
            
            // 指示器
            View {
                attr {
                    flexDirectionRow()
                    justifyContentCenter()
                    marginTop(12f)
                }
                for (i in 0 until BANNER_COUNT) {
                    View {
                        attr {
                            size(8f, 8f)
                            borderRadius(4f)
                            margin(4f)
                            backgroundColor(if (i == ctx.currentPageIndex) Color(0xFF007AFF) else Color(0xFFCCCCCC))
                        }
                    }
                }
            }
            
            // 说明文字
            View {
                attr {
                    margin(20f)
                    padding(16f)
                    backgroundColor(Color(0xFFF5F5F5))
                    borderRadius(8f)
                }
                Text {
                    attr {
                        fontSize(13f)
                        color(Color(0xFF666666))
                        text("""
                            |缓存恢复说明：
                            |
                            |1. 端侧（iOS）恢复：
                            |   - contentOffsetX/Y：通过 KRScrollView 的 applyTurboDisplayExtraCacheContent 方法恢复
                            |
                            |2. 跨端侧（Kotlin）恢复：
                            |   - pageIndex：通过 scrollToPageIndex 方法恢复
                            |
                            |测试步骤：
                            |1. 滑动到任意 Banner 页
                            |2. 点击「刷新缓存」或切换 Banner 自动缓存
                            |3. 退出页面后重新进入
                            |4. 观察是否恢复到上次的 Banner 页
                        """.trimMargin())
                    }
                }
            }
        }
    }
}
