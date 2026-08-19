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

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Kotlin wrapper for OHOS C++ [CallKotlinPerfTestModule] (demo/test only).
 * Timing comes from the native module (`cpp_toCValue` / `cpp_callKotlin`); no core hooks.
 */
internal class CallKotlinPerfTestModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    fun bench(iterations: Int = 5000): String {
        val params = JSONObject().apply { put("iterations", iterations) }
        return syncToNativeMethod(METHOD_BENCH, params, null)
    }

    fun benchPhases(iterations: Int = 2000, jsonBytes: Int = 1024, mode: String = "fire"): String {
        val params = JSONObject().apply {
            put("iterations", iterations)
            put("jsonBytes", jsonBytes)
            put("mode", mode)
        }
        return syncToNativeMethod(METHOD_BENCH_PHASES, params, null)
    }

    fun benchJson(iterationsPerSize: Int = 500): String {
        val params = JSONObject().apply { put("iterations", iterationsPerSize) }
        return syncToNativeMethod(METHOD_BENCH_JSON, params, null)
    }

    /** Native exports a nested NATIVE_JSON owner; platform actual runs lifecycle checks. */
    fun testNestedLifecycle(): String {
        val raw = syncToNativeMethod(METHOD_EXPORT_NESTED_OWNER, JSONObject(), null)
        val owner = try {
            JSONObject(raw).optLong("owner")
        } catch (_: Throwable) {
            return """{"ok":false,"error":"bad export: $raw"}"""
        }
        if (owner == 0L) {
            return """{"ok":false,"error":"export failed: $raw"}"""
        }
        return runNestedCJsonLifecycleTest(owner)
    }

    companion object {
        const val MODULE_NAME = "CallKotlinPerfTestModule"
        private const val METHOD_BENCH = "bench"
        private const val METHOD_BENCH_PHASES = "benchPhases"
        private const val METHOD_BENCH_JSON = "benchJson"
        private const val METHOD_EXPORT_NESTED_OWNER = "exportNestedOwner"
    }
}
