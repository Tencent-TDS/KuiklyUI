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
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

@Page("ImageExamplePage")
internal class ImageExamplePage: BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "Image Attr Example" } }
            List {
                attr { flex(1f) }
                ViewExampleSectionHeader {  attr { title = "Image { attr { resizeContain() } }" } }
                Image {
                    attr {
                        alignSelfCenter()
                        margin(all = 8f)
                        backgroundColor(0xFFE5E5E5)
                        size(width = 240f, height = 180f)
                        src("https://picsum.photos/200/300")
                        resizeContain()
                        borderRadius(20f)
                        boxShadow(BoxShadow(10f, 10f, 30f, Color.BLACK))
                    }
                }
                ViewExampleSectionHeader { attr { title = "Image { attr { resizeCover() } }" } }
                Image {
                    attr {
                        alignSelfCenter()
                        margin(all = 8f)
                        backgroundColor(0xFFE5E5E5)
                        size(width = 240f, height = 180f)
                        src("https://picsum.photos/200/300?test=1")
                        resizeCover()
                        placeholderSrc("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59ef6918.gif")
                        boxShadow(BoxShadow(2f, 2f, 10f, Color.BLACK))
                    }
                }
                ViewExampleSectionHeader { attr { title = "Image { attr { resizeStretch() } }" } }
                Image {
                    attr {
                        alignSelfCenter()
                        margin(all = 8f)
                        backgroundColor(0xFFE5E5E5)
                        size(width = 240f, height = 180f)
                        src("https://picsum.photos/200/300")
                        resizeStretch()
                    }
                }
                ViewExampleSectionHeader { attr { title = "Image { attr { blurRadius(5f) } }" } }
                Image {
                    attr {
                        alignSelfCenter()
                        margin(all = 8f)
                        backgroundColor(0xFFE5E5E5)
                        size(width = 240f, height = 180f)
                        src("https://picsum.photos/200/300")
                        resizeCover()
                        blurRadius(5f)
                    }
                }
                // Note: On MiniApp, tintColor is implemented via CSS drop-shadow.
                // The WeChat DevTools simulator does not render this filter correctly
                // (the whole image appears as a solid color block), but it works as
                // expected on real devices (iOS / Android). Please verify on a real
                // device when testing this example.
                ViewExampleSectionHeader { attr { title = "Image { attr { tintColor(Color.RED) } }" } }
                Text {
                    attr {
                        margin(all = 8f)
                        color(Color.RED)
                        fontSize(12f)
                        text("Note: On MiniApp, please test on a real device (WeChat DevTools does not render drop-shadow correctly).")
                    }
                }
                Image {
                    attr {
                        alignSelfCenter()
                        margin(all = 8f)
                        backgroundColor(0xFFE5E5E5)
                        size(width = 180f, height = 120f)
                        src("https://raw.githubusercontent.com/Tencent-TDS/KuiklyUI/refs/heads/main/demo/src/commonMain/assets/ChatDemo/kuikly_logo.png")
                        resizeContain()
                        tintColor(Color.RED)
                    }
                }
                // capInsets defines a 9-patch stretch region: the 4 corners keep
                // their original size, the 4 edges are stretched along one axis,
                // and the center is stretched along both axes. Useful for bubbles
                // / buttons with rounded corners that should not distort when
                // resized. On H5/MiniApp it is implemented via CSS `border-image`.
                ViewExampleSectionHeader {
                    attr { title = "Image { attr { capInsets(top, left, bottom, right) } }" }
                }
                // Original bubble image, stretched without capInsets (corners distorted)
                Text {
                    attr {
                        margin(left = 8f, right = 8f, top = 4f)
                        color(Color.GRAY)
                        fontSize(12f)
                        text("Without capInsets: resizeStretch scales the whole image, the rounded corners are distorted.")
                    }
                }
                Image {
                    attr {
                        alignSelfCenter()
                        margin(all = 8f)
                        size(width = 300f, height = 80f)
                        src("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/gjCqDSbr.png")
                        resizeStretch()
                    }
                }
                // Same image with capInsets: the 4 corners keep their size and
                // only the center is stretched, preserving the rounded borders.
                Text {
                    attr {
                        margin(left = 8f, right = 8f, top = 4f)
                        color(Color.GRAY)
                        fontSize(12f)
                        text("With capInsets(12, 25, 12, 12): 9-patch stretch. The 4 corners keep their size, edges stretch along one axis, center stretches along both. Rounded borders are preserved.")
                    }
                }
                Image {
                    attr {
                        alignSelfCenter()
                        margin(all = 8f)
                        size(width = 300f, height = 80f)
                        src("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/gjCqDSbr.png")
                        resizeStretch()
                        capInsets(12f, 25f, 12f, 12f)
                    }
                }
                ViewExampleSectionHeader {
                    attr { title = "Image { attr { maskLinearGradient(...) } }" }
                }
                // Top -> Bottom fade out (white opaque -> white transparent)
                Image {
                    attr {
                        alignSelfCenter()
                        margin(all = 8f)
                        size(width = 240f, height = 180f)
                        src("https://picsum.photos/200/300?mask=1")
                        resizeCover()
                        maskLinearGradient(
                            Direction.TO_BOTTOM,
                            ColorStop(Color.WHITE, 0f),
                            ColorStop(Color(red255 = 255, green255 = 255, blue255 = 255, 0f), 1f)
                        )
                    }
                }
                // Left -> Right fade out
                Image {
                    attr {
                        alignSelfCenter()
                        margin(all = 8f)
                        size(width = 240f, height = 180f)
                        src("https://picsum.photos/200/300?mask=2")
                        resizeCover()
                        maskLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color.WHITE, 0f),
                            ColorStop(Color(red255 = 255, green255 = 255, blue255 = 255, 0f), 1f)
                        )
                    }
                }
                // Two-end fade (transparent -> opaque -> transparent)
                Image {
                    attr {
                        alignSelfCenter()
                        margin(all = 8f)
                        size(width = 240f, height = 180f)
                        src("https://picsum.photos/200/300?mask=3")
                        resizeCover()
                        maskLinearGradient(
                            Direction.TO_BOTTOM,
                            ColorStop(Color(red255 = 255, green255 = 255, blue255 = 255, 0f), 0f),
                            ColorStop(Color.WHITE, 0.5f),
                            ColorStop(Color(red255 = 255, green255 = 255, blue255 = 255, 0f), 1f)
                        )
                    }
                }
                View {
                    attr {
                        height(3000f)
                    }
                }
            }
        }
    }
}