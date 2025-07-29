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

package com.tencent.kuikly.core.global

import com.tencent.kuikly.core.collection.fastHashMapOf

object GlobalFunctions {
    private var globalFunctionProducer = 0

    private val functionMap = fastHashMapOf<String, MutableMap<String, GlobalFunction>>()

    fun createFunction(pagerId: String, func: GlobalFunction): GlobalFunctionRef {
        val funcRef = globalFunctionProducer++.toString()
        if (!functionMap.containsKey(pagerId)) {
            functionMap[pagerId] = fastHashMapOf<String, GlobalFunction>()
        }
        functionMap[pagerId]?.also {
            it[funcRef] = func
        }
        return funcRef
    }

    fun invokeFunction(
        pagerId: String,
        funcRef: GlobalFunctionRef,
        data: Any? = null
    ) {
        functionMap[pagerId]?.also {
            val keepFuncAlive = it[funcRef]?.invoke(data)
            if (keepFuncAlive != true) {
                destroyGlobalFunction(pagerId, funcRef)
            }
        }
    }

    fun destroyGlobalFunction(pagerId: String, globalFunctionRef: GlobalFunctionRef) {
        functionMap[pagerId]?.also {
            it.remove(globalFunctionRef)
        }
    }

    fun destroyGlobalFunction(pagerId: String) {
        functionMap.remove(pagerId)
    }
}

typealias GlobalFunction = (data: Any?) -> KeepFuncAlive
typealias KeepFuncAlive = Boolean
typealias GlobalFunctionRef = String