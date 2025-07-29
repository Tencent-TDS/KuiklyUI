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
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.base.extension.GCView
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ViewExtExamplePage")
internal class ViewExtExamplePage : BasePager() {
    private val bgUrl = "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg"

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "View扩展能力"
                }
            }

            GCView {
                attr {
                    backgroundImage(ctx.bgUrl) //  背景属性
                    justifyContentCenter()
                    alignItemsCenter()
                    flex(1f)
                }
                Text {
                    attr {
                        text("我是居中文本")
                    }
                }
            }

        }

    }
}