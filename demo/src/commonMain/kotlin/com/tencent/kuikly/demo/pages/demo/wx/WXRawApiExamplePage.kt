/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 */

package com.tencent.kuikly.demo.pages.demo.wx

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.wx.WXRawApiModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.wx.WXButton
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

/**
 * 微信小程序 API 兜底桥（[WXRawApiModule]）Demo。
 *
 * 展示如何在 **没有专门封装 Module** 的情况下，直接调用任意 `wx.xxx`：
 * - wx.vibrateShort       / wx.vibrateLong        （体感）
 * - wx.setNavigationBarTitle                      （动态设置导航条）
 * - wx.navigateToMiniProgram                      （跳转小程序，演示失败回调）
 * - wx.getBatteryInfoSync                         （同步调用）
 *
 * 当 Kuikly 未封装某 API 时，推荐先用本兜底桥快速接入；
 * 当业务上反复使用，建议再沉淀为业务自有的 Module（参考文档 DevGuide/miniapp-wx-apis.md）。
 */
@Page("WXRawApiExamplePage")
internal class WXRawApiExamplePage : BasePager() {

    private var vibrateTip by observable("尚未调用")
    private var titleTip by observable("尚未修改")
    private var navigateTip by observable("尚未调用")
    private var batteryTip by observable("尚未获取")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "WX Raw API Demo" } }
            List {
                attr { flex(1f) }

                ViewExampleSectionHeader { attr { title = "用法说明" } }
                View {
                    attr { padding(left = 16f, right = 16f, top = 8f, bottom = 12f) }
                    Text {
                        attr {
                            fontSize(12f)
                            color(0xFF666666)
                            text("通过 WXRawApiModule 可直接调用任意 wx.xxx。\n" +
                                "调用规则：call(apiName, args, onSuccess, onFail)；\n" +
                                "同步方法（xxxSync）用 callSync(apiName, args) 直接取结果。")
                        }
                    }
                }

                // ----- 异步示例 -----
                ViewExampleSectionHeader { attr { title = "异步调用 call(...)" } }
                rawRow("wx.vibrateShort") {
                    ctx.acquireModule<WXRawApiModule>(WXRawApiModule.MODULE_NAME).call(
                        apiName = "vibrateShort",
                        args = JSONObject().apply { put("type", "heavy") },
                        onSuccess = { ctx.vibrateTip = "已震动 (heavy)" },
                        onFail = { err -> ctx.vibrateTip = "fail ${err?.optString("errMsg")}" }
                    )
                }
                rawRow("wx.vibrateLong") {
                    ctx.acquireModule<WXRawApiModule>(WXRawApiModule.MODULE_NAME).call(
                        apiName = "vibrateLong",
                        onSuccess = { ctx.vibrateTip = "已长震" },
                        onFail = { err -> ctx.vibrateTip = "fail ${err?.optString("errMsg")}" }
                    )
                }
                tipRow { ctx.vibrateTip }

                rawRow("wx.setNavigationBarTitle") {
                    ctx.acquireModule<WXRawApiModule>(WXRawApiModule.MODULE_NAME).call(
                        apiName = "setNavigationBarTitle",
                        args = JSONObject().apply { put("title", "Raw API Title ${kotlin.random.Random.nextInt(100)}") },
                        onSuccess = { ctx.titleTip = "导航条标题已修改（仅对宿主导航条生效）" },
                        onFail = { err -> ctx.titleTip = "fail ${err?.optString("errMsg")}" }
                    )
                }
                tipRow { ctx.titleTip }

                rawRow("wx.navigateToMiniProgram (未配置会 fail)") {
                    ctx.acquireModule<WXRawApiModule>(WXRawApiModule.MODULE_NAME).call(
                        apiName = "navigateToMiniProgram",
                        args = JSONObject().apply {
                            put("appId", "wx_demo_app_id_not_exist")
                            put("path", "pages/index/index")
                        },
                        onSuccess = { ctx.navigateTip = "跳转成功" },
                        onFail = { err ->
                            ctx.navigateTip = "fail（预期内）: ${err?.optString("errMsg")}"
                            KLog.i(TAG, "navigate fail as expected: $err")
                        }
                    )
                }
                tipRow { ctx.navigateTip }

                // ----- 同步示例 -----
                ViewExampleSectionHeader { attr { title = "同步调用 callSync(...)" } }
                rawRow("wx.getBatteryInfoSync") {
                    val info = ctx.acquireModule<WXRawApiModule>(WXRawApiModule.MODULE_NAME)
                        .callSync("getBatteryInfoSync")
                    ctx.batteryTip = "level=" + (info?.optString("level") ?: "-") +
                        ", isCharging=" + (info?.optBoolean("isCharging") ?: false)
                }
                tipRow { ctx.batteryTip }

                // ----- 高级示例：向任意 API 传数组 -----
                ViewExampleSectionHeader { attr { title = "进阶：传递数组参数" } }
                rawRow("wx.showToast (via raw)") {
                    ctx.acquireModule<WXRawApiModule>(WXRawApiModule.MODULE_NAME).call(
                        apiName = "showToast",
                        args = JSONObject().apply {
                            put("title", "from raw bridge")
                            put("icon", "none")
                            put("duration", 1200)
                        }
                    )
                }
                rawRow("wx.chooseImage (via raw)") {
                    ctx.acquireModule<WXRawApiModule>(WXRawApiModule.MODULE_NAME).call(
                        apiName = "chooseImage",
                        args = JSONObject().apply {
                            put("count", 1)
                            put("sizeType", JSONArray().apply { put("compressed") })
                            put("sourceType", JSONArray().apply { put("album"); put("camera") })
                        },
                        onSuccess = { res ->
                            val paths = res?.optJSONArray("tempFilePaths")
                            KLog.i(TAG, "chooseImage path=${paths?.opt(0)}")
                        }
                    )
                }

                View { attr { height(40f) } }
            }
        }
    }

    companion object {
        private const val TAG = "WXRawApiExamplePage"
    }
}

private fun ViewContainer<*, *>.rawRow(label: String, onTap: () -> Unit) {
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            padding(left = 16f, right = 16f, top = 6f, bottom = 6f)
        }
        Text {
            attr {
                flex(1f)
                fontSize(14f)
                color(0xFF333333)
                text(label)
            }
        }
        WXButton {
            attr {
                type("primary")
                size("mini")
                titleAttr {
                    text("调用")
                    fontSize(13f)
                    color(Color.WHITE)
                }
            }
            event {
                click { onTap() }
            }
        }
    }
}

private fun ViewContainer<*, *>.tipRow(provider: () -> String) {
    View {
        attr { padding(left = 16f, right = 16f, bottom = 6f) }
        Text {
            attr {
                fontSize(12f)
                color(0xFF888888)
                text(provider())
            }
        }
    }
}
