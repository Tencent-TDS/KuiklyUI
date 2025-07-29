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
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Slider
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("SliderExamplePage")
internal class SliderExamplePage : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            NavBar {
                attr {
                    title = "SliderExamplePage"
                }
            }

            List {
                attr {
                    flex(1f)
                }
                Slider {
                    attr {
                        marginTop(30f)
                        marginLeft(10f)
                        width(pagerData.pageViewWidth - 40f)
                        height(30f)
                        trackColor(Color.GRAY)
                        thumbColor(Color.BLUE)
                        progressColor(Color.RED)

                    }
                }

                Slider {
                    attr {
                        currentProgress(0.5f)
                        marginTop(30f)
                        marginLeft(10f)
                        width(20f)
                        height(300f)
                        trackColor(Color.BLACK)
                        thumbColor(Color.GREEN)
                        sliderDirection(false)
                    }
                    event {
                        progressDidChanged {
                            KLog.i("2", "progressDidChanged" + it.toString())
                        }
                        beginDragSlider {
                            KLog.i("2", "beginDragSlider")
                        }

                        endDragSlider {
                            KLog.i("2", "endDragSlider")
                        }
                    }
                }
            }

        }
    }
}