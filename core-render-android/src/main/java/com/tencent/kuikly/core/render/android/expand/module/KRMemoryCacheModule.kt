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

package com.tencent.kuikly.core.render.android.expand.module

import android.widget.ImageView
import com.tencent.kuikly.core.render.android.adapter.HRImageLoadOption
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.css.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.android.expand.component.image.FetchImageCallback
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 内存缓存模块
 */
class KRMemoryCacheModule : KuiklyRenderBaseModule() {

    private val cacheMap = ConcurrentHashMap<String, Any>()

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_SET_OBJECT -> setObject(params)
            METHOD_CACHE_IMAGE -> cacheImage(params, callback)
            else -> super.call(method, params, callback)
        }
    }

    /**
     * 根据key获取值
     * @param T key关联的值的类型
     * @param key
     * @return key关联的值
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? = cacheMap[key] as? T

    /**
     * 关联key与value
     * @param key
     * @param value
     */
    fun set(key: String, value: Any) {
        cacheMap[key] = value
    }

    private fun setObject(params: String?) {
        val json = params.toJSONObjectSafely()
        val value = json[KEY_VALUE] ?: return
        val key = json.optString(KEY_KEY)
        set(key, value)
    }

    private fun cacheImage(params: String?, callback: KuiklyRenderCallback?): JSONObject {
        val json = params.toJSONObjectSafely()
        val src = json.optString("src", "")
        val cacheKey = generateCacheKey(src)
        val result = JSONObject()
        if (cacheMap.containsKey(cacheKey)) {
            KuiklyRenderLog.d("KRMemory", "cacheImage key exist $cacheKey")
            result.put("state", "Complete")
            result.put("errorCode", 0)
            result.put("cacheKey", cacheKey)
            callback?.invoke(result)
            return result
        }
        val imageLoader = kuiklyRenderContext?.getImageLoader()
        //                 status.errorCode = it.optInt("errorCode",0)
        //                status.errorMsg = it.optString("errorMsg", "")
        //                status.state = it.optString("state", ImageCacheStatus.Complete)
        //                status.cacheKey = it.optString("cacheKey", "")
        if (imageLoader != null && KuiklyRenderAdapterManager.krImageAdapter != null) {
            val sync = json.optInt("sync") == 1
            val option = HRImageLoadOption(src, -1, -1, false, ImageView.ScaleType.FIT_XY)
            val notify = CountDownLatch(1)
            imageLoader.fetchImageAsync(option, generateCacheImageCallback(this, cacheKey, notify, callback))
            if (sync) {
                // wait
                val flag = notify.await(5, TimeUnit.SECONDS)
                result.put("state", "Complete")
                if (flag) {
                    if (cacheMap.containsKey(cacheKey)) {
                        result.put("errorCode", 0)
                        result.put("cacheKey", cacheKey)
                    } else {
                        result.put("errorCode", -1)
                        result.put("errorMsg", "fetch failed")
                    }
                } else {
                    result.put("errorCode", -1)
                    result.put("errorMsg", "fetch timeout")
                }
            } else {
                result.put("state", "Complete")
                result.put("errorCode", -1)
                result.put("errorMsg", "async call, should get result from callback")
            }
            return result
        } else {
            result.put("state", "Complete")
            result.put("errorCode", -1)
            result.put("errorMsg", "krImageAdapter is required")
            callback?.invoke(result)
            return result
        }
    }

    companion object {
        const val MODULE_NAME = "KRMemoryCacheModule"

        private const val METHOD_SET_OBJECT = "setObject"
        private const val METHOD_CACHE_IMAGE = "cacheImage"
        private const val KEY_VALUE = "value"
        private const val KEY_KEY = "key"
        private const val CACHE_KEY_PREFIX = "data:image_Md5_"

        private fun generateCacheKey(src: String): String {
            return CACHE_KEY_PREFIX + if (src.length > 200) {
                src.hashCode().toString()
            } else {
                KRCodecModule.base64Encode(src)
            }
        }

        private fun generateCacheImageCallback(
            module: KRMemoryCacheModule,
            cacheKey: String,
            notify: CountDownLatch?,
            callback: KuiklyRenderCallback?
        ): FetchImageCallback {
            return { drawable ->
                val result = JSONObject()
                result.put("state", "Complete")
                if (drawable != null) {
                    module.cacheMap[cacheKey] = drawable
                    result.put("errorCode", 0)
                    result.put("cacheKey", cacheKey)
                } else {
                    result.put("errorCode", -1)
                    result.put("errorMsg", "fetch failed")
                }
                callback?.invoke(result)
                notify?.countDown()
            }
        }

    }
}
