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

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.appearPercentage
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.View

internal class GalleryCardAttr : ComposeAttr() {
    var galleryItemData : GalleryCardData? by observable(null)
}

internal class GalleryCardEvent : ComposeEvent()

// 画廊卡片
internal class GalleryCardView : ComposeView<GalleryCardAttr, GalleryCardEvent>() {
    override fun createAttr() = GalleryCardAttr()
    override fun createEvent() = GalleryCardEvent()
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
            }
            View {
                attr {
                    flex(1f)
                    marginLeft(pagerData.pageViewWidth * 0.22f)
                    marginRight(pagerData.pageViewWidth * 0.22f)
                    backgroundColor(ctx.attr.galleryItemData?.bgColor ?: Color.TRANSPARENT)
                }
                event {
                    appearPercentage {
                        KLog.i("GalleryCardView", it.toString())
                    }
                }
                // 真正卡片内容都在这里写
                // 可以放video view等任意子view内容

            }

        }

    }
}

internal fun ViewContainer<*, *>.GalleryCard(init : GalleryCardView.() -> Unit) {
    addChild(GalleryCardView(), init)
}
