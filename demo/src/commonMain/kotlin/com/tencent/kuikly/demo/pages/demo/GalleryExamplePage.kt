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
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

const val minScale = 0.85f
const val galleryMarginLeft = 50f // 画廊居中的内容距离最左边margin
internal class GalleryCardData {
    var bgColor = Color.BLACK
    var transformScale by observable(minScale)
}

@Page("GalleryExamplePage")
internal class GalleryExamplePage : BasePager() {
    private var galleryHeight by observable(0f)
    private var cardDataList by observableList<GalleryCardData>()
    override fun created() {
        super.created()
        galleryHeight = pageData.pageViewHeight * 0.7f // 画廊高度为屏幕高度的80%
        // 添加几个卡片数据
        cardDataList.add( GalleryCardData().apply {
            bgColor = Color.YELLOW
            transformScale = 1.0f // 第一个默认是1f
        })
        cardDataList.add( GalleryCardData().apply {
            bgColor = Color.RED
        })
        cardDataList.add( GalleryCardData().apply {
            bgColor = Color.BLUE
        })
        cardDataList.add( GalleryCardData().apply {
            bgColor = Color.GREEN
        })
    }
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "GalleryExamplePage"
                }
            }
            View {
                attr {
                    marginTop(60f)
                    alignItemsCenter()
                }
                // 画廊区域View
                PageList {
                    attr {
                        pageItemHeight(pagerData.pageViewHeight * 0.65f) // 每个卡片高度
                        pageItemWidth(pagerData.pageViewWidth * 0.75f)  // 每个卡片宽度
                        flexDirectionRow() // 横向
                        overflow(false) // 不剪裁孩子，即子孩子可露出父亲布局区域
                        showScrollerIndicator(false) // 不显示滚动指示进度条
                    }
                    event {
                        scroll {
                            var index = 0
                            val offsetX = it.offsetX
                            val pageListWidth = it.viewWidth
                            ctx.cardDataList.forEach {
                                val itemLeft = index * pageListWidth
                                val itemRange = (itemLeft).toInt() .. (itemLeft+ pageListWidth).toInt()
                                val listRange = (offsetX ).toInt()..(offsetX  + pageListWidth).toInt()
                                val overlap = itemRange.intersect(listRange) // 计算两个区间重合区域
                                val visiblePercentage = overlap.count() * 1f / pageListWidth.toInt() // 重合区域除以宽度就是可见比例
                                it.transformScale =  minScale + (1 - minScale) * visiblePercentage
                                index++
                            }
                        }
                    }

                    vfor({ctx.cardDataList}) { item ->
                        GalleryCard {
                            attr {
                                galleryItemData = item
                                transform(scale = Scale(item.transformScale, item.transformScale))
                            }
                        }

                    }
                }
            }

        }
    }

}