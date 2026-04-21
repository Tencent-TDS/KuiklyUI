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

import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 微信小程序账号 / 登录 / 用户信息相关 API 的封装。
 *
 * 对应微信小程序 API：
 * - wx.login / wx.checkSession
 * - wx.getUserProfile / wx.getUserInfo
 * - wx.getAccountInfoSync
 *
 * 仅在微信小程序平台生效；其他平台调用会收到 `fail` 回调（errMsg = "not supported"）。
 *
 * 使用示例：
 * ```kotlin
 * acquireModule<WXApiModule>(WXApiModule.MODULE_NAME).login(
 *     onSuccess = { res -> KLog.i("tag", "code=" + res?.optString("code")) },
 *     onFail = { err -> KLog.e("tag", "login fail: $err") }
 * )
 * ```
 */
class WXApiModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /**
     * wx.login —— 获取登录凭证 code
     * @param timeout 超时毫秒，默认走微信默认超时
     */
    fun login(
        timeout: Int? = null,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            timeout?.also { put("timeout", it) }
        }
        invoke(METHOD_LOGIN, params, onSuccess, onFail)
    }

    /**
     * wx.checkSession —— 检查登录态是否过期
     */
    fun checkSession(
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        invoke(METHOD_CHECK_SESSION, null, onSuccess, onFail)
    }

    /**
     * wx.getUserProfile —— 获取用户信息（需要用户点击触发）
     * @param desc 声明获取用户信息后的用途，后续会展示在弹窗中
     * @param lang "en" / "zh_CN" / "zh_TW"，默认 "en"
     */
    fun getUserProfile(
        desc: String,
        lang: String = "en",
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            put("desc", desc)
            put("lang", lang)
        }
        invoke(METHOD_GET_USER_PROFILE, params, onSuccess, onFail)
    }

    /**
     * wx.getUserInfo —— 获取用户信息（旧版 API，不推荐）
     */
    fun getUserInfo(
        withCredentials: Boolean = false,
        lang: String = "en",
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            put("withCredentials", withCredentials)
            put("lang", lang)
        }
        invoke(METHOD_GET_USER_INFO, params, onSuccess, onFail)
    }

    /**
     * wx.getAccountInfoSync —— 同步获取当前小程序账号信息（appId、envVersion 等）
     * @return JSONObject，结构 { miniProgram: { appId, envVersion, version } }；非小程序平台返回 null
     */
    fun getAccountInfoSync(): JSONObject? {
        val raw = syncToNativeMethod(METHOD_GET_ACCOUNT_INFO_SYNC, null, null)
        return if (raw.isNullOrEmpty()) null else JSONObject(raw)
    }

    private fun invoke(
        method: String,
        params: JSONObject?,
        onSuccess: CallbackFn?,
        onFail: CallbackFn?,
    ) {
        val wrapped = JSONObject().apply {
            put(KEY_ARGS, params ?: JSONObject())
        }
        asyncToNativeMethod(method, wrapped) { data ->
            val ok = data?.optInt(KEY_OK, 0) == 1
            val payload = data?.optJSONObject(KEY_DATA)
            if (ok) {
                onSuccess?.invoke(payload)
            } else {
                onFail?.invoke(payload)
            }
        }
    }

    companion object {
        const val MODULE_NAME = WXModuleConst.API

        private const val METHOD_LOGIN = "login"
        private const val METHOD_CHECK_SESSION = "checkSession"
        private const val METHOD_GET_USER_PROFILE = "getUserProfile"
        private const val METHOD_GET_USER_INFO = "getUserInfo"
        private const val METHOD_GET_ACCOUNT_INFO_SYNC = "getAccountInfoSync"

        internal const val KEY_ARGS = "args"
        internal const val KEY_OK = "ok"
        internal const val KEY_DATA = "data"
    }
}
