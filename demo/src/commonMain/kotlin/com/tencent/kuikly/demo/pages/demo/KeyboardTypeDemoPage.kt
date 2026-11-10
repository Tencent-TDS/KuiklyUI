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
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputAttr
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.TextAreaAttr
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("KeyboardTypeDemoPage")
internal class KeyboardTypeDemoPage : BasePager() {

    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(Color(0xFFF5F5F5))
            }
            NavBar {
                attr {
                    title = "KeyboardType"
                }
            }
            List {
                attr {
                    flex(1f)
                }
                View {
                    attr {
                        padding(left = 16f, right = 16f, bottom = 24f)
                    }
                    SectionTitle("Input 单行")
                    KeyboardInput("默认文本", "Text")
                    KeyboardInput("ASCII") { keyboardTypeAscii() }
                    KeyboardInput("数字") { keyboardTypeNumber() }
                    KeyboardInput("电话") { keyboardTypePhone() }
                    KeyboardInput("网址") { keyboardTypeUri() }
                    KeyboardInput("邮箱") { keyboardTypeEmail() }
                    KeyboardInput("密码") { keyboardTypePassword() }
                    KeyboardInput("数字密码") { keyboardTypeNumberPassword() }
                    KeyboardInput("小数") { keyboardTypeDecimal() }

                    SectionTitle("TextArea 多行")
                    KeyboardArea("默认文本", "Text")
                    KeyboardArea("ASCII") { keyboardTypeAscii() }
                    KeyboardArea("数字") { keyboardTypeNumber() }
                    KeyboardArea("电话") { keyboardTypePhone() }
                    KeyboardArea("网址") { keyboardTypeUri() }
                    KeyboardArea("邮箱") { keyboardTypeEmail() }
                    KeyboardArea("密码") { keyboardTypePassword() }
                    KeyboardArea("数字密码") { keyboardTypeNumberPassword() }
                    KeyboardArea("小数") { keyboardTypeDecimal() }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.SectionTitle(title: String) {
    Text {
        attr {
            marginTop(16f)
            fontSize(16f)
            fontWeightBold()
            color(Color(0xFF333333))
            text(title)
        }
    }
}

private fun ViewContainer<*, *>.KeyboardInput(
    label: String,
    placeholder: String = label,
    keyboard: InputAttr.() -> Unit = {}
) {
    Text {
        attr {
            marginTop(12f)
            fontSize(13f)
            color(Color(0xFF666666))
            text(label)
        }
    }
    Input {
        attr {
            marginTop(4f)
            height(44f)
            borderRadius(8f)
            fontSize(16f)
            color(Color.BLACK)
            backgroundColor(Color.WHITE)
            placeholder(placeholder)
            placeholderColor(Color(0xFFBBBBBB))
            keyboard()
        }
    }
}

private fun ViewContainer<*, *>.KeyboardArea(
    label: String,
    placeholder: String = label,
    keyboard: TextAreaAttr.() -> Unit = {}
) {
    Text {
        attr {
            marginTop(12f)
            fontSize(13f)
            color(Color(0xFF666666))
            text(label)
        }
    }
    TextArea {
        attr {
            marginTop(4f)
            height(72f)
            borderRadius(8f)
            fontSize(16f)
            color(Color.BLACK)
            backgroundColor(Color.WHITE)
            placeholder(placeholder)
            placeholderColor(Color(0xFFBBBBBB))
            keyboard()
        }
    }
}
