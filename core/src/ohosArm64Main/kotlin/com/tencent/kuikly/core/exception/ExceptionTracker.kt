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

package com.tencent.kuikly.core.exception

import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import ohos.com_tencent_kuikly_GetAddress
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

typealias StackTraceProcessor = (t: Throwable) -> String

internal class BacktraceFrame{
    constructor(index: String, soName: String, addr: String, symbolInfo: String){
        this.index = index
        this.soName = soName
        this.addr = addr
        this.symbolInfo = symbolInfo
    }
    val index : String
    val soName : String
    val addr : String
    val symbolInfo : String

    override fun toString(): String {
        return "index:${index.padStart(2, '0')}, addr:$addr, so:${soName.trim()}, symbolInfo: $symbolInfo"
    }
}
object ExceptionTracker {

    private val SPLIT_PATTERN = Regex("\\s+")
    private var stackTraceProcessor: StackTraceProcessor? = null

    fun setStackTraceProcessor(processor: StackTraceProcessor?) {
        stackTraceProcessor = processor
    }

    fun notifyKuiklyException(t: Throwable) {
        val encoded = (stackTraceProcessor ?: ::stackTraceToString)(t)
        BridgeManager.callExceptionMethod(encoded)
    }
    @OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
    private fun stackTraceToString(t: Throwable): String{
        // Example:
        // 30  libkuikly.so                        0x7ba48d44d3       0x0 + 531041699027 (\/Applications\/DevEco-Studio.app\/Contents\/sdk\/default\/openharmony\/native\/llvm\/bin\/..\/include\/libcxx-ohos\/include\/c++\/v1\/__functional\/function.h:512:16)
        // 31  libkuikly.so                        0x7ba48d43a7       _ZNKSt4__n18functionIFvvEEclEv + 23 (\/Applications\/DevEco-Studio.app\/Contents\/sdk\/default\/openharmony\/native\/llvm\/bin\/..\/include\/libcxx-ohos\/include\/c++\/v1\/__functional\/function.h:1197:12)
        // (1) (2)                                 (3)                (4)
        // A regex to parse input into 4 parts as illustrated above
        val regex = Regex("(\\d+)\\s+(.*)\\s+(0x\\S+)\\s+(\\S+\\s+\\+\\s+.+)")
        val addr = t.getStackTraceAddresses()
        val name = t::class.simpleName ?: "Throwable"
        val message = t.message ?: ""
        val stack = t.getStackTrace().mapIndexed { index: Int, s: String ->
            var result = s
            regex.find(s)?.let{
                val frame = BacktraceFrame(it.groupValues[1], it.groupValues[2], it.groupValues[3], it.groupValues[4])
                val offset = (if (index < addr.size) com_tencent_kuikly_GetAddress(addr[index]) else 0).toString(16).padStart(16, '0')
                result = "#${frame.index.trim().padStart(2, '0')} pc $offset ${frame.soName.trim()} ${frame.symbolInfo}"
            }
            result
        }
        val error = JSONObject()
        error.put("name", name)
        error.put("message", message)
        error.put("stack", stack.joinToString("\n"))
        return error.toString()
    }
}