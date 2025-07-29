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

package com.tencent.kuikly.demo.pages.router_page

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.utils.urlParams
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.base.Utils
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("router", supportInLocal = true)
internal class RouterPage : BasePager() {
    var inputText: String = ""
    lateinit var inputRef: ViewRef<InputView>
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            NavBar {
                attr {
                    title = TITLE
                    backDisable = true
                }
            }

            View {
                attr {
                    allCenter()
                    margin(20f)
                }
                View {
                    attr {
                        backgroundColor(Color.WHITE)
                        borderRadius(10f)
                        padding(10f)
                    }
                    Image {
                        attr {
                            src(LOGO)
                            size(
                                pagerData.pageViewWidth * 0.4f,
                                (pagerData.pageViewWidth * 0.4f) * (1678f / 2284f)
                            )
                        }
                    }
                }

            }

            View {
                attr {
                    flexDirectionRow()
                }
                View {
                    attr {
                        margin(all = 10f)
                        marginTop(0f)
                        height(40f)
                        flex(1f)
                        borderRadius(5f)
                    }
                    View {
                        attr {
                            absolutePositionAllZero()
                            backgroundLinearGradient(
                                Direction.TO_LEFT,
                                ColorStop(Color(0xFF23D3FD), 0f),
                                ColorStop(Color(0xFFAD37FE), 1f)
                            )
                        }
                        View {
                            attr {
                                absolutePosition(top = 1f, left = 1f, right = 1f, bottom = 1f)
                                backgroundColor(Color.WHITE)
                                borderRadius(5f)
                            }
                        }
                    }
                    Input {
                        ref {
                            ctx.inputRef = it
                        }
                        attr {
                            flex(1f)
                            fontSize(15f)
                            color(Color(0xFFAD37FE))
                            marginLeft(10f)
                            marginRight(10f)
                            placeholder(PLACEHOLDER)
                            autofocus(true)
                            placeholderColor(Color(0xAA23D3FD))

                        }
                        event {
                            textDidChange {
                                ctx.inputText = it.text
                            }
                        }
                    }
                }
                Button {
                    attr {
                        size(80f, 40f)
                        borderRadius(20f)
                        marginLeft(2f)
                        marginRight(15f)
                        backgroundLinearGradient(
                            Direction.TO_BOTTOM,
                            ColorStop(Color(0xAA23D3FD), 0f),
                            ColorStop(Color(0xAAAD37FE), 1f)
                        )

                        titleAttr {
                            text(JUMP_TEXT)
                            fontSize(17f)
                            color(Color.WHITE)
                            accessibility("跳转2")
                        }
                    }
                    event {
                        click {
                            if (ctx.inputText.isEmpty()) {
                                Utils.bridgeModule(this).toast("请输入PageName")
                            } else {
                                ctx.inputRef.view?.blur() // 失焦
                                getPager().acquireModule<SharedPreferencesModule>(
                                    SharedPreferencesModule.MODULE_NAME
                                ).setItem(
                                    CACHE_KEY, ctx.inputText
                                )
                                ctx.jumpPage(ctx.inputText)

                            }
                        }
                    }
                }

            }

            Text {
                attr {
                    fontSize(12f)
                    marginLeft(10f)
                    marginTop(5f)
                    text(AAR_MODE_TIP)

                    backgroundLinearGradient(
                        Direction.TO_RIGHT,
                        ColorStop(Color(0xFFAD37FE), 0f),
                        ColorStop(Color(0xFF23D3FD), 1f)
                    )

                }
            }

            View {
                attr {
                    allCenter()
                    margin(20f)
                }
                Text {
                    attr {
                        fontSize(20f)
                        text("APP原型Demo")
                        textDecorationUnderLine()
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFFAD37FE), 0f),
                            ColorStop(Color(0xFF23D3FD), 1f)
                        )
                    }
                }
                event {
                    click {
                        ctx.jumpPage("AppTabPage")
                    }
                }
            }

            View {
                attr {
                    allCenter()
                    margin(20f)
                }
                Text {
                    attr {
                        fontSize(20f)
                        text("Demo案例-Kuikly语法")
                        textDecorationUnderLine()
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFFAD37FE), 0f),
                            ColorStop(Color(0xFF23D3FD), 1f)
                        )

                    }
                }
                event {
                    click {
                        ctx.jumpPage("ExampleIndexPage")
                    }
                }
            }

            View {
                attr {
                    allCenter()
                    margin(20f)
                }
                Text {
                    attr {
                        fontSize(20f)
                        text("Demo案例-Compose语法")
                        textDecorationUnderLine()
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFFAD37FE), 0f),
                            ColorStop(Color(0xFF23D3FD), 1f)
                        )
                    }
                }
                event {
                    click {
                        ctx.jumpPage("ComposeAllSample")
                    }
                }
            }
        }

    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        val cacheInputText =
            getPager().acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)
                .getItem(
                    CACHE_KEY
                )
        if (cacheInputText.isNotEmpty()) {
            inputRef.view?.setText(cacheInputText)
        }
    }

    private fun jumpPage(inputText: String) {
        val params = urlParams("pageName=$inputText")
        val pageData = JSONObject()
        params.forEach {
            pageData.put(it.key, it.value)
        }
        val pageName = pageData.optString("pageName")

        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName, pageData)
    }

    companion object {
        const val PLACEHOLDER = "输入pageName（不区分大小写）"
        const val CACHE_KEY = "router_last_input_key2"
        const val BG_URL =
            "https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg"
        const val LOGO = "https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/Dfnp7Q9F.png"
        const val JUMP_TEXT = "跳转"
        const val TEXT_KEY = "text"
        const val TITLE = "Kuikly页面路由"
        private const val AAR_MODE_TIP = "如：router 或者 router&key=value （&后面为页面参数）"
    }

}
