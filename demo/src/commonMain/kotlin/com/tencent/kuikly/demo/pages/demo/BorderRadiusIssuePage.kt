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
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Hover
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("BorderRadiusIssuePage")
internal class BorderRadiusIssuePage : BasePager() {
    var btnBgColor by observable(Color.BLACK)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xfff5f5f5))
            }
            NavBar {
                attr {
                    title = "BorderRadiusIssuePage"
                }
            }

            List {
                attr {
                    flex(1f)
                }

                View {
                    attr {
                        height(10f)
                    }
                }
                Hover {
                    attr {
                        borderRadius(topLeft = 10f, topRight = 10f, bottomRight = 0f, bottomLeft = 0f)
                        //height(100f)

                    }
                    View {
                        attr {
                            height(100f)
                            backgroundColor(Color.WHITE)
                        }
                    }
                }
                View {
                    attr {

                        //   borderRadius(topLeft = 10f, topRight = 10f, bottomRight = 0f, bottomLeft = 0f)
                        height(100f)
                        backgroundColor(Color.WHITE)

                    }
                }
            }

        }
    }
}