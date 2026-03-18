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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKString
import ohos.kuiklyGetBuildIDByName
import platform.posix.Dl_info
import platform.posix.dladdr
import platform.posix.free
import platform.posix.memset

@OptIn(ExperimentalForeignApi::class)
internal object KuiklyUUIDCache {
    private val soToUUID = HashMap<String, String?>()

    /**
     * 根据内存地址获取所在 so 的 UUID
     */
    fun getUUID(address: Long): String? {
        var result: String? = null
        memScoped {
            val dlInfo = alloc<Dl_info> { memset(this.ptr, 0, sizeOf<Dl_info>().toULong()) }
            dladdr(address.toCPointer<LongVar>(), dlInfo.ptr)
            val soName = dlInfo.dli_fname?.toKString() ?: return result
            val cache = soToUUID[soName]
            if (!cache.isNullOrEmpty()) {
                result = cache
            } else {
                val resultCStr = kuiklyGetBuildIDByName(soName)
                val resultFromSymbol = resultCStr?.toKString()
                soToUUID[soName] = resultFromSymbol
                result = resultFromSymbol
                free(resultCStr)
            }
        }
        return result
    }
}
