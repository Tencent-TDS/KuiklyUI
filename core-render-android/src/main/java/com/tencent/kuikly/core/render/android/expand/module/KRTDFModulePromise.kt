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

import com.tencent.kuikly.core.render.android.css.ktx.toTDFModuleCallResult
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.tencent.tdf.module.TDFModulePromise

class KRTDFModulePromise(
    private val callId: String,
    private val successCallback: KuiklyRenderCallback?,
    private val errorCallback: KuiklyRenderCallback?)
    : TDFModulePromise {

    companion object {
        const val CALL_ID_NO_CALLBACK = "-1"
    }

    override fun getCallId(): String = callId

    override fun isCallback(): Boolean = callId != CALL_ID_NO_CALLBACK

    override fun resolve(result: Any?) {
        successCallback?.invoke(result?.toTDFModuleCallResult())
    }

    override fun reject(error: Any?) {
        errorCallback?.invoke(error?.toTDFModuleCallResult())
    }

}