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
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.wx.module.WXApiModule
import com.tencent.kuikly.core.wx.module.WXClipboardModule
import com.tencent.kuikly.core.wx.module.WXLocationModule
import com.tencent.kuikly.core.wx.module.WXMediaModule
import com.tencent.kuikly.core.wx.module.WXScanModule
import com.tencent.kuikly.core.wx.module.WXShareModule
import com.tencent.kuikly.core.wx.module.WXStorageModule
import com.tencent.kuikly.core.wx.module.WXSystemModule
import com.tencent.kuikly.core.wx.module.WXUIModule
import com.tencent.kuikly.core.wx.registerWXModules
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.wx.views.WXButton
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

/**
 * 微信小程序 API 封装 Demo 页（强类型 Module）。
 *
 * 覆盖 9 个强类型 Module：
 * - WXApiModule / WXUIModule / WXSystemModule / WXStorageModule
 * - WXClipboardModule / WXLocationModule / WXScanModule / WXMediaModule / WXShareModule
 *
 * 兜底桥（WXRawApiModule）示例请见 [WXRawApiExamplePage]。
 */
@Page("WXApiExamplePage")
internal class WXApiExamplePage : BasePager() {

    override fun createExternalModules(): Map<String, Module>? {
        // 注册微信小程序 API Modules（仅小程序平台生效，其他平台为空操作）
        registerWXModules()
        return super.createExternalModules()
    }

