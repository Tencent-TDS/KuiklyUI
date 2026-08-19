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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.android.demo.module

import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject

/**
 * 对应鸿蒙侧 KRMyModule / iOS 侧 KRMyModule，用于演示 Kuikly Module 的同步与异步调用。
 * 方法名需与 Kotlin 侧 MyModule 调用及鸿蒙/iOS 侧实现保持一致。
 */
class KRMyModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            // 同步调用(JSON传输)：接收JSON字符串，返回data字段值
            "syncJsonCall" -> {
                val json = JSONObject(params as? String ?: "{}")
                json.optString("data")
            }
            // 同步调用(ByteArray传输)：接收含ByteArray的数组，交换字节后返回
            "syncByteArrayCall" -> {
                val array = params as? Array<*>
                val bytes = (array?.firstOrNull() as? ByteArray) ?: byteArrayOf()
                if (bytes.size >= 4) {
                    val tmp1 = bytes[0]
                    val tmp2 = bytes[1]
                    bytes[0] = bytes[3]
                    bytes[1] = bytes[2]
                    bytes[2] = tmp2
                    bytes[3] = tmp1
                }
                bytes
            }
            // 异步回调(JSON传输)：接收JSON字符串，通过callback回传 **JSON字符串**（与鸿蒙一致）。
            // 框架 Module.toNative 的 CallbackFn 路径在 res is String 时会用框架 JSONObject 解析，
            // 才能被 Kotlin CallbackFn(JSONObject?) 正确接收；传 Map 或 org.json.JSONObject 都会因
            // 类型判断失败导致 dataJSONObject=null。
            "asyncJsonCallback" -> {
                val json = JSONObject(params as? String ?: "{}")
                val content = json.optString("data")
                callback?.invoke("{\"content\":\"$content\"}")
                null
            }
            // 异步回调(数组传输)：接收字符串数组，通过callback回传第一个元素
            "asyncArrayCallback" -> {
                val array = params as? Array<*>
                callback?.invoke(array?.firstOrNull()?.toString())
                null
            }
            // 异步回调(混合数组)：无业务参数，通过callback回传 [ByteArray, String, String]
            "asyncMixedArrayCallback" -> {
                callback?.invoke(arrayOf(byteArrayOf(33, 34, 35), "s1", "hello"))
                null
            }
            // 同步调用(返回Double 3.14)
            "retDouble" -> {
                3.14
            }
            // 同步调用(返回Int 314)
            "retInt" -> {
                314
            }
            else -> callback?.invoke(mapOf(
                "code" to -1,
                "message" to "方法不存在"
            ))
        }
    }

    companion object {
        const val MODULE_NAME = "KRMyModule"
    }
}