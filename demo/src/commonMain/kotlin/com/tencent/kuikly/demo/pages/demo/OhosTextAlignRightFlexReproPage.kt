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
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Regression page for multiline Chinese glyph corruption after relayouting a
 * flex(1f) and textAlignRight() text node on OHOS.
 */
@Page("OhosTextAlignRightFlexRepro")
internal class OhosTextAlignRightFlexReproPage : BasePager() {

    override fun body(): ViewBuilder {
        return {
            NavBar {
                attr {
                    width(pagerData.pageViewWidth)
                    title = "TextAlignRight + Flex"
                }
            }
            View {
                attr {
                    flex(1f)
                    backgroundColor(Color.WHITE)
                    flexDirectionColumn()
                }
                Text {
                    attr {
                        text("flex(1f) + textAlignRight() 多行中文回归")
                        fontSize(14f)
                        color(Color(0xFF999999))
                        marginBottom(10f)
                        marginTop(16f)
                    }
                }
                val kvList = arrayListOf(
                    "策略类型" to "开板卖出",
                    "触发条件" to "封单手数小于或等于30.00万手，或开板即触发",
                    "委托价格" to "市价",
                    "委托数量" to "1000股",
                    "有效期" to "至2026年09月02日收盘前（距今1天）",
                    "止损价" to "封单手数小于或等于50.00万手，或开板即触发",
                    "追加条件" to "封单手数小于或等于100.00万手，或开板即触发且成交量放大",
                )
                kvList.forEach { (label, value) ->
                    View {
                        attr {
                            flexDirectionRow()
                            justifyContentSpaceBetween()
                            marginBottom(12f)
                            alignItems(FlexAlign.FLEX_START)
                            width(pagerData.pageViewWidth - 32f)
                            marginLeft(16f)
                            marginRight(16f)
                        }
                        Text {
                            attr {
                                text(label)
                                fontSize(16f)
                                fontWeightNormal()
                                lineHeight(22f)
                                color(Color(0xFF999999))
                                marginRight(12f)
                            }
                        }
                        Text {
                            attr {
                                text(value)
                                fontSize(16f)
                                fontWeightNormal()
                                lineHeight(22f)
                                color(Color(0xFF333333))
                                flex(1f)
                                textAlignRight()
                            }
                        }
                    }
                }
            }
        }
    }
}
