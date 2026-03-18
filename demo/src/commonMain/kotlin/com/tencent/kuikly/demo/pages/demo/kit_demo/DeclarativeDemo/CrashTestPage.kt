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
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar


@Page("CrashTestPage")
internal class CrashTestPage: BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(Color.WHITE)
                alignItemsCenter()
            }
            NavBar { attr { title = "Crash Test Page" } }
            Button {
                attr {
                    width(200f)
                    height(100f)
                    backgroundColor(Color.GRAY)
                    highlightBackgroundColor(Color.BLUE)
                    titleAttr {
                        text("Crash Now!")
                    }
                }
                event{
                    click {
                        val x = arrayOf(1)
                        print("${x[100]}")
                    }
                }
            }
        }
    }
}
