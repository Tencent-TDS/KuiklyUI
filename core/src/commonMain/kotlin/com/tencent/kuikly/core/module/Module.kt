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

import com.tencent.kuikly.core.base.toInt
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.global.GlobalFunctionRef
import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.nvi.serialization.serialization
import com.tencent.kuikly.core.pager.PageData

/*
 * @brief 扩展module的统一基类，所有module都需要继承该类
 */
abstract class Module {
    var pagerId: String = ""
    var pageData: PageData? = null

    abstract fun moduleName(): String
    class ReturnValue(val callbackRef: CallbackRef?, val returnValue: Any? = null, errorCallbackRef: CallbackRef? = null) {
        override fun toString(): String {
            if (returnValue is String) {
                return returnValue as String
            } else if (returnValue != null) {
                return returnValue.toString()
            }
            return ""
        }
    }

    internal fun injectVar(pagerId: String, pageData: PageData) {
        this.pagerId = pagerId
        this.pageData = pageData
    }

    /*
    * @brief 同步调用Native方法（推荐的便利方法）(端在子线程调用到该方法)
    * @param methodName 方法名
    * @param data 方法数据
    * @param callbackFn 方法回调闭包(回参类型为JSONObject)
    * @return 返回值(值类型为String)
    */
    fun syncToNativeMethod(
        methodName: String,
        data: JSONObject?,
        callbackFn: CallbackFn?
    ): String {
        return toNative(
            false,
            methodName,
            data?.toString(),
            callbackFn,
            true
        ).toString()
    }
    /*
     * @brief 同步调用Native方法（原子方法，可用于二进制数据(ByteArray)通信）(端在子线程调用到该方法)
     * @param methodName 方法名
     * @param args 方法对应的参数列表(参数类型仅支持String，Int，Float，ByteArray类型)
     * @param callbackFn 方法回调闭包(回参类型为Any? , 类型可为Array，String，ByteArray等基础数据类型)
     * @return 返回值 (值类型可为String，Int，Float，ByteArray, Array等基础数据类型)
     */
    fun syncToNativeMethod(
        methodName: String,
        args: Array<Any>, // 参数列表(参数类型仅支持String，Int，Float，ByteArray类型)
        callbackFn: AnyCallbackFn?
    ): Any? {
        // 转成平台数据结构
        val argsValue = fastArrayListOf<Any>()
        args.forEach {
            if (it is String || it is Int || it is Float || it is ByteArray) {
                argsValue.add(it.toPlatformObject())
            } else {
                throwRuntimeError("syncToNativeMethod args参数类型仅支持String，Int，Float，ByteArray类型, ele:${it.toString()}")
            }
        }
        return innerToNative(
            false,
            methodName,
            argsValue.toPlatformObject(),
            callbackFn,
            true
        ).returnValue?.toKotlinObject()
    }
    /*
    * @brief 异步调用Native方法（原子方法，可用于二进制数据(ByteArray)通信）(端在主线程调用到该方法)
    * @param methodName 方法名
    * @param data JsonObject数据结构，端到收到转成jsonString
    * @param callbackFn 方法回调闭包(回参类型为JSONObject？, 适用于回参是json的场景)
    */
    fun asyncToNativeMethod(methodName: String, data: JSONObject?, callbackFn: CallbackFn?) {
        toNative(
            false,
            methodName,
            data?.toString(),
            callbackFn,
            false
        )
    }

    /*
     * @brief 异步调用Native方法（原子方法，可用于二进制数据(ByteArray)通信）(端在子线程调用到该方法)
     * @param methodName 方法名
     * @param args 方法对应的参数列表(参数类型仅支持String，Int，Float，ByteArray类型)
     * @param callbackFn 方法回调闭包(回参类型为Any? , 类型可为String，Array，Float，Int，ByteArray基础数据类型)
     */
    fun asyncToNativeMethod(
        methodName: String,
        args: Array<Any>, // 参数列表(参数类型仅支持String，Int，Float，ByteArray类型)
        callbackFn: AnyCallbackFn?
    ) {
        // 转成平台数据结构
        val argsValue = fastArrayListOf<Any>()
        args.forEach {
            argsValue.add(it.toPlatformObject())
        }
        innerToNative(
            false,
            methodName,
            argsValue.toPlatformObject(),
            callbackFn,
            false
        )
    }

