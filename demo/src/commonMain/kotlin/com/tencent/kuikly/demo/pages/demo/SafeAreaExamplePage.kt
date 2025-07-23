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
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.SafeArea
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("SafeAreaExamplePage")
internal class SafeAreaExamplePage: BasePager() {

    override fun body(): ViewBuilder {
        val hairWidth = 1f / pagerData.density
        val ctx = this
        return {
            attr {
                border(Border(hairWidth, BorderStyle.SOLID, Color.RED))
            }
            View {
                attr {
                    absolutePosition(top = 0f, left = 0f, right = 0f)
                    height(pagerData.safeAreaInsets.top)
                    backgroundColor(Color.GRAY)
                }
            }
            SafeArea {
                View {
                    attr {
                        flex(1f)
                        margin(hairWidth)
                        // backgroundColor(Color.BLUE)
                        border(Border(hairWidth, BorderStyle.SOLID, Color.BLUE))
                    }
                }
            }
        }
    }
}