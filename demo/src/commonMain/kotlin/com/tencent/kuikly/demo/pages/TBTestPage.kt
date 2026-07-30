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

import com.tencent.kuikly.compose.material3.Scaffold
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("TBTest")
internal class TBTestPage : BasePager() {

    var moduleRetureValue by observable("点击显示ArkTS层 Module返回值")
    var moduleRetureValue2 by observable("点击显示C++层 Module返回值 ")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }
            View {
                attr {
                    size(getPager().pageData.pageViewWidth, getPager().pageData.pageViewHeight)
                }
                Text {
                    attr {
                        marginLeft(10f)
                        text(ctx.moduleRetureValue)
                    }
                    event {
                        click {
                            ctx.moduleRetureValue = getPager().acquireModule<MyLogModule>("KRMyLogModule").test()
                        }
                    }
                }
                Text {
                    attr {
                        marginTop(30f)
                        marginLeft(10f)
                        text(ctx.moduleRetureValue2)
                        color(Color.BLUE)
                    }
                    event {
                        click {
                            ctx.moduleRetureValue2 = getPager().acquireModule<LogTestModule>("KRLogTestModule").test()
                        }
                    }
                }
            }
        }
    }

    override fun createExternalModules(): Map<String, Module>? {
        return mapOf(
            "KRMyLogModule" to MyLogModule(),
            "KRLogTestModule" to LogTestModule()
        )
    }
}
