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
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reflection.*
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ReelectionExamplePage")
internal class ReelectionExamplePage : BasePager() {

    var title by observable("title")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = ctx.title
                }
            }
        }

    }

    override fun created() {
        super.created()
        if (pageData.isIOS) {
            val dic = "NSMutableDictionary2".callOC("new")
            val numberStr = "NSNumber".callOC("numberWithInt:", 344)
            dic.callToString("setObject:forKey:", numberStr, "test")
            title = dic.toString()
        } else if (pageData.isAndroid) {
            val version = "android.os.Build\$VERSION".getField("RELEASE")
            title = "系统版本：$version"
        }
    }
}