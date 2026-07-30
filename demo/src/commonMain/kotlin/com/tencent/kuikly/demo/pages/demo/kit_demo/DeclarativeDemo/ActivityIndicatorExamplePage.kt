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

package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.ActivityIndicator
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ActivityIndicatorExamplePage")
internal class ActivityIndicatorExamplePage : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "ActivityIndicator Example"
                }
            }
            ViewExampleSectionHeader {
                attr {
                    title = "活动指示器-白色style（又名loading菊花，默认白色）"
                }
            }
            View {
                attr {
                    backgroundColor(Color.BLACK)
                    height(50f)
                }
                // 默认尺寸就是20*20
                ActivityIndicator {
                    attr {
                        // 默认尺寸就是20*20 所以不需要指定尺寸，如果想控制大小，就通过transform scale来缩放控制大小
                    }
                }
            }
            ViewExampleSectionHeader {
                attr {
                    title = "活动指示器-灰色style"
                }
            }
            View {
                attr {
                    backgroundColor(Color.WHITE)
                    height(50f)
                }
                // 默认尺寸就是20*20
                ActivityIndicator {
                    attr {
                        isGrayStyle(true) // 灰色菊花
                        // 默认尺寸就是20*20 所以不需要指定尺寸，如果想控制大小，就通过transform scale来缩放控制大小
                    }
                }
            }
        }
    }
}