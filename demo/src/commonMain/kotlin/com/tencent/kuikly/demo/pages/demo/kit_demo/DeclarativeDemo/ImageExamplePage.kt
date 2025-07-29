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
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
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
                View {
                    attr {
                        height(3000f)
                    }
                }
            }
        }
    }
}