    private var loginTip by observable("尚未登录")
    private var userTip by observable("尚未获取")
    private var accountTip by observable("尚未获取")
    private var systemTip by observable("尚未获取")
    private var storageTip by observable("尚未读取")
    private var clipTip by observable("尚未读取")
    private var locationTip by observable("尚未定位")
    private var scanTip by observable("尚未扫码")
    private var mediaTip by observable("尚未选择")
    private var shareTip by observable("尚未操作")
    private var modalTip by observable("尚未点击")
    private var actionTip by observable("尚未点击")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "WX API Demo" } }
            List {
                attr { flex(1f) }

                // ----- WXApiModule -----
                ViewExampleSectionHeader { attr { title = "WXApiModule 账号 / 登录" } }
                apiRow("wx.login") {
                    ctx.loginTip = "请求中..."
                    ctx.acquireModule<WXApiModule>(WXApiModule.MODULE_NAME).login(
                        onSuccess = { res ->
                            ctx.loginTip = "code=" + (res?.optString("code") ?: "-")
                            KLog.i(TAG, "login ok: $res")
                        },
                        onFail = { err ->
                            ctx.loginTip = "fail: ${err?.optString("errMsg")}"
                            KLog.e(TAG, "login fail: $err")
                        }
                    )
                }
                apiRow("wx.checkSession") {
                    ctx.acquireModule<WXApiModule>(WXApiModule.MODULE_NAME).checkSession(
                        onSuccess = { ctx.loginTip = "session 有效" },
                        onFail = { ctx.loginTip = "session 失效" }
                    )
                }
                apiRow("wx.getUserProfile") {
                    ctx.acquireModule<WXApiModule>(WXApiModule.MODULE_NAME).getUserProfile(
                        desc = "用于演示 Kuikly 封装",
                        onSuccess = { res ->
                            ctx.userTip = res?.optJSONObject("userInfo")?.optString("nickName") ?: "-"
                        },
                        onFail = { err -> ctx.userTip = "fail: ${err?.optString("errMsg")}" }
                    )
                }
                apiRow("wx.getAccountInfoSync") {
                    val info = ctx.acquireModule<WXApiModule>(WXApiModule.MODULE_NAME)
                        .getAccountInfoSync()
                    ctx.accountTip = info?.optJSONObject("miniProgram")
                        ?.optString("appId") ?: "-"
                }
                tipRow { "login: ${ctx.loginTip} | user: ${ctx.userTip} | appId: ${ctx.accountTip}" }

                // ----- WXUIModule -----
                ViewExampleSectionHeader { attr { title = "WXUIModule 交互控件" } }
                apiRow("wx.showToast") {
                    ctx.acquireModule<WXUIModule>(WXUIModule.MODULE_NAME)
                        .showToast(title = "Hello Kuikly", icon = "success", duration = 1500)
                }
                apiRow("wx.showLoading 1.5s") {
                    val ui = ctx.acquireModule<WXUIModule>(WXUIModule.MODULE_NAME)
                    ui.showLoading("加载中...")
                    ctx.setTimeout(1500) { ui.hideLoading() }
                }
                apiRow("wx.showModal") {
                    ctx.acquireModule<WXUIModule>(WXUIModule.MODULE_NAME).showModal(
                        title = "标题",
                        content = "这是一个 modal 示例",
                        onSuccess = { res ->
                            ctx.modalTip = if (res?.optBoolean("confirm") == true) "点了确定" else "点了取消"
                        }
                    )
                }
                apiRow("wx.showActionSheet") {
                    ctx.acquireModule<WXUIModule>(WXUIModule.MODULE_NAME).showActionSheet(
                        itemList = listOf("选项 A", "选项 B", "选项 C"),
                        onSuccess = { res ->
                            ctx.actionTip = "选择了第 ${res?.optInt("tapIndex", -1)} 项"
                        },
                        onFail = { ctx.actionTip = "取消" }
                    )
                }
                tipRow { "modal: ${ctx.modalTip} | actionSheet: ${ctx.actionTip}" }

                // ----- WXSystemModule -----
                ViewExampleSectionHeader { attr { title = "WXSystemModule 系统信息" } }
                apiRow("wx.getWindowInfo") {
                    val w = ctx.acquireModule<WXSystemModule>(WXSystemModule.MODULE_NAME)
                        .getWindowInfoSync()
                    ctx.systemTip = "window=" + (w?.toString()?.take(80) ?: "-")
                }
                apiRow("wx.getDeviceInfo") {
                    val d = ctx.acquireModule<WXSystemModule>(WXSystemModule.MODULE_NAME)
                        .getDeviceInfoSync()
                    ctx.systemTip = "device.model=" + (d?.optString("model") ?: "-")
                }
                apiRow("wx.getLaunchOptionsSync") {
                    val o = ctx.acquireModule<WXSystemModule>(WXSystemModule.MODULE_NAME)
                        .getLaunchOptionsSync()
                    ctx.systemTip = "launch.scene=" + (o?.optInt("scene", 0) ?: "-")
                }
                tipRow { ctx.systemTip }

                // ----- WXStorageModule -----
                ViewExampleSectionHeader { attr { title = "WXStorageModule 存储" } }
                apiRow("setStorageSync") {
                    val value = "kuikly_demo_value_" + kotlin.random.Random.nextInt(1000)
                    ctx.acquireModule<WXStorageModule>(WXStorageModule.MODULE_NAME)
                        .setStorageSync(STORAGE_KEY, value)
                    ctx.storageTip = "已写入: $value"
                }
                apiRow("getStorageSync") {
                    val v = ctx.acquireModule<WXStorageModule>(WXStorageModule.MODULE_NAME)
                        .getStorageSync(STORAGE_KEY)
                    ctx.storageTip = "读取=$v"
                }
                apiRow("removeStorage") {
                    ctx.acquireModule<WXStorageModule>(WXStorageModule.MODULE_NAME).removeStorage(
                        key = STORAGE_KEY,
                        onSuccess = { ctx.storageTip = "已删除" },
                        onFail = { err -> ctx.storageTip = "fail ${err?.optString("errMsg")}" }
                    )
                }
                tipRow { ctx.storageTip }

                // ----- WXClipboardModule -----
                ViewExampleSectionHeader { attr { title = "WXClipboardModule 剪贴板" } }
                apiRow("setClipboardData") {
                    ctx.acquireModule<WXClipboardModule>(WXClipboardModule.MODULE_NAME)
                        .setClipboardData("https://github.com/Tencent-TDS/KuiklyUI",
                            onSuccess = { ctx.clipTip = "已写入剪贴板" })
                }
                apiRow("getClipboardData") {
                    ctx.acquireModule<WXClipboardModule>(WXClipboardModule.MODULE_NAME)
                        .getClipboardData(
                            onSuccess = { res -> ctx.clipTip = "data=" + (res?.optString("data") ?: "-") },
                            onFail = { err -> ctx.clipTip = "fail ${err?.optString("errMsg")}" }
                        )
                }
                tipRow { ctx.clipTip }

                // ----- WXLocationModule -----
                ViewExampleSectionHeader { attr { title = "WXLocationModule 定位" } }
                apiRow("getLocation") {
                    ctx.acquireModule<WXLocationModule>(WXLocationModule.MODULE_NAME).getLocation(
                        onSuccess = { res ->
                            val lat = res?.optString("latitude") ?: "-"
                            val lng = res?.optString("longitude") ?: "-"
                            ctx.locationTip = "lat=$lat, lng=$lng"
                        },
                        onFail = { err -> ctx.locationTip = "fail ${err?.optString("errMsg")}" }
                    )
                }
                tipRow { ctx.locationTip }

                // ----- WXScanModule -----
                ViewExampleSectionHeader { attr { title = "WXScanModule 扫码" } }
                apiRow("scanCode") {
                    ctx.acquireModule<WXScanModule>(WXScanModule.MODULE_NAME).scanCode(
                        onSuccess = { res ->
                            ctx.scanTip = "result=" + (res?.optString("result") ?: "-")
                        },
                        onFail = { err -> ctx.scanTip = "fail ${err?.optString("errMsg")}" }
                    )
                }
                tipRow { ctx.scanTip }

                // ----- WXMediaModule -----
                ViewExampleSectionHeader { attr { title = "WXMediaModule 图片 / 多媒体" } }
                apiRow("chooseImage (1张)") {
                    ctx.acquireModule<WXMediaModule>(WXMediaModule.MODULE_NAME).chooseImage(
                        count = 1,
                        onSuccess = { res ->
                            val paths = res?.optJSONArray("tempFilePaths")
                            ctx.mediaTip = "tempFile=" + (paths?.opt(0)?.toString() ?: "-")
                        },
                        onFail = { err -> ctx.mediaTip = "fail ${err?.optString("errMsg")}" }
                    )
                }
                tipRow { ctx.mediaTip }

                // ----- WXShareModule -----
                ViewExampleSectionHeader { attr { title = "WXShareModule 分享菜单" } }
                apiRow("showShareMenu") {
                    ctx.acquireModule<WXShareModule>(WXShareModule.MODULE_NAME).showShareMenu(
                        onSuccess = { ctx.shareTip = "已启用分享菜单（右上角）" },
                        onFail = { err -> ctx.shareTip = "fail ${err?.optString("errMsg")}" }
                    )
                }
                apiRow("hideShareMenu") {
                    ctx.acquireModule<WXShareModule>(WXShareModule.MODULE_NAME).hideShareMenu(
                        onSuccess = { ctx.shareTip = "已隐藏分享菜单" }
                    )
                }
                tipRow { ctx.shareTip }

                View { attr { height(40f) } }
            }
        }
    }

    companion object {
        private const val TAG = "WXApiExamplePage"
        private const val STORAGE_KEY = "kuikly_demo_key"
    }
}

/** 一行 "label ——  [调用]" 的 demo 行，点击按钮触发 [onTap]。 */
private fun ViewContainer<*, *>.apiRow(label: String, onTap: () -> Unit) {
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

/** 一行淡灰色小字提示行，内容由 [provider] 惰性返回（会随 observable 响应刷新）。 */
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