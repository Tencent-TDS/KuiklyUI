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
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.tencent.kuikly.core.module

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.get
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

actual fun Any.toPlatformObject(): Any {
    if (this is ByteArray) {
        return this.toNSData()
    }
    if (this is Array<*>) {
        return this.toList()
    }
    return this
}

internal fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        if (pinned.get().isNotEmpty()) {
            NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
        } else
            NSData()
    }
}

internal fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val byteArray = ByteArray(length)
    val bytePtr = this.bytes
    if (bytePtr != null) {
        for (i in 0 until length) {
            byteArray[i] = bytePtr.reinterpret<ByteVar>()[i]
        }
    }
    return byteArray
}

actual fun Any.toKotlinObject(): Any {
    if (this is List<*>) {
        val kotlinList = mutableListOf<Any>()
        this.forEach {
            it?.also {
                kotlinList.add(it.toKotlinObject())
            }
        }
        return kotlinList.toTypedArray()
    }
    if (this is NSData) {
        return this.toByteArray()
    }
    return this
}