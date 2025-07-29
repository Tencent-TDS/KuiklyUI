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

package com.tencent.kuikly.demo.pages.base

import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.Module

class TDFTestModule : Module() {
    override fun moduleName(): String = MODULE_NAME

    companion object {
        const val MODULE_NAME = "TDFTestModule"
    }

    fun syncCall(
        a1: String?,
        a2: Int?,
        a3: Double?,
        a4: Boolean?,
        a5: Float?,
        a6: List<Any?>?,
        a7: Map<String, Any?>?,
    ) {
        toTDFNative(
            false,
            "syncCall",
            listOf(a1, a2, a3, a4, a5, a6, a7),
            null,
            null,
            true
        )
    }

    fun syncCallWithReturnValue(
        a1: String?,
        a2: Int?,
        a3: Double?,
        a4: Boolean?,
        a5: Float?,
        a6: List<Any?>?,
        a7: Map<String, Any?>?,
    ) : List<Any?>? {
        val result = toTDFNative(
            false,
            "syncCallWithReturnValue",
            listOf(a1, a2, a3, a4, a5, a6, a7),
            null,
            null,
            true
        )
        if (result.returnValue is List<*>) {
            return result.returnValue as List<Any?>
        }
        return null
    }

    fun asyncCall(isSuccess: Boolean) {
        toTDFNative(
            false,
            "asyncCall",
            listOf(isSuccess),
            successCallback = { result ->
                KLog.d("TDFModule", "asyncCall success callback: $result")
            },
            errorCallback = { result ->
                KLog.d("TDFModule", "asyncCall error callback: $result")
            },
            false
        )
    }

}