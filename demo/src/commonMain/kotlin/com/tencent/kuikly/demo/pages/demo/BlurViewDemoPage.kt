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
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("BlurViewDemoPage")
internal class BlurViewDemoPage : BasePager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // navBar
            NavBar {
                attr {
                    title = "BlurView组件Demo"
                }
            }
            View {
                attr {
                    flex(1f)
                    backgroundColor(Color(0xFF3c6cbdL))
                }
                Image {
                    attr {
                        absolutePositionAllZero()
                        resizeContain()

                        src(ImageUri.commonAssets("panda2.png"))
                    }
                }
                // 背景图
                Image {
                    val d = this
                    attr {
                        absolutePositionAllZero()
                        resizeContain()

                        blurRadius(5f)
                        maskLinearGradient(Direction.TO_BOTTOM, ColorStop(Color.WHITE, 0f), ColorStop(Color.WHITE, 0.5f),  ColorStop(Color(red255 = 255, green255 = 255, blue255 = 255, 0f), 1f))
                        src(ImageUri.commonAssets("penguin2.png"))
                    }
                    event {
                        click {
                            d.getViewAttr().blurRadius(2f)
                            d.getViewAttr().maskLinearGradient(Direction.TO_BOTTOM, ColorStop(Color.WHITE, 0f), ColorStop(Color.WHITE, 0.5f),  ColorStop(Color(red255 = 255, green255 = 255, blue255 = 255, 0.8f), 1f))

                        }
                    }
                }
            }
        }
    }

    override fun created() {
        super.created()
        lifecycleScope.launch {
            KLog.i("2222", "2343434")
        }
    }
}