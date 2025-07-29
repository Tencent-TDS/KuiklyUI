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
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.toInt
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.CheckBox
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("CheckBoxExamplePage")
internal class CheckBoxExamplePage : BasePager() {
    var disable by observable(false)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "CheckBoxExamplePage"
                }
            }
            View {
                attr {
                    height(30f)
                    backgroundColor(Color.GRAY)
                }
            }
            CheckBox {
                attr {
                    size(30f, 30f)
                    checked(true)
                    defaultImageSrc("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/Efeg39sG.png")
                    checkedImageSrc("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/m5kRYKMt.png")

                    disableImageSrc("https://vfiles.gtimg.cn/wuji_material/web283034f3-ee18-407b-c117-cdaf23bd7a38.png")
                    disable(ctx.disable)
                }
                event {
                    checkedDidChanged {
                        KLog.i("2", "checkedDidChanged:" + it.toInt())
                        ctx.disable = true
                    }
                }
            }
        }
    }
}