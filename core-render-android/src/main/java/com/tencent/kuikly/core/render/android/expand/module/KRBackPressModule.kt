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

package com.tencent.kuikly.core.render.android.expand.module

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback

/**
 *  监听BackPress消费状态回调
 *
 *  created by zhenhuachen on 2025/6/09.
 */
class KRBackPressModule : KuiklyRenderBaseModule() {

    var isBackConsumed = false
    var backConsumedTime = 0L

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_BACK_HANDLE -> backHandle(params)
            else -> super.call(method, params, callback)
        }
    }

    private fun backHandle(params: String?) {
        if (params != null) {
            isBackConsumed = JSONObject(params).optInt("consumed", 0) == 1
        } else {
            isBackConsumed = false
        }
        backConsumedTime = System.currentTimeMillis()
    }

    companion object {
        const val MODULE_NAME = "KRBackPressModule"
        const val METHOD_BACK_HANDLE = "backHandle"
    }
}