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
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("CustomPaggingListDemoPage")
internal class CustomPaggingListDemoPage: BasePager() {

    lateinit var listViewRef : ViewRef<ListView<*, *>>
    var indexText by observable("")
    var offsetXDragBegin : Float = 0f
    var maxCount = 4
    var cardDataList by observableList<Color>()
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // navBar
            NavBar {
                attr {
                    title = "EventDemo"
                }
            }
            Text {
                attr {
                    text("curIndex:" + ctx.indexText)
                    color(Color.BLACK)
                }
            }

            View {
                attr {
                    flexDirectionRow()
                    height(200f)
                }
                PageList {
                    attr {
                        pageItemWidth(pagerData.pageViewWidth - 32f)
                        pageItemHeight(200f)
                        marginLeft(16f)
                        backgroundColor(Color.BLACK)
                        flex(1f)
                        pageDirection(true)
                    }
                    event {
                        pageIndexDidChanged {
                            KLog.i("PageList", it.toString())
                        }
                    }
                    View {
                        attr {
                            //size(pagerData.pageViewWidth - 48f)

                            backgroundColor(Color.YELLOW)
                        }
                    }
                    View {
                        attr {
                            //size(pagerData.pageViewWidth - 48f)

                            backgroundColor(Color.BLACK)
                        }
                    }
                    View {
                        attr {
                            //size(pagerData.pageViewWidth - 48f)

                            backgroundColor(Color.BLUE)
                        }
                    }
                    View {
                        attr {
                            //size(pagerData.pageViewWidth - 48f)

                            backgroundColor(Color.GREEN)
                        }
                    }
                }

            }

            PageList {
                attr {
                    height(200f)
                    flexDirectionRow()
                    marginLeft(12f)
                    backgroundColor(Color.BLACK)
                }
                event {
                    pageIndexDidChanged {
                        KLog.i("PageList", "second:" + it.toString())
                    }
                }

                View {
                    attr {
                        width(pagerData.pageViewWidth)
                        backgroundColor(Color(0xFF0000, 0.5f))
                    }
                    Button {
                        attr {
                            padding(4f)
                            alignSelfFlexStart()
                            backgroundColor(Color.GREEN)
                            borderRadius(10f)
                            imageAttr {
                                backgroundColor(Color.YELLOW)
                                size(30f, 30f)
                            }
                            titleAttr {
                                text("2222")
                            }
                            highlightBackgroundColor(Color.GRAY)
                        }
                    }
                }

                View {
                    attr {
                        width(pagerData.pageViewWidth)
                        backgroundColor(Color.RED)
                    }
                }

                View {
                    attr {
                        width(pagerData.pageViewWidth)
                        backgroundColor(Color.YELLOW)
                    }
                }

                View {
                    attr {
                        width(150f)
                        backgroundColor(Color.GREEN)
                    }
                }

                View {
                    attr {
                        width(200f)
                        backgroundColor(Color.GRAY)
                    }
                }

                View {
                    attr {
                        width(350f)
                        backgroundColor(Color.BLACK)
                    }
                }
            }
            PageList {
                attr {
                    marginTop(50f)
                    flex(1f)
                    backgroundColor(Color.BLACK)
                }
                event {
                    pageIndexDidChanged {
                        KLog.i("PageList", "third:" + it.toString())

                    }
                }
                event {
                    willDragEndBySync {
                        //KLog.i("2", "334343434")
                    }
                }

                View {
                    attr {
                        height(200f)
                        backgroundColor(Color.BLUE)
                    }
                }

                View {
                    attr {
                        height(300f)
                        backgroundColor(Color.RED)
                    }
                }

                View {
                    attr {
                        height(200f)
                        backgroundColor(Color.YELLOW)
                    }
                }

                View {
                    attr {
                        height(150f)
                        backgroundColor(Color.GREEN)
                    }
                }

                View {
                    attr {
                        height(200f)
                        backgroundColor(Color.GRAY)
                    }
                }

                View {
                    attr {
                        height(350f)
                        backgroundColor(Color.BLACK)
                    }
                }
            }
        }
    }

}