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

package com.tencent.kuikly.demo.pages.base.ktx

import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IPager
import com.tencent.kuikly.demo.pages.base.*
import com.tencent.kuikly.demo.pages.base.extension.userData

/**
 * 通过通知的方式封装了一个解决页面间跳转回参问题的方法，A 页面通过 [openPageForResult] 打开 B 页面，然后 B 页面通过
 * [callbackResult] 方法回参，A 能够在 [callbackFn] 中收到回调
 */
internal fun IPager.openPageForResult(url: String, closeCurPage: Boolean = false, closeSamePage: Boolean = false, userData: JSONObject = JSONObject(), callbackFn: CallbackFn? = null) {
    val bridgeModule = acquireModule<BridgeModule>(BridgeModule.MODULE_NAME)
    val notifyModule = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
    if (callbackFn == null) {
        bridgeModule.openPage(url, closeCurPage)
        return
    }
    val callbackId = bridgeModule.currentTimeStamp()
    userData.put("callbackId", callbackId)
    bridgeModule.openPage(url, closeCurPage, closeSamePage, userData)
    notifyModule.addNotify("$callbackId", cb = callbackFn)
}

/**
 * 配合 [openPageForResult] 使用
 */
internal fun IPager.callbackResult(param: JSONObject) {
    val callbackId = pageData.userData.optLong("callbackId")
    if (callbackId == 0L) run {
        KLog.e("callbackResult", "this page has no callbackId")
        return
    }
    val notifyModule = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
    notifyModule.postNotify("$callbackId", param)
}
