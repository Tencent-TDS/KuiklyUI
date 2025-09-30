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

package com.tencent.kuikly.compose.ui

import com.tencent.kuikly.compose.ui.graphics.ImageBitmap
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.ImageCacheStatus
import com.tencent.kuikly.core.module.MemoryCacheModule
import com.tencent.kuikly.core.pager.Pager

private const val DEBUG_LOG = false
private inline fun logInfo(msg: () -> String) {
    if (DEBUG_LOG) {
        KLog.i("CacheMgr", msg())
    }
}

internal class KuiklyImageCacheManager(pager: Pager) {
    private val cacheMap = mutableMapOf<String, KuiklyImageBitmap>()
    private val referenceMap = mutableMapOf<String, Int>()
    private val memoryCacheModule = pager.acquireModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME)

    fun loadImage(src: String): ImageBitmap {
        val imageBitmap = cacheMap.getOrPut(src) {
            KuiklyImageBitmap(src) {
                val refCount = (referenceMap[src] ?: 1) - 1
                logInfo { "forgot $src refCount=$refCount" }
                if (refCount <= 0) {
                    referenceMap.remove(src)
                    cacheMap.remove(src)
                    clearCache(this.status)
                } else {
                    referenceMap[src] = refCount
                }
            }
        }
        referenceMap[src] = (referenceMap[src] ?: 0) + 1
        if (imageBitmap.status.errorCode != 0) {
            logInfo { "start cache $src" }
            imageBitmap.status = memoryCacheModule.cacheImage(src, false) {
                logInfo { "end cache code=${it.errorCode} state=${it.state} $src" }
                imageBitmap.status = it
                if (src !in cacheMap) {
                    clearCache(it)
                }
            }
        } else {
            logInfo { "use cache code=${imageBitmap.status.errorCode} state=${imageBitmap.status.state} $src" }
        }
        return imageBitmap
    }

    private fun clearCache(status: ImageCacheStatus) {
        val cacheKey = status.cacheKey.ifEmpty { return }
        memoryCacheModule.setObject(cacheKey, "") // clear the cached image object
    }
}