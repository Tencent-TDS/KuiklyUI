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
package com.tencent.kuikly.demo.pages.demo

import kotlin.native.runtime.Debugging
import kotlin.native.runtime.NativeRuntimeApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.O_CREAT
import platform.posix.O_TRUNC
import platform.posix.O_WRONLY
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.close
import platform.posix.open
import platform.posix.mkdir
import platform.posix.strerror
import platform.posix.errno

@OptIn(NativeRuntimeApi::class, ExperimentalForeignApi::class)
actual fun dumpMemory() {
    val filePath = "/data/storage/el2/base/haps/entry/temp/memory.bin"

    // 创建目录（如果不存在），忽略错误（目录可能已存在）
    val dirPath = "/data/storage/el2/base/haps/entry/temp"
    mkdir(dirPath, (S_IRUSR or S_IWUSR).toUInt())

    // 打开文件，如果不存在则创建，如果存在则截断
    val fd = open(filePath, O_CREAT or O_WRONLY or O_TRUNC, (S_IRUSR or S_IWUSR or 7).toUInt())

    if (fd < 0) {
        // 文件打开失败
        val errorMsg = strerror(errno)?.toKString() ?: "Unknown error"
        println("Failed to open file $filePath: $errorMsg (errno: $errno)")
        return
    }

    try {
        // 转储内存到文件
        println("dump memory begin")
        Debugging.dumpMemory(fd.toLong())
        println("dump memory done")
    } finally {
        // 关闭文件描述符
        close(fd)
    }
}