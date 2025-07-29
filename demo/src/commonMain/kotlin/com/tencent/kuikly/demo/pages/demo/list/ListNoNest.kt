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

package com.tencent.kuikly.demo.pages.demo.list

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("lnestno")
internal class ListNestBoundFalse : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            List {
                attr {
                    width(pagerData.pageViewWidth)
                    height(pagerData.pageViewHeight)
                    backgroundColor(Color.YELLOW)
                }

                for (i in 1..20) {
                    Text { attr { text(" 外部 $i ") } }
                }

                // bouncesEnable(false)
                List {
                    attr {
                        width(pagerData.pageViewWidth)
                        height(800f)
                        backgroundColor(Color.RED)
                        bouncesEnable(false)
                    }
                    for (i in 1..50) {
                        Text { attr { text("bounce false $i")
                            height(150f)} }
                    }
                }

                for (i in 1..2) {
                    Text { attr { text(" 外部 $i ") } }
                }

                // bouncesEnable(true)
                List {
                    attr {
                        width(pagerData.pageViewWidth)
                        height(200f)
                        backgroundColor(Color.GREEN)
                        bouncesEnable(true)
                    }
                    for (i in 1..50) {
                        Text { attr { text("bounce true $i")
                        height(150f)} }
                    }
                }

                for (i in 1..10) {
                    Text { attr { text(" 外部 $i ") } }
                }
            }
        }
    }
}