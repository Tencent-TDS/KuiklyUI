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
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.base.extension.to375
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ListInitContentOfffsetTestPage")
internal class ListInitContentOfffsetTestPage: BasePager() {

    private var list by observableList<String>()
    private var listRef : ViewRef<ListView<*, *>>? = null

    override fun created() {
        super.created()
        list.add("0")
        list.add("1")
        setTimeout(2000) {
            for (i in 2..10) {
                list.add(i.toString())
            }
        }

    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {

            NavBar {
                attr {
                    title = "ListInitContentOfffsetTest"
                }
            }

            List {
                ref {
                    ctx.listRef = it
                }
                attr {
                    marginTop(20f)
                    backgroundColor(Color.YELLOW)
                    height(4 *80f.to375)
                    width(80f.to375)
                   // flexDirectionRow()
                    firstContentLoadMaxIndex(400)
                }
                event {
                    contentSizeChanged { width, height ->
                        if (height > 80f.to375 * 8) {
                            ctx.listRef?.view?.setContentOffset(0f,80f.to375 * 4, false)

                        }
                    }
                }

                vfor({ctx.list}) {string ->
                    View {
                        attr {
                            size(80f.to375, 80f.to375)
                            allCenter()

                        }
                        Text {
                            attr {
                                fontSize(15f)
                                text(string)
                            }
                        }
                    }
                }

            }
        }
    }

}