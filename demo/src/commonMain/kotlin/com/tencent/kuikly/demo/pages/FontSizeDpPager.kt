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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * Created by kam on 2023/11/23.
 */
@Page("000")
internal class FontSizeDpPager : BasePager() {

    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Text {
                attr {
                    fontSize(20f)
                    useDpFontSizeDim()
                    text("使用dp作为单位")
                }
            }
            Text {
                attr {
                    fontSize(20f)
                    text("使用sp作为单位")
                }
            }

            RichText {
                Span {
                    fontSize(20f)
                    text("使用sp作为单位")
                }
                Span {
                    fontSize(20f)
                    text("使用dp作为单位")
                    useDpFontSizeDim()
                }
            }

            Input {
                attr {
                    size(200f, 100f)
                    fontSize(20f)
                    useDpFontSizeDim()
                    text("使用dp作为单位")
                }
            }
        }
    }
}