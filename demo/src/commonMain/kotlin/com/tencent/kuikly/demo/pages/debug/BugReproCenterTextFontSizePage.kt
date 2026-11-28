/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.demo.pages.debug

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * Minimal repro: one business-sized Button centered on the screen.
 * Expected: "同意" stays on one line.
 */
@Page("BugReproCenterTextFontSizePage")
internal class BugReproCenterTextFontSizePage : BasePager() {

    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(Color(0xFFF2F3F5L))
                allCenter()
            }
            View {
                attr {
                    allCenter()
                }
                Button {
                    attr {
                        width(158.3077f)
                        height(44.666668f)
                        backgroundColor(Color(0xFFDDDDDDL))
                        borderRadius(21f)
                        marginLeft(14f)
                        titleAttr {
                            text("同意")
                            fontSize(16f)
                            textAlignCenter()
                            color(Color(0xFF1A1A1AL))
                        }
                        marginBottom(40f)
                        accessibility("同意条款，开启完整体验模式")
                        accessibilityRole(AccessibilityRole.BUTTON)
                        accessibilityInfo(
                            clickable = true,
                            longClickable = false
                        )
                    }
                }
            }
        }
    }
}