    fun toTDFNative(
        keepCallbackAlive: Boolean = false,
        methodName: String,
        params: List<Any?>?,
        successCallback: TDFModuleCallbackFn? = null,
        errorCallback: TDFModuleCallbackFn? = null,
        syncCall: Boolean = false
        ) : ReturnValue {

        var paramStr: String? = null
        params?.let {
            paramStr = it.serialization().toString()
        }
        var succCallbackRef: CallbackRef? = null
        successCallback?.also { callback ->
            succCallbackRef = GlobalFunctions.createFunction(pagerId) { data ->
                callback(parseTDFResult(data?.toKotlinObject()))
                keepCallbackAlive
            }
        }
        var errorCallbackRef: CallbackRef? = null
        errorCallback?.also { callback ->
            errorCallbackRef = GlobalFunctions.createFunction(pagerId) { data ->
                callback(parseTDFResult(data?.toKotlinObject()))
                keepCallbackAlive
            }
        }
        val returnValue = BridgeManager.callTDFModuleMethod(
            pagerId,
            moduleName(),
            methodName,
            paramStr,
            succCallbackRef,
            errorCallbackRef,
            syncCall.toInt()
        )
        return ReturnValue(succCallbackRef, parseTDFResult(returnValue), errorCallbackRef)
    }

    private fun parseTDFResult(data: Any?): Any? {
        if (data == null) {
            return null
        }
        var jsonObj: JSONObject? = null
        if (data is JSONObject) {
            jsonObj = data
        } else if (data is String) {
            jsonObj = JSONObject(data)
        }
        jsonObj?.let {
            when (val result = jsonObj.opt(TDF_METHOD_RESULT_KEY)) {
                is JSONArray -> {
                    return result.toList()
                }

                is JSONObject -> {
                    return result.toMap()
                }

                else -> {
                    return result
                }
            }
        }
        return null
    }

    /**
     * 通用的与Native Module通信方法
     */
    fun toNative(
        keepCallbackAlive: Boolean = false,
        methodName: String,
        param: Any?,
        callback: CallbackFn? = null,
        syncCall: Boolean = false
    ): ReturnValue {
        var nativeCallback : AnyCallbackFn? = null
        callback?.also {
            nativeCallback = { res ->
                var dataJSONObject : JSONObject? = null
                if (res != null && res is String) {
                    dataJSONObject = JSONObject(res)
                } else if (res != null && res is JSONObject) {
                    dataJSONObject = res
                }
                callback(dataJSONObject)
            }
        }
        return innerToNative(keepCallbackAlive, methodName, param, nativeCallback, syncCall)
    }

    private fun innerToNative(
        keepCallbackAlive: Boolean = false,
        methodName: String,
        param: Any?,
        callback: AnyCallbackFn? = null,
        syncCall: Boolean = false
    ): ReturnValue {
        var callbackRef: CallbackRef? = null
        callback?.also { cb ->
            callbackRef = GlobalFunctions.createFunction(pagerId) { res ->
                cb(res?.toKotlinObject())
                keepCallbackAlive
            }
        }
        val returnValue = BridgeManager.callModuleMethod(
            pagerId,
            moduleName(),
            methodName,
            param,
            callbackRef,
            convertSyncCall(syncCall, keepCallbackAlive)
        )
        return ReturnValue(callbackRef, returnValue)
    }

    fun removeCallback(callbackRef: CallbackRef) {
        GlobalFunctions.destroyGlobalFunction(pagerId, callbackRef)
    }

    private fun convertSyncCall(syncCall: Boolean, keepCallbackAlive: Boolean): Int {
        return if (pageData?.isOhOs == true) {
            // 鸿蒙平台 c++ to arkts方法需要感知keepAlive参数
            // 由于callModule参数已经达到6个，无法直接传递, 这里跟SyncCall复用同个字段, 鸿蒙c++
            // 判断 > 1时，会 - 2 或者 & CALLBACK_KEEP_ALIVE_MASK 来判断callback是否keepAlive
            return syncCall.toInt() + if (keepCallbackAlive) CALLBACK_KEEP_ALIVE_MASK else 0
        } else {
            syncCall.toInt()
        }
    }

    companion object {
        private const val TDF_METHOD_RESULT_KEY = "result"
        private const val CALLBACK_KEEP_ALIVE_MASK = 2
    }
}
// 平台侧回参为JSONObject场景使用
typealias CallbackFn = (data: JSONObject?) -> Unit
// 平台侧回参为非JSONObject场景使用
typealias AnyCallbackFn = (data: Any?) -> Unit
typealias CallbackRef = GlobalFunctionRef

typealias TDFModuleCallbackFn = (data: Any?) -> Unit

expect fun Any.toPlatformObject() : Any
expect fun Any.toKotlinObject() : Any
