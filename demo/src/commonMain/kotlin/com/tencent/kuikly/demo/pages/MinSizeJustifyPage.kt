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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 主轴尺寸被 minWidth 撑大的容器，justifyContent 的对齐结果应与直接指定同样宽度时一致。
 * 每组左边的灰块用 minWidth(96f) 撑开，右边用 width(96f) 作对照，两边的内容应出现在相同位置。
 */
@Page("MinSizeJustifyPage")
internal class MinSizeJustifyPage : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            // 左右 padding 各 16、居中：文字应在灰块正中，不偏右、不超出
            View {
                attr {
                    flexDirectionRow()
                    marginBottom(24f)
                }
                ctx.paddedBox(this, fixedWidth = false)
                ctx.paddedBox(this, fixedWidth = true)
            }

            // left = 0、top = 0 的绝对定位红块：应留在灰块左上角，文字仍居中
            View {
                attr {
                    flexDirectionRow()
                }
                ctx.boxWithAbsoluteChild(this, fixedWidth = false)
                ctx.boxWithAbsoluteChild(this, fixedWidth = true)
            }
        }
    }

    private fun paddedBox(container: ViewContainer<*, *>, fixedWidth: Boolean) {
        container.View {
            attr {
                flexDirectionRow()
                justifyContentCenter()
                alignItemsCenter()
                if (fixedWidth) width(96f) else minWidth(96f)
                height(96f)
                paddingLeft(16f)
                paddingRight(16f)
                margin(left = 8f, right = 8f)
                backgroundColor(Color.GRAY)
            }
            Text {
                attr {
                    text("请注意")
                    fontSize(14f)
                    color(Color.WHITE)
                }
            }
        }
    }

    private fun boxWithAbsoluteChild(container: ViewContainer<*, *>, fixedWidth: Boolean) {
        container.View {
            attr {
                flexDirectionRow()
                justifyContentCenter()
                alignItemsCenter()
                if (fixedWidth) width(96f) else minWidth(96f)
                height(96f)
                margin(left = 8f, right = 8f)
                backgroundColor(Color.GRAY)
            }
            Text {
                attr {
                    text("角标")
                    fontSize(14f)
                    color(Color.WHITE)
                }
            }
            View {
                attr {
                    absolutePosition(top = 0f, left = 0f)
                    size(16f, 16f)
                    backgroundColor(Color.RED)
                }
            }
        }
    }
}
