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
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("nestedHorizontalList")
internal class NestedHorizontalListPage : BasePager() {

    private var transformAnimationFlag by observable(false)
    private var backgroundColorAnimationFlag by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            PageList {
                attr {
                    flexDirectionRow()
                    pageItemWidth(pagerData.pageViewWidth)
                    pageItemHeight(pagerData.pageViewHeight)
                }
                List {
                    attr {
                        size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    }

                    for (i in 0 until 20) {
                        View {
                            attr {
                                if (i % 2 == 0) {
                                    backgroundColor(Color.GREEN)
                                } else {
                                    backgroundColor(Color.GRAY)
                                }
                                height(300f)
                            }

                            if (i == 1) {
                                List {
                                    attr {
                                        flexDirectionRow()
                                        height(300f)
                                    }

                                    for (ii in 0 until 10) {
                                        View {
                                            attr {
                                                size(250f, 300f)
                                                if (ii % 2 == 0) {
                                                    backgroundColor(Color.RED)
                                                } else {
                                                    backgroundColor(Color.WHITE)
                                                }
                                            }
                                            event {
                                                click {
                                                    KLog.d("NestedHorizontalListPage", "view click")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                View {
                    attr {
                        size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                        backgroundColor(Color.YELLOW)
                    }
                }
                View {
                    attr {
                        size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                        backgroundColor(Color.BLUE)
                    }
                }
            }

        }
    }

}
