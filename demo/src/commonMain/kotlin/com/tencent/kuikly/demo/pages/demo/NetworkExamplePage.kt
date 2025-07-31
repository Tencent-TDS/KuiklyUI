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
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.views.layout.Row
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("NetworkExamplePage")
internal class NetworkExamplePage: BasePager() {

    private var output by observable("")
    private var src by observable("")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "网络请求示例"
                }
            }
            Row {
                Button {
                    attr {
                        size(150f, 40f)
                        borderRadius(20f)
                        marginLeft(10f)
                        marginTop(5f)
                        backgroundColor(Color(0x6200ee, 1f))
                        titleAttr {
                            text("requestGet")
                            color(Color.WHITE)
                        }
                        highlightBackgroundColor(Color.GRAY)
                    }
                    event {
                        click {
                            ctx.output = "requestGet..."
                            ctx.src = ""
                            ctx.requestGet()
                        }
                    }
                }
                Button {
                    attr {
                        size(150f, 40f)
                        borderRadius(20f)
                        marginLeft(10f)
                        marginTop(5f)
                        backgroundColor(Color(0x6200ee, 1f))
                        titleAttr {
                            text("requestGetBinary")
                            color(Color.WHITE)
                        }
                        highlightBackgroundColor(Color.GRAY)
                    }
                    event {
                        click {
                            ctx.output = "requestGetBinary..."
                            ctx.src = ""
                            ctx.requestGetBinary()
                        }
                    }
                }
            }
            Row {
                Button {
                    attr {
                        size(150f, 40f)
                        borderRadius(20f)
                        marginLeft(10f)
                        marginTop(5f)
                        backgroundColor(Color(0x6200ee, 1f))
                        titleAttr {
                            text("requestPost")
                            color(Color.WHITE)
                        }
                        highlightBackgroundColor(Color.GRAY)
                    }
                    event {
                        click {
                            ctx.output = "requestPost..."
                            ctx.src = ""
                            ctx.requestPost()
                        }
                    }
                }
                Button {
                    attr {
                        size(150f, 40f)
                        borderRadius(20f)
                        marginLeft(10f)
                        marginTop(5f)
                        backgroundColor(Color(0x6200ee, 1f))
                        titleAttr {
                            text("requestPostBinary")
                            color(Color.WHITE)
                        }
                        highlightBackgroundColor(Color.GRAY)
                    }
                    event {
                        click {
                            ctx.output = "requestPostBinary..."
                            ctx.src = ""
                            ctx.requestPostBinary()
                        }
                    }
                }
            }
            Row {
                Button {
                    attr {
                        size(150f, 40f)
                        borderRadius(20f)
                        marginLeft(10f)
                        marginTop(5f)
                        backgroundColor(Color(0x6200ee, 1f))
                        titleAttr {
                            text("status 204")
                            color(Color.WHITE)
                        }
                        highlightBackgroundColor(Color.GRAY)
                    }
                    event {
                        click {
                            ctx.output = "requestGet..."
                            ctx.src = ""
                            ctx.requestStatus204()
                        }
                    }
                }
            }
            View {
                attr {
                    marginLeft(10f)
                    marginRight(10f)
                    marginTop(5f)
                    alignSelfStretch()
                    border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                    height(200f)
                }
                Scroller {
                    attr {
                        padding(5f)
                        alignSelfStretch()
                        flex(1f)
                    }
                    Text {
                        attr {
                            text(ctx.output)
                            color(Color.BLACK)
                        }
                    }
                }
            }
            Image {
                attr {
                    size(200f, 150f)
                    marginLeft(10f)
                    marginTop(5f)
                    src(ctx.src)
                    resizeContain()
                }
            }
        }
    }

    private fun requestGet() {
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestGet(
            "https://httpbin.org/get",
            JSONObject().apply { put("key", "value") }
        ) { data, success, errorMsg, response ->
            output = """Get request completed:
                | success=$success,
                | 
                | data=$data,
                | 
                | errorMsg=$errorMsg,
                | 
                | statusCode=${response.statusCode},
                | 
                | headers=${response.headerFields}""".trimMargin()
        }
    }

    private fun requestGetBinary() {
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestGetBinary(
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/Dfnp7Q9F.png",
            JSONObject()
        ) { data, success, errorMsg, response ->
            output = """Get request completed:
                | success=$success,
                | 
                | errorMsg=$errorMsg,
                | 
                | statusCode=${response.statusCode},
                | 
                | headers=${response.headerFields}""".trimMargin()
            src = if (success) {
                "data:image/png;base64,${data.encodeBase64()}"
            } else {
                ""
                }
        }
    }

    private fun requestPost() {
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestPost(
            "https://httpbin.org/post",
            JSONObject().apply { put("key", "value") }
        ) { data, success, errorMsg, response ->
            output = """Post request completed:
                | success=$success,
                | 
                | data=$data,
                | 
                | errorMsg=$errorMsg,
                | 
                | statusCode=${response.statusCode},
                | 
                | headers=${response.headerFields}""".trimMargin()
        }
    }

    private fun requestPostBinary() {
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestPostBinary(
            "https://httpbin.org/post",
            "hello world".encodeToByteArray(),
        ) { data, success, errorMsg, response ->
            output = """Post request completed:
                | success=$success,
                | 
                | data=${data.decodeToString()},
                | 
                | errorMsg=$errorMsg,
                | 
                | statusCode=${response.statusCode},
                | 
                | headers=${response.headerFields}""".trimMargin()
        }
    }

    private fun requestStatus204() {
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestGet(
            "https://httpbin.org/status/204",
            JSONObject(),
        ) { data, success, errorMsg, response ->
            output = """Get request completed:
                | success=$success,
                | 
                | data=${data},
                | 
                | errorMsg=$errorMsg,
                | 
                | statusCode=${response.statusCode},
                | 
                | headers=${response.headerFields}""".trimMargin()
        }
    }

    private fun ByteArray.encodeBase64(): String {
        val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val result = StringBuilder((size + 2) / 3 * 4)
        var i = 0
        while (i < size) {
            val b0 = this[i].toInt() and 0xFF
            val b1 = if (i + 1 < size) this[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < size) this[i + 2].toInt() and 0xFF else 0
            result.append(table[b0 shr 2])
            result.append(table[((b0 and 0x03) shl 4) or (b1 shr 4)])
            if (i + 1 < size) {
                result.append(table[((b1 and 0x0F) shl 2) or (b2 shr 6)])
            } else {
                result.append('=')
            }
            if (i + 2 < size) {
                result.append(table[b2 and 0x3F])
            } else {
                result.append('=')
            }
            i += 3
        }
        return result.toString()
    }
}