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

import com.tencent.kuikly.core.base.toInt
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.module.ReflectionModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/*
 * @brief Native对象类
 * 自动内存释放（下一帧统一释放）
 * 如果需要手动管理内存，则通过retain/release()方法
 */
abstract class NativeObject<T: NativeObject<T>>(val objectID: String) {

    abstract fun createNativeObject(objectID: String) : T
    abstract fun emptyConstObject() : T

    /*
   * 调用方法
   * @param method 方法名 （iOS对应selector）
   * @param args 参数列表，支持NativeObject类，Boolean，Int，Float，Double，String，JSONObject, OCBlock类型参数,详情见encodeArg方法
   * @return 返回native对象
   */
    fun call(
        method: String,
        arg0: Any? = null,
        arg1: Any? = null,
        arg2: Any? = null,
        arg3: Any? = null,
        arg4: Any? = null,
        arg5: Any? = null
    ): T {
        if (objectID.isEmpty()) {
            return emptyConstObject()
        }
        val argsString = encodeArg(arg0) + encodeArg(arg1) + encodeArg(arg2) +
                encodeArg(arg3) + encodeArg(arg4) + encodeArg(arg5)
        val objectId = ReflectionModule.instance().call(this, method, argsString)
        if (objectId.isEmpty()) {
            return emptyConstObject()
        }
        return createNativeObject(objectId)
    }

    /*
     * native对象提取数据到kotlin
     */
    override fun toString(): String {
        if (objectID.isEmpty()) return ""
        return ReflectionModule.instance().toString(this)
    }

    /*
     *  调用该方法，说明要去手动管理该native对象内存(默认自动内存释放（下一帧统一释放）)
     *  这里采用引用计数释放，retain() 计数+1， release() 计数-1，当计数等用0，原生则释放
     */
    open fun retain() {
        if (this === emptyConstObject()) {
            return
        }
        ReflectionModule.instance().retain(this)
    }

    open fun release() {
        if (this === emptyConstObject()) {
            return
        }
        ReflectionModule.instance().release(this)
    }

    open fun encodeArg(arg: Any?): String {
        if (arg == null) {
            return ""
        }
        return when (arg) {
            is NativeObject<*> -> encodeArgToString(NATIVE_OBJECT, (arg as NativeObject<*>).objectID)
            is Boolean -> encodeArgToString(BOOLEAN, (arg as Boolean).toInt().toString())
            is Int -> encodeArgToString(INT_, (arg as Int).toString())
            is UInt -> encodeArgToString(U_INT, (arg as UInt).toString())
            is Short -> encodeArgToString(SHORT, (arg as Short).toString())
            is Float -> encodeArgToString(FLOAT, (arg as Float).toString())
            is Double -> encodeArgToString(DOUBLE, (arg as Double).toString())
            is String -> encodeArgToString(STRING, (arg as String))
            is JSONObject -> encodeArgToString(JSON_OBJECT, (arg as JSONObject).toString())
            is JSONArray -> encodeArgToString(JSON_ARRAY, (arg as JSONArray).toString())
            else -> {
                throwRuntimeError("unsupported arg type:$arg")
                return ""
            }
        }
    }

    protected fun encodeArgToString(type: String, value: String): String {
        return "$SPLIT_TAG${type.length}$type$value"
    }

    companion object {
        const val MODULE_NAME = "KRReflection"
        const val SPLIT_TAG = "\n$\t&@\n"
        const val NATIVE_OBJECT = "object"
        const val BOOLEAN = "boolean"
        const val INT_ = "int"
        const val U_INT = "uint"
        const val SHORT = "short"
        const val FLOAT = "float"
        const val DOUBLE = "double"
        const val STRING = "string"
        const val JSON_OBJECT = "jsonObject"
        const val JSON_ARRAY = "jsonArray"
        const val METHOD_RETAIN = "retain"
        const val METHOD_RELEASE = "release"
    }
}
