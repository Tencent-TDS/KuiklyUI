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
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 输入控件对比入口页（仅 OHOS 有实际切换效果，其它平台标志位无意义）。
 *
 * 两个按钮：
 *  - 进入老控件测试：openPage("InputCompareDemoPage", {"useTextEditorImpl": "0"})
 *  - 进入新控件测试：openPage("InputCompareDemoPage", {"useTextEditorImpl": "1"})
 *
 * 鸿蒙宿主在 AppKRRouterAdapter.openPage 中根据 pageName/useTextEditorImpl 参数，
 * 在真正跳转前调用 libkuikly_entry.so 暴露的 setUseNewTextInputComponent
 * 写入 OHOS 侧全局标志位（该标志位实际存储在 libkuikly.so 内，通过 C API 跨 so 透传），
 * 无需业务层通过 BridgeModule 显式下发。iOS/Android 则直接忽略该参数。
 */
@Page("InputCompareEntryPage")
internal class InputCompareEntryPage : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            NavBar {
                attr {
                    title = "输入控件 新/老对比入口"
                }
            }

            View {
                attr {
                    flex(1f)
                    padding(16f)
                }

                // 说明文本
                Text {
                    attr {
                        fontSize(13f)
                        color(Color(0xFF666666L))
                        marginTop(8f)
                        value(
                            "该入口用于对比 OpenHarmony 下两种 Input 实现：\n" +
                                "• 老实现：ARKUI_NODE_TEXT_INPUT / ARKUI_NODE_TEXT_AREA\n" +
                                "• 新实现：ARKUI_NODE_TEXT_EDITOR（仅 API>=24 生效）\n\n" +
                                "选择下方任一按钮后，会先设置全局开关再跳转到同一对比测试页面。"
                        )
                    }
                }

                // 按钮：老控件
                View {
                    attr {
                        marginTop(24f)
                        height(48f)
                        backgroundColor(Color(0xFF888888L))
                        borderRadius(6f)
                        allCenter()
                    }
                    event {
                        click { ctx.openComparePage(useNew = false) }
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.WHITE)
                            value("进入老控件测试（flag=0）")
                        }
                    }
                }

                // 按钮：新控件
                View {
                    attr {
                        marginTop(12f)
                        height(48f)
                        backgroundColor(Color(0xFF3C6CBDL))
                        borderRadius(6f)
                        allCenter()
                    }
                    event {
                        click { ctx.openComparePage(useNew = true) }
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.WHITE)
                            value("进入新控件测试（flag=1，需 API>=24）")
                        }
                    }
                }

                // 提示
                Text {
                    attr {
                        fontSize(12f)
                        color(Color(0xFF999999L))
                        marginTop(16f)
                        value(
                            "提示：标志位只影响「设置后新创建」的 Input/TextArea；\n" +
                                "若当前进程已存在残留的老实例，不会被替换为新实现，反之亦然。"
                        )
                    }
                }
            }
        }
    }

    /**
     * 直接跳转到对比测试页，把 useTextEditorImpl 通过 pageData 传给宿主。
     * OHOS 宿主在 AppKRRouterAdapter.openPage 中读取该参数并写入 OHOS 侧全局标志位，
     * 同步完成后再进行 pushUrl，所以这里无需额外延时或竞争保护。
     */
    private fun openComparePage(useNew: Boolean) {
        val flagValue = if (useNew) 1 else 0
        val userData = JSONObject()
        userData.put(InputCompareDemoPage.PARAM_USE_TEXT_EDITOR_IMPL, flagValue.toString())
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(
            "InputCompareDemoPage",
            userData
        )
    }

    companion object {
        private const val TAG = "InputCompareEntryPage"
    }
}
