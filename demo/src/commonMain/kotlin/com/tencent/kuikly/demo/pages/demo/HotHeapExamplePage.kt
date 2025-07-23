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
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasView
import com.tencent.kuikly.core.views.ImageView
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.random.Random

@Page("HotHeapExamplePage")
internal class HotHeapExamplePage : BasePager() {
    var imageRef: ViewRef<ImageView>? = null
    var canvasRef: ViewRef<CanvasView>? = null
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "热力图"
                }
            }

            View {
                attr {
                    flex(1f)
                }
                Canvas({
                    ref {
                        ctx.canvasRef = it
                    }
                    attr {
                        flex(1f)

//                        visibility(false)
                    }
                },{ context, width, height ->
                    context.beginPath()

                    for (i in 0..200) {
                        val centerX = ctx.generateRandomNumber(30, (width - 30).toInt())
                        val centerY = ctx.generateRandomNumber(30, (height - 30).toInt())
                        val alpha = ctx.generateRandomNumber(30, 100)
                        context.createRadialGradient(centerX.toFloat(), centerY.toFloat(),0f,
                            centerX.toFloat() , centerY.toFloat(), 25f,
                            alpha / 100f,
                            Color(red255 = 0, green255 = 0, blue255 = 0, 1f),
                            Color(red255 = 0, green255 = 0, blue255 = 0, 0f),
                        )
                    }
                    context.closePath()
                })

            }

        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
//        canvasRef?.view?.setHotHeapMode(1f,
//            ColorStop(Color.RED, 0f),
//            ColorStop(Color.YELLOW, 0.3f),
//            ColorStop(Color(red255 = 0, green255 = 255, blue255 = 0, 1f), 0.5f),
//            ColorStop(Color(red255 = 0, green255 = 255, blue255 = 0, 0f), 1f))

    }

    fun generateRandomNumber(min: Int, max: Int): Int {
        return Random.nextInt(min, max + 1)
    }
}