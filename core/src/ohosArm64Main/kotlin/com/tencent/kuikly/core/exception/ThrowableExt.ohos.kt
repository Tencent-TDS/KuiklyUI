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

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKString
import kotlinx.cinterop.toLong
import ohos.fill_dl_info_sym
import ohos.xdl_t
import platform.ohos.LOG_APP
import platform.ohos.LOG_ERROR
import platform.ohos.LOG_INFO
import platform.ohos.LOG_WARN
import platform.ohos.LogLevel
import platform.ohos.OH_LOG_Print
import platform.posix.Dl_info
import platform.posix.dladdr
import platform.posix.memset
import kotlin.experimental.ExperimentalNativeApi

private const val TAG = "KotlinUncaughtException"
private const val UUID_UNKNOWN = "UUID_UNKNOWN"

internal typealias LogProxy = (LogLevel, String, String) -> Unit

internal var logProxy: LogProxy = { logLevel, tag, msg ->
    OH_LOG_Print(
        LOG_APP,
        logLevel,
        1u,
        tag,
        msg
    )
}

fun setLogProxy(logImpl: LogProxy) {
    logProxy = logImpl
}

/**
 * 打印异常堆栈
 */
actual fun Throwable.printStacks() {
    printStacksInner(this, "KotlinNativeStackTrace", LOG_WARN)
}

private fun printUnhandledException(t: Throwable) {
    printStacksInner(t, TAG, LOG_ERROR)
}

private fun printStacksInner(t: Throwable, tag: String, logLevel: LogLevel) {
    val logs = t.stacksToString().split("\n")
    logs.forEach {
        logProxy(logLevel, tag, it)
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private fun buildStackString(
    t: Throwable,
    fillUUID: Boolean = false,
    alignSymbol: Boolean = true
): String {
    val stackAbsAddrArray = t.getStackTraceAddresses()
    val stackAddrArr = parseSymbolInfo(stackAbsAddrArray)
    val maxSoNameLength = if (stackAddrArr.isNotEmpty()) stackAddrArr.maxOf { it.soName.length } else 0

    val result = StringBuilder()
    result.append("exception: ").append(t::class.qualifiedName)
    result.append("\nmessage: ").append(t.message)

    val addr2LineStr = StringBuilder()
    var lastSoName = ""

    for (i in stackAddrArr.indices) {
        result.append("\n")
        val symbolInfo = stackAddrArr[i]
        result.append("#${i.toString().padStart(2, '0')}")
        result.append("    pc ")

        val offsetStr = symbolInfo.offsetFromSoBase.toHexStringWithLeadingZeros(16)
        result.append(offsetStr)
        result.append("    ")

        result.append(symbolInfo.soName)
        if (alignSymbol) {
            for (j in 0 until maxSoNameLength + 5 - symbolInfo.soName.length) {
                result.append(" ")
            }
        }

        result.append(" (${symbolInfo.name}+${symbolInfo.offsetFromSymbol})")

        if (fillUUID) {
            // Bugly 采用的是后 16字节 UUID
            result.append(" [arm64-v8a::${KuiklyUUIDCache.getUUID(stackAbsAddrArray[i])?.substring(8) ?: UUID_UNKNOWN}]")
        }

        if (lastSoName != symbolInfo.soName) {
            addr2LineStr.append(" -e ")
            if (symbolInfo.soName.startsWith("/")) {
                addr2LineStr.append(symbolInfo.soName.substring(symbolInfo.soName.lastIndexOf("/") + 1))
            } else {
                addr2LineStr.append(symbolInfo.soName)
            }
            lastSoName = symbolInfo.soName
        }
        addr2LineStr.append(" ").append(symbolInfo.offsetFromSoBase.toString(16))
    }

    result.append("\naddr2line -p -f ").append(addr2LineStr)

    t.cause?.let {
        result.append("\nCaused by: \n")
        result.append(buildStackString(it, fillUUID, alignSymbol))
    }
    return result.toString()
}

private class SymbolInfo {
    var offsetFromSoBase = 0L
    var offsetFromSymbol = 0L
    var name = ""
    var soName = ""
}

@OptIn(ExperimentalForeignApi::class)
private fun parseSymbolInfo(addrList: List<Long>): List<SymbolInfo> {
    val symbolInfoList = mutableListOf<SymbolInfo>()

    memScoped {
        var xdlInfo = alloc<xdl_t> { memset(this.ptr, 0, sizeOf<xdl_t>().toULong()) }
        for (addr in addrList) {
            val result = alloc<Dl_info> { memset(this.ptr, 0, sizeOf<Dl_info>().toULong()) }
            dladdr(addr.toCPointer<LongVar>(), result.ptr)

            if (xdlInfo.so_name == null) {
                xdlInfo.so_name = result.dli_fname
            } else if ((xdlInfo.so_name as CPointer<ByteVar>).toKString() != result.dli_fname?.toKString()) {
                xdlInfo = alloc<xdl_t> { memset(this.ptr, 0, sizeOf<xdl_t>().toULong()) }
            }

            val symbolInfo = SymbolInfo()
            symbolInfo.offsetFromSoBase = addr - result.dli_fbase.toLong() - 1
            symbolInfo.soName = result.dli_fname?.toKString() ?: ""

            fill_dl_info_sym(xdlInfo.ptr, result.ptr, symbolInfo.offsetFromSoBase)
            symbolInfo.name = result.dli_sname?.toKString() ?: ""
            symbolInfo.offsetFromSymbol = result.dli_saddr.toLong()
            symbolInfoList.add(symbolInfo)
        }
    }
    return symbolInfoList
}

@OptIn(ExperimentalNativeApi::class)
fun kuiklyRegisterDefaultUnhandledExceptionHook() {
    logProxy(LOG_INFO, TAG, "register UnhandledExceptionHook")
    var old: ReportUnhandledExceptionHook? = null
    old = setUnhandledExceptionHook {
        logProxy(LOG_INFO, TAG, "handle UnhandledException begin.")
        printUnhandledException(it)
        old?.invoke(it)
        logProxy(LOG_INFO, TAG, "handle UnhandledException end.")
        throw it
    }
}

/**
 * 获取堆栈字符串
 */
actual fun Throwable.stacksToString(): String {
    return "UncaughtException: \n${buildStackString(this)}"
}

private fun Long.toHexStringWithLeadingZeros(totalLength: Int): String {
    val hexString = this.toString(16)
    val zerosNeeded = totalLength - hexString.length
    val leadingZeros = "0".repeat(zerosNeeded.coerceAtLeast(0))
    return leadingZeros + hexString
}

/**
 * 获取堆栈 for Bugly 上报
 */
actual fun Throwable.getStackTraceForBuglyReport(): String {
    return "UncaughtException: \n${buildStackString(this, fillUUID = true, alignSymbol = false)}"
}
