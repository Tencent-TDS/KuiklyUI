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

package com.tencent.kuikly.core.module.wx

/**
 * 微信小程序 API 封装模块名集中常量。
 *
 * 所有 WX*Module 的 moduleName() 必须从这里取，避免渲染层与业务层字符串不一致。
 * 这些 Module 只在微信小程序平台有实际实现，其它平台调用会走 `super.call(...)`
 * 返回 null，业务侧需要自行判断 `pageData.params.optString("is_wx_mp") == "1"`。
 */
object WXModuleConst {
    const val API        = "KRWXApiModule"        // 账号 / 登录 / 用户信息
    const val STORAGE    = "KRWXStorageModule"    // wx.setStorage 系列
    const val UI         = "KRWXUIModule"         // toast / modal / loading / actionSheet
    const val SYSTEM     = "KRWXSystemModule"     // 系统 / 设备 / 启动参数信息
    const val CLIPBOARD  = "KRWXClipboardModule"  // 剪贴板
    const val LOCATION   = "KRWXLocationModule"   // 定位
    const val SCAN       = "KRWXScanModule"       // 扫码
    const val MEDIA      = "KRWXMediaModule"      // 图片 / 多媒体
    const val SHARE      = "KRWXShareModule"      // 分享菜单

    // 兜底桥
    const val RAW        = "KRWXRawApiModule"     // 任意 wx.xxx 透传
}
