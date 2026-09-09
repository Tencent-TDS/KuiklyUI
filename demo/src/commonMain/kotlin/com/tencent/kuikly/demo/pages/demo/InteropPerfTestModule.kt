/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.module.AnyCallbackFn
import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Keep-alive callbacks for the ArkTS -> Kotlin payload benches.
 *
 * Every payload case is identified by [caseName], so the ArkTS side can cache
 * prepared payloads per case and report results back to the same slot.
 */
internal class InteropPerfTestModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    fun setStatsCallback(callbackFn: CallbackFn) {
        toNative(
            keepCallbackAlive = true,
            methodName = "SetStatsCallback",
            param = null,
            callback = callbackFn,
            syncCall = true
        )
    }

    fun setCallbackWithPlainString(caseName: String, callbackFn: AnyCallbackFn) {
        val callbackRef = GlobalFunctions.createFunction(pagerId) { data ->
            callbackFn(data)
            true
        }
        BridgeManager.callModuleMethod(
            pagerId,
            moduleName(),
            "SetCallbackWithPlainString",
            caseName,
            callbackRef,
            syncCallValue(syncCall = true, keepCallbackAlive = true)
        )
    }

    fun setCallbackWithJson(caseName: String, callbackFn: CallbackFn) {
        toNative(
            keepCallbackAlive = true,
            methodName = "SetCallbackWithJson",
            param = caseName,
            callback = callbackFn,
            syncCall = true
        )
    }

    fun runPlainString(caseName: String, count: Int, payloadChars: Int) {
        runConfig("RunPlainString", caseName, count, payloadChars)
    }

    fun runJsonString(caseName: String, count: Int, payloadChars: Int) {
        runConfig("RunJsonString", caseName, count, payloadChars)
    }

    fun runKRRecord(caseName: String, count: Int, payloadChars: Int) {
        runConfig("RunKRRecord", caseName, count, payloadChars)
    }

    private fun runConfig(methodName: String, caseName: String, count: Int, payloadChars: Int) {
        val config = JSONObject()
            .put("case", caseName)
            .put("count", count)
            .put("payloadChars", payloadChars)
        toNative(
            keepCallbackAlive = false,
            methodName = methodName,
            param = config.toString(),
            callback = null,
            syncCall = false
        )
    }

    private fun syncCallValue(syncCall: Boolean, keepCallbackAlive: Boolean): Int {
        return if (pageData?.isOhOs == true) {
            (if (syncCall) 1 else 0) + if (keepCallbackAlive) CALLBACK_KEEP_ALIVE_MASK else 0
        } else {
            if (syncCall) 1 else 0
        }
    }

    companion object {
        const val MODULE_NAME = "KRInteropPerfTestModule"
        private const val CALLBACK_KEEP_ALIVE_MASK = 2
    }
}
