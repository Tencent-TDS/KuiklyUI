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

package com.tencent.kuikly.core.reflection

import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.module.ReflectionModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

open class OCObject(objectID: String) : NativeObject<OCObject>(objectID) {

    override fun emptyConstObject(): OCObject {
        return EMPTY_OBJECT
    }

    override fun createNativeObject(objectID: String): OCObject {
        if (objectID.isEmpty()) {
            return EMPTY_OBJECT
        }
        return OCObject(objectID)
    }

    override fun encodeArg(arg: Any?): String {
        return when (arg) {
            is OCBlock0 -> encodeBlockArgToString(arg, BLOCK0)
            is OCBlock1 -> encodeBlockArgToString(arg, BLOCK1)
            is OCBlock2 -> encodeBlockArgToString(arg, BLOCK2)
            is OCBlock3 -> encodeBlockArgToString(arg, BLOCK3)
            is OCBlock4 -> encodeBlockArgToString(arg, BLOCK4)
            is OCBlock5 -> encodeBlockArgToString(arg, BLOCK5)
            else -> super.encodeArg(arg)
        }
    }

    protected fun encodeBlockArgToString(arg: Any, type: String): String {
        val callbackRef =
            GlobalFunctions.createFunction(ReflectionModule.instance().pagerId) { dataStr ->
                val res = JSONObject(if (dataStr is String) dataStr else "{}")
                val arg1 =
                    if (res.has("arg1")) createNativeObject(res.optString("arg1")) else null
                val arg2 =
                    if (res.has("arg2")) createNativeObject(res.optString("arg2")) else null
                val arg3 =
                    if (res.has("arg3")) createNativeObject(res.optString("arg3")) else null
                val arg4 =
                    if (res.has("arg4")) createNativeObject(res.optString("arg4")) else null
                val arg5 =
                    if (res.has("arg5")) createNativeObject(res.optString("arg5")) else null
                when (arg) {
                    is OCBlock0 -> (arg as OCBlock0).block.invoke()
                    is OCBlock1 -> (arg as OCBlock1).block.invoke(arg1)
                    is OCBlock2 -> (arg as OCBlock2).block.invoke(arg1, arg2)
                    is OCBlock3 -> (arg as OCBlock3).block.invoke(arg1, arg2, arg3)
                    is OCBlock4 -> (arg as OCBlock4).block.invoke(arg1, arg2, arg3, arg4)
                    is OCBlock5 -> (arg as OCBlock5).block.invoke(arg1, arg2, arg3, arg4, arg5)
                }
                false
            }
        return encodeArgToString(type, callbackRef)
    }

    companion object {
        val EMPTY_OBJECT = OCObject("")
        const val BLOCK0 = "block0"
        const val BLOCK1 = "block1"
        const val BLOCK2 = "block2"
        const val BLOCK3 = "block3"
        const val BLOCK4 = "block4"
        const val BLOCK5 = "block5"
    }
}