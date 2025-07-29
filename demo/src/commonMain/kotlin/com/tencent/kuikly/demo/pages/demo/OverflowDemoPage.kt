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
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Created by kam on 2022/7/28.
 */
@Page("OverflowDemoPage")
internal class OverflowDemoPage : Pager() {

    var dataList: ObservableList<String> by observableList()

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }

            NavBar {
                attr {
                    title = "OverflowDemoPage"
                }
            }

            List {
                attr {
                    flex(1f)
                }
                View {
                    attr {
                        height(200f)
                        backgroundColor(Color.GREEN)
                    }
                }
                View {
                    attr {
                        height(200f)
                        backgroundColor(Color.RED)
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
                        height(200f)
                        backgroundColor(Color.BLACK)
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
                        height(200f)
                        backgroundColor(Color.WHITE)
                    }
                }

            }

            View {
                attr {
                    absolutePosition(bottom = 600f, right = 0f)
                    size(100f, 100f)
                    backgroundColor(Color.YELLOW)

                }
                View {
                    attr {
                        absolutePosition(top = -40f, right = 0f)
                        size(50f, 50f)
                        backgroundColor(Color.RED)
                    }
                }
            }

            View {
                attr {
                    absolutePosition(bottom = 400f, right = 0f)
                    size(100f, 100f)
                    backgroundColor(Color.BLACK)
                    overflow(true)
                }
                View {
                    attr {
                        absolutePosition(top = -40f, right = 0f)
                        size(50f, 50f)
                        backgroundColor(Color.RED)
                    }
                }
            }

            View {
                attr {
                    absolutePosition(bottom = 100f, right = 0f)
                    size(100f, 100f)
                    backgroundColor(Color.GREEN)
                    overflow(false)
                }
                View {
                    attr {
                        absolutePosition(top = -40f, right = 0f)
                        size(50f, 50f)
                        backgroundColor(Color.RED)
                    }
                }
            }

        }
    }
}