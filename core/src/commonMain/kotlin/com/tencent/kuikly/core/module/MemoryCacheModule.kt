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

package com.tencent.kuikly.core.module

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

class ImageRef(val cacheKey: String)

class ImageCacheStatus(
    val state: String = InProgress,
    val errorCode: Int = 0,
    val errorMsg: String = "",
    val cacheKey: String = "",
    val width: Int = 0,
    val height: Int = 0
) {
    companion object {
        const val InProgress = "InProgress"
        const val Complete   = "Complete"
    }
}

typealias ImageCacheCallback = (status: ImageCacheStatus) -> Unit

class MemoryCacheModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    companion object {
        const val MODULE_NAME = ModuleConst.MEMORY
        const val METHOD_SET_OBJECT = "setObject"
        const val METHOD_CACHE_IMAGE = "cacheImage"
    }

    fun setObject(key: String, value: Any) {
        val params = JSONObject()
        params.put("key", key)
        params.put("value", value)
        toNative(
            false,
            METHOD_SET_OBJECT,
            params.toString()
        )
    }

    fun cacheImage(src: String, sync: Boolean, callback: ImageCacheCallback):ImageCacheStatus {
        return cacheImage(src, null, sync, callback)
    }

    fun cacheImage(src: String, imageParams: JSONObject?, sync: Boolean, callback: ImageCacheCallback):ImageCacheStatus {
        val params = JSONObject()
        params.put("src", src)
        params.put("sync", if(sync) 1 else 0)
        if (imageParams != null) {
            params.put("imageParams", imageParams)
        }

        val retStr = toNative(
            false,
            MemoryCacheModule.METHOD_CACHE_IMAGE,
            params.toString(),
            callback = { res ->
                res?.also {
                    val status = ImageCacheStatus(
                        errorCode = it.optInt("errorCode",-1),
                        errorMsg = it.optString("errorMsg", ""),
                        state = ImageCacheStatus.Complete,
                        cacheKey = it.optString("cacheKey", ""),
                        width = it.optInt("width", 0),
                        height = it.optInt("height", 0)
                    )
                    callback(status)
                }
            },
            syncCall = true
        ).toString()
        try {
            val json = JSONObject(retStr)
            val status = ImageCacheStatus(
                errorCode = json.optInt("errorCode",-1),
                errorMsg = json.optString("errorMsg", ""),
                state = json.optString("state", ImageCacheStatus.Complete),
                cacheKey = json.optString("cacheKey", ""),
                width = json.optInt("width", 0),
                height = json.optInt("height", 0)
            )
            return status
        } catch (e : Throwable) {
            val status = ImageCacheStatus(
                state = ImageCacheStatus.Complete,
                errorCode = -1,
                errorMsg = "Error parsing result:$e"
            )
            return status
        }
    }
}