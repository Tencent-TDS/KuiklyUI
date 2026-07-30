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

package com.tencent.kuikly.core.module

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/*
 * Kuikly返回键模块，用于通知宿主测是否消费返回键按下事件（Android、鸿蒙）
 */
class BackPressModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    fun backHandle(isConsumed: Boolean) {
        toNative(
            false,
            METHOD_BACK_HANDLE,
            param = JSONObject().apply {
                put("consumed", if (isConsumed) 1 else 0)
            }.toString(),
            syncCall = true
        )
    }

    companion object {
        const val MODULE_NAME = ModuleConst.BACK_PRESS
        const val METHOD_BACK_HANDLE = "backHandle"
    }
}