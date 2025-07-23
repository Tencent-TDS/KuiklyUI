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

package com.tencent.kuikly.core.utils

import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.cinterop.*
import ohos.KRRenderCValue
import ohos.Type
import platform.ohos.OH_LOG_Print
import platform.posix.int32_t

/**
 * Created by kamlin on 2024/4/20.
 */

@OptIn(ExperimentalForeignApi::class)
fun Any?.toKRRenderCValue(memScope: MemScope, renderCValue: KRRenderCValue): KRRenderCValue {
    when (this) {
        is Int -> {
            renderCValue.type = Type.INT
            renderCValue.value.intValue = this
        }
        is Long -> {
            renderCValue.type = Type.LONG
            renderCValue.value.longValue = this
        }
        is Float -> {
            renderCValue.type = Type.FLOAT
            renderCValue.value.floatValue = this
        }
        is Double -> {
            renderCValue.type = Type.DOUBLE
            renderCValue.value.doubleValue = this
        }
        is Boolean -> {
            renderCValue.type = Type.BOOL
            renderCValue.value.boolValue = if (this) 1 else 0
        }
        is String -> {
            with(memScope) {
                renderCValue.type = Type.STRING
                renderCValue.value.stringValue = this@toKRRenderCValue.cstr.ptr
            }
        }
        is ByteArray -> {
            val bytes = this
            renderCValue.type = Type.BYTES
            renderCValue.size = bytes.size
            if (renderCValue.size > 0) {
                renderCValue.value.bytesValue = this.usePinned { it.addressOf(0) }
            }
        }
        is Array<*> -> {
            renderCValue.type = Type.ARRAY
            renderCValue.size = this.size
            val cArray = memScope.allocArray<KRRenderCValue>(size)
            for (i in 0 until size) {
                this@toKRRenderCValue[i]?.toKRRenderCValue(memScope, cArray[i])
            }
            renderCValue.value.arrayValue = cArray
        }
        else -> {
            renderCValue.type = Type.NULL
            renderCValue.value.intValue = 0
        }
    }
    return renderCValue
}

@OptIn(ExperimentalForeignApi::class)
fun Any?.toKRRenderCValue(memScope: MemScope): CValue<KRRenderCValue> {
    return cValue<KRRenderCValue> {
        this@toKRRenderCValue.toKRRenderCValue(memScope, this)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun KRRenderCValue.toAny(): Any? {
    return when (type) {
        Type.INT -> value.intValue
        Type.BOOL -> value.boolValue == 1
        Type.LONG -> value.longValue
        Type.FLOAT -> value.floatValue
        Type.DOUBLE -> value.doubleValue
        Type.STRING -> value.stringValue?.toKString()
        Type.BYTES -> toByteArray()
        Type.ARRAY -> value.arrayValue?.arrayToAny(size)
        else -> null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<KRRenderCValue>.arrayToAny(size: Int): Any {
    val list = mutableListOf<Any?>()
    for (i in 0 until size) {
        val value = this[i].toAny()
        list.add(value)
    }
    return list.toTypedArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun KRRenderCValue.toByteArray(): Any {
    val size = size
    val byteArray = ByteArray(size)
    for (index in 0 until size) {
        byteArray[index] = value.bytesValue!![index]
    }
    return byteArray
}

@OptIn(ExperimentalForeignApi::class)
fun CValue<KRRenderCValue>.toAny(): Any? {
    return useContents {
        toAny()
    }
}

@OptIn(ExperimentalForeignApi::class)
fun CValue<KRRenderCValue>.asString(): String {
    return useContents { asString() }
}

@OptIn(ExperimentalForeignApi::class)
fun KRRenderCValue.asString(): String {
    return value.stringValue?.toKString() ?: ""
}