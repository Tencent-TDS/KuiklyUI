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
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.KRNestedScrollMode
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager

// 竖向List > 竖向List
@Page("lnest")
internal class ListNestBounceFalse : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            List {
                attr {
                    width(pagerData.pageViewWidth)
                    height(pagerData.pageViewHeight)
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

                for (i in 1..20) {
                    Text { attr { text(" 外部 $i ") } }
                }

                // bouncesEnable(false)
                List {
                    attr {
                        width(pagerData.pageViewWidth)
                        height(200f)
                        backgroundColor(Color.RED)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.PARENT_FIRST, KRNestedScrollMode.SELF_ONLY)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner1 list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner1 dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner1 dragEnd")
                        }
                    }

                    for (i in 1..50) {
                        Text { attr { text("PARENT_FIRST SELF_ONLY $i") } }
                    }
                }

                for (i in 1..2) {
                    Text { attr { text(" 外部 $i ") } }
                }

//                // bouncesEnable(true)
                List {
                    attr {
                        width(pagerData.pageViewWidth)
                        height(200f)
                        backgroundColor(Color.GREEN)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.SELF_ONLY, KRNestedScrollMode.SELF_ONLY)
                    }

                    event {
                        scroll {
                            KLog.i("nested", "inner2 list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner2 dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner2 dragEnd")
                        }
                    }
                    for (i in 1..50) {
                        Text { attr { text("SELF_ONLY SELF_ONLY true $i") } }
                    }
                }

                for (i in 1..2) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {
                    attr {
                        width(pagerData.pageViewWidth)
                        height(200f)
                        backgroundColor(Color.GREEN)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.SELF_FIRST, KRNestedScrollMode.SELF_FIRST)
                    }
                    event {
                        scroll {
                            KLog.i("nested", "inner3 list scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner3 dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner3 dragEnd")
                        }
                    }
                    for (i in 1..50) {
                        Text { attr { text("bounce true, scrollWithParent false, SELF_FIRSTSELF_FIRST $i") } }
                    }
                }

                for (i in 1..10) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {
                    attr {
                        width(pagerData.pageViewWidth)
                        height(200f)
                        backgroundColor(Color.GREEN)
                        bouncesEnable(false)
                        nestedScroll(KRNestedScrollMode.PARENT_FIRST, KRNestedScrollMode.PARENT_FIRST)
                    }
                    event {
                        scroll {
                            KLog.i("nested", "inner4 list PARENT_FIRST scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner4 list PARENT_FIRST dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner4 list PARENT_FIRST dragEnd")
                        }
                    }
                    for (i in 1..50) {
                        Text { attr { text("list4 PARENT_FIRST PARENT_FIRST , $i") } }
                    }
                }

                for (i in 1..20) {
                    Text { attr { text(" 外部 $i ") } }
                }

                List {
                    attr {
                        width(pagerData.pageViewWidth)
                        height(200f)
                        backgroundColor(Color.GREEN)
                        nestedScroll(KRNestedScrollMode.PARENT_FIRST, KRNestedScrollMode.SELF_FIRST)
                    }
                    event {
                        scroll {
                            KLog.i("nested", "inner5 list PARENT_FIRST scroll y: ${it.offsetY}")
                        }
                        dragBegin {
                            KLog.i("nested", "inner5 list PARENT_FIRST dragBegin")
                        }
                        dragEnd {
                            KLog.i("nested", "inner5 list PARENT_FIRST dragEnd")
                        }
                    }
                    for (i in 1..50) {
                        Text { attr { text("list5 , $i") } }
                    }
                }

                for (i in 1..20) {
                    Text { attr { text(" 外部 $i ") } }
                }
            }
        }
    }
}