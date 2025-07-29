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
import com.tencent.kuikly.core.views.Blur
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("BlurExamplePage")
internal class BlurExamplePage : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF3c6cbdL))

            }
            // 背景图
            Image {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                    src("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/lQ8TO29r.gif")
                }
            }
            // navBar
            NavBar {
                attr {
                    title = "BlurExamplePage"
                }
            }

            Text {
                attr {
                    marginTop(100f)
                    color(Color.WHITE)
                    text("模糊半径：10f")
                    fontSize(17f)
                }
            }
            Blur {
                attr {

                    height(100f)
                    blurRadius(10f)
                }
            }
            Text {
                attr {
                    marginTop(50f)
                    color(Color.WHITE)
                    text("模糊半径：4f")
                    fontSize(17f)
                }
            }
            View {
                attr {
                    height(100f)
                }

                Blur {
                    attr {
                        absolutePosition(left = 0f, right = 0f, top = 0f)
                        height(100f)
                        blurRadius(10f)
                    }

                }

            }
            Text {
                attr {
                    marginTop(50f)
                    color(Color.WHITE)
                    text("模糊半径：3f")
                    fontSize(17f)
                }
            }
            Blur {
                attr {

                    height(100f)
                    blurRadius(3f)
                }
            }
            Text {
                attr {
                    marginTop(50f)
                    color(Color.WHITE)
                    text("模糊半径：1f")
                    fontSize(17f)
                }
            }
            Blur {
                attr {
                    height(100f)
                    blurRadius(1f)
                }
            }
        }
    }
}