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
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Mask
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("mask_demo")
internal class MaskDemoPage : BasePager() {
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                paddingTop(30f)
                backgroundColor(Color.BLACK)
            }

            Mask( {
                attr {
                    // test
                    size(351f, 400f)
                    allCenter()
                }
                Image {
                    attr {
                        size(351f, 400f)
                        resizeStretch()
                        src("https://tianquan.gtimg.cn/shoal/qqvip/f3cb664e-b2cb-451a-9c00-f8cb9d8d302e.png")
                    }
                }
            },
                {
                    View {
                        attr {
                            size(351f, 400f)
                            backgroundColor(Color.BLUE)
                        }
                    }
                }
            )
        }
    }
}