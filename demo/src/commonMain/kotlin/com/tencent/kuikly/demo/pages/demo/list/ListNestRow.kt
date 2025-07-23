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
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.KRNestedScrollMode
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager

// 横向List > 横向List
@Page("lnestRow")
internal class ListNestRow : BasePager() {

    private var listRef : ViewRef<ListView<*, *>>? = null

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            List {
                attr {
                    marginTop(200f)
                    flexDirection(FlexDirection.ROW)
                    width(pagerData.pageViewWidth)
                    height(100f)
                    backgroundColor(Color.YELLOW)
                    bouncesEnable(false)
                }
                event {
                    scroll {
                        KLog.i("nested", "outter list scroll y: ${it.offsetY}")
                    }
                    dragBegin {
                        KLog.i("nested", "outter dragBegin")
                    }
                    dragEnd {
                        KLog.i("nested", "outter dragEnd")
                    }
                }

                List {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        width(200f)
                        backgroundColor(Color.GREEN)
                        bouncesEnable(false)
                        // 有点问题
                        nestedScroll(KRNestedScrollMode.SELF_ONLY, KRNestedScrollMode.PARENT_FIRST)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner so pf list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner so pf dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner so pf dragEnd")
                        }
                    }
                    for (i in 1..10) {
                        Text { attr { text("so pf $i ") } }
                    }
                }

                for (i in 1..5) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        width(200f)
                        backgroundColor(Color.GREEN)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.SELF_ONLY, KRNestedScrollMode.SELF_ONLY)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner so so list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner so so dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner so so dragEnd")
                        }
                    }
                    for (i in 1..10) {
                        Text { attr { text("so so $i ") } }
                    }
                }

                for (i in 1..5) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        width(200f)
                        backgroundColor(Color.RED)
                        bouncesEnable(false)
                        // 有点问题
                        nestedScroll(KRNestedScrollMode.SELF_ONLY, KRNestedScrollMode.SELF_FIRST)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner so sf list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner so sf dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner so sf dragEnd")
                        }
                    }
                    for (i in 1..10) {
                        Text { attr { text("so sf $i ") } }
                    }
                }

                for (i in 1..5) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        width(200f)
                        backgroundColor(Color.GREEN)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.PARENT_FIRST, KRNestedScrollMode.PARENT_FIRST)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner pf pf list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner pf pf dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner pf pf dragEnd")
                        }
                    }
                    for (i in 1..10) {
                        Text { attr { text("pf pf $i ") } }
                    }
                }

                for (i in 1..5) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {
                    ref {
                        ctx.listRef = it
                    }
                    attr {
                        flexDirection(FlexDirection.ROW)
                        width(200f)
                        backgroundColor(Color.RED)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.PARENT_FIRST, KRNestedScrollMode.SELF_FIRST)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner pf sf list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner pf sf dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner pf sf dragEnd")
                        }
                    }
                    for (i in 1..10) {
                        Text { attr { text("pf sf $i ") } }
                    }
                }

                for (i in 1..5) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        width(200f)
                        backgroundColor(Color.GREEN)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.PARENT_FIRST, KRNestedScrollMode.SELF_ONLY)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner pf so list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner pf so dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner pf so dragEnd")
                        }
                    }
                    for (i in 1..10) {
                        Text { attr { text("pf so $i ") } }
                    }
                }

                for (i in 1..5) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        width(200f)
                        backgroundColor(Color.RED)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.SELF_FIRST, KRNestedScrollMode.SELF_FIRST)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner sf sf list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner sf sf dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner sf sf dragEnd")
                        }
                    }
                    for (i in 1..10) {
                        Text { attr { text("sf sf $i ") } }
                    }
                }

                for (i in 1..5) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        width(200f)
                        backgroundColor(Color.RED)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.SELF_FIRST, KRNestedScrollMode.SELF_ONLY)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner sf so list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner sf so dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner sf so dragEnd")
                        }
                    }
                    for (i in 1..10) {
                        Text { attr { text("sf so $i ") } }
                    }
                }

                for (i in 1..5) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {

                    attr {
                        flexDirection(FlexDirection.ROW)
                        width(200f)
                        backgroundColor(Color.RED)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.SELF_FIRST, KRNestedScrollMode.PARENT_FIRST)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner sf pf list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner sf pf dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner sf pf dragEnd")
                        }
                    }
                    for (i in 1..10) {
                        Text { attr { text("sf pf $i ") } }
                    }
                }

            }

            Text {
                attr {
                    marginTop(50f)
                    text("pf sf")
                }
                event {
                    click {
                        ctx.listRef?.view?.setContentOffset(100f, 0f)
                    }
                }
            }
        }
    }
}