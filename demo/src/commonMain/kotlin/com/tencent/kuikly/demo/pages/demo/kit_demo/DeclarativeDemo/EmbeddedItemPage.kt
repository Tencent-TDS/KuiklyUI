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
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager


@Page("EmbeddedItemPage")
internal class EmbeddedItemPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(Color.WHITE)
                alignItemsCenter()
            }
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                    padding(all = 16f)
                }
                Image {
                    attr {
                        size(60f, 60f)
                        borderRadius(20f)
                        resizeContain()
                        src(ImageUri.commonAssets("penguin2.png"))
                    }
                }
                Text {
                    attr {
                        marginLeft(12f)
                        fontSize(16f)
                        color(Color(0xFF333333))
                        text("Embedded Item Text")
                    }
                }
            }
        }
    }
}
