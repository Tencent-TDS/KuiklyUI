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
import com.tencent.kuikly.core.module.CodecModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("CodecTestPager")
internal class CodecTestPager : BasePager() {

    private var urlEncode by observable("")
    private var urlDecode by observable("")
    private var base64Encode by observable("")
    private var base64Decode by observable("")
    private var md5 by observable("")
    private var sha256 by observable("")

    override fun body(): ViewBuilder {
        val ctx = this

        return {
            NavBar {
                attr {
                    title = "CodecTestPager"
                }
            }
            Scroller {
                attr {
                    flex(1f)
                }
                Text {
                    attr {
                        text("urlEncode" + ctx.urlEncode)
                    }
                }
                Text {
                    attr {
                        text("urlDecode" + ctx.urlDecode)
                    }
                }
                Text {
                    attr {
                        text("base64Encode" + ctx.base64Encode)
                    }
                }
                Text {
                    attr {
                        text("base64Decode" + ctx.base64Decode)
                    }
                }
                Text {
                    attr {
                        text("md5" + ctx.md5)
                    }
                }
                Text {
                    attr {
                        text("sha256" + ctx.sha256)
                    }
                }
            }

        }
    }

    override fun created() {
        super.created()
        val string = "你好，Kuikly！"
        urlEncode = acquireModule<CodecModule>(CodecModule.MODULE_NAME).urlEncode(string)
        urlDecode = acquireModule<CodecModule>(CodecModule.MODULE_NAME).urlDecode(urlEncode)

        base64Encode = acquireModule<CodecModule>(CodecModule.MODULE_NAME).base64Encode(string)
        base64Decode = acquireModule<CodecModule>(CodecModule.MODULE_NAME).base64Decode(base64Encode)
        md5 = acquireModule<CodecModule>(CodecModule.MODULE_NAME).md5(string)

        sha256 = acquireModule<CodecModule>(CodecModule.MODULE_NAME).sha256(string)

    }
}