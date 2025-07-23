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
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * Created by kam on 2023/10/31.
 */
@Page("line_height_bug_page")
internal class AndroidLineHeightWithLineBreakBugPager : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Text {
                attr {
                    // 修复Android文本中带有\n时，lineHeight失效
                    text("哈哈哈我啊\n哈啊啊啊啊 啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊我啊哈啊啊啊啊 啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊我啊哈啊啊啊啊 啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊" +
                            "我啊哈啊啊啊啊 啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊我啊哈啊啊啊啊 啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊我啊哈啊啊啊啊 啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊" +
                            "我啊哈啊啊啊啊 啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊我啊哈啊啊啊啊 啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊")
                    fontSize(20f)
                    lineHeight(50f)
                }
            }
        }
    }
}