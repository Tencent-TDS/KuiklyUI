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

package com.tencent.kuikly.demo.pages.app.common

import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.View

internal class AppNineGridView: ComposeView<AppNineGridViewAttr, AppNineGridViewEvent>() {

    companion object {
        // 每行图片数
        private const val MAX_ROW_IMAGE_SIZE = 3
        private const val IMAGE_PADDING = 15.0f
        private const val IMAGE_SPACING = 2
        private const val MAX_IMAGE_WIDTH = 250.0f
    }

    private var imageWidth: Float by observable(0.0f)

    override fun createEvent(): AppNineGridViewEvent {
        return AppNineGridViewEvent()
    }

    override fun createAttr(): AppNineGridViewAttr {
        return AppNineGridViewAttr()
    }

    override fun layoutFrameDidChanged(frame: Frame) {
        super.layoutFrameDidChanged(frame)
        updateImageWidth(frame.width)
    }

    private fun getColumCount(): Int {
        val imageSize = attr.picUrls.size
        return if (imageSize <= MAX_ROW_IMAGE_SIZE) {
            imageSize
        } else if (imageSize <= MAX_ROW_IMAGE_SIZE * 2) {
            if (imageSize == 4) {
                2
            } else {
                3
            }
        } else {
            3
        }
    }

    private fun updateImageWidth(containerWidth: Float) {
        val columCount = getColumCount()
        imageWidth = (containerWidth - IMAGE_PADDING * 2 - (columCount - 1) * IMAGE_SPACING) / columCount
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionRow()
                justifyContent(FlexJustifyContent.FLEX_START)
                padding(5f, IMAGE_PADDING, 5f, IMAGE_PADDING)
                flexWrap(FlexWrap.WRAP)
                alignItems(FlexAlign.CENTER)
            }
            val imageSize = ctx.attr.picUrls.size
            if (imageSize > 0) {
                if (imageSize == 1) {
                    View {
                        attr {
                            padding(2.0f)
                        }
                        Image {
                            attr {
                                alignSelfCenter()
                                resizeCover()
                                size(MAX_IMAGE_WIDTH, MAX_IMAGE_WIDTH)
                                src(ctx.attr.picUrls[0])
                            }
                        }
                    }
                } else {
                    val columCount = ctx.getColumCount()
                    for (i in 1..imageSize) {
                        Image {
                            attr {
                                alignSelfCenter()
                                resizeCover()
                                size(ctx.imageWidth, ctx.imageWidth)
                                src(ctx.attr.picUrls[i - 1])
                                if (i % columCount != 0) {
                                    marginRight(2f)
                                }
                                if (i > columCount) {
                                    marginTop(2f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class AppNineGridViewAttr : ComposeAttr() {
    var picUrls = listOf<String>()
}

internal class AppNineGridViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.AppNineGrid(init: AppNineGridView.() -> Unit) {
    addChild(AppNineGridView(), init)
}