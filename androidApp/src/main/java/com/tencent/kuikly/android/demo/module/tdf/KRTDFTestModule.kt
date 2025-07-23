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

package com.tencent.kuikly.android.demo.module.tdf

import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.tdf.annotation.TDFMethod
import com.tencent.tdf.annotation.TDFModule
import com.tencent.tdf.module.TDFBaseModule
import com.tencent.tdf.module.TDFModuleContext
import com.tencent.tdf.module.TDFModulePromise
import java.lang.RuntimeException

@TDFModule(name = "TDFTestModule")
class KRTDFTestModule(context: TDFModuleContext?) : TDFBaseModule(context) {

    @TDFMethod
    fun syncCall(
        a1: String?,
        a2: Int?,
        a3: Double?,
        a4: Boolean?,
        a5: Float?,
        a6: List<Any?>?,
        a7: Map<String, Any?>?
    ) {
        KuiklyRenderLog.d(MODULE_NAME, "syncCall: a1: $a1, a2: $a2, a3: $a3, a4: $a4, a5: $a5, a6: $a6, a7:$a7")
    }

    @TDFMethod
    fun syncCallWithReturnValue(
        a1: String?,
        a2: Int?,
        a3: Double?,
        a4: Boolean?,
        a5: Float?,
        a6: List<Any?>?,
        a7: Map<String, Any?>?
    ) : List<Any?>{
        return listOf(1, 2, 3 ,"", listOf<Any>(4, 5, 6), mapOf("a" to 1, "b" to 2))
    }

    @TDFMethod
    fun asyncCall(isSuccess: Boolean, promise: TDFModulePromise) {
        if (isSuccess) {
            promise.resolve(mapOf("a" to 1, "b" to 2, "c" to  listOf<Any>(4, 5, 6)))
        } else {
            promise.reject(RuntimeException("asyncCall error!!!"))
        }
    }

    companion object {
        const val MODULE_NAME = "HRBridgeModule"
    }

}