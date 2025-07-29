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
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ScreenFrameExamplePage")
internal class ScreenFrameExamplePage : BasePager() {
    var frameY by observable(0f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "ScreenFrameExamplePage" + ctx.frameY
                }
            }

            View {
                attr {
                    size(100f, 100f)
                    marginTop(ctx.frameY)

                    screenFramePause(ctx.frameY > 200f)
                    backgroundColor(Color.RED)
                }
                event {
                    screenFrame {
                        ctx.frameY += 5f
                        if (ctx.frameY > 199) {
                            setTimeout(1000) {
                                ctx.frameY = 0f
                            }
                        }

                        KLog.i("22", "screenFrame")
                    }
                }
            }

        }
    }
}