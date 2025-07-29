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

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Hover
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text

@Page("HoverExamplePage")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            List {
                attr {
                    flex(1f)
                    backgroundColor(Color.GRAY)
                }
                Text {
                    attr {
                        marginTop(100f)
                        size(pagerData.pageViewWidth, 2000f)
                        text("占位组件，用于滑动列表")
                        fontSize(20f)
                    }
                }
                Hover {
                    attr {
                        absolutePosition(top = 300f, left =0f, right =0f)
                        height(50f)
                        backgroundColor(Color.RED)
                    }
                }
                Hover {
                    attr {
                        absolutePosition(top = 600f, left =0f, right =0f)
                        height(50f)
                        backgroundColor(Color.BLUE)
                    }
                }
            }
        }
    }
}
