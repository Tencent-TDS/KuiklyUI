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

import com.tencent.kuikly.core.render.android.expand.vendor.KRReflect
import com.tencent.kuikly.core.render.android.expand.vendor.ReflectException
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreContextScheduler

class KRReflectionModule : KuiklyRenderBaseModule() {
    private val objectRegistry = hashMapOf<String, KRJavaObject>()
    private var autoObjectID = 0L
    private var needAutoReleaseNextLoop = false
    class KRJavaObject {
        var krRetainCount = 0
        lateinit var javaObject : KRReflect
        fun toJavaObject() : Any {
            if (javaObject.get<Any>() != null) {
                return javaObject.get<Any>()!!
            }
            return javaObject.clazz
        }
    }
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_INVOKE -> invoke(params)
            METHOD_RETAIN -> retain(params)
            METHOD_RELEASE -> release(params)
            METHOD_TO_STRING -> kToString(params)
            METHOD_GET_FIELD -> getField(params)
            METHOD_SET_FIELD -> setField(params)
            else -> super.call(method, params, callback)
        }
    }
    private fun kToString(params: String?) : String {
        val string = params ?: return ""
        val krObject = getObject(string, "toString") ?: return ""
        if (krObject is KRJavaObject) {
            return krObject.javaObject.callMethod("toString").get<String>() ?: ""
        } else if (krObject is KRReflect) {
            return krObject.callMethod("toString").get<String>() ?: ""
        }
        return ""
    }

    private fun getField(params: String?) : String {
        val string = params ?: return ""
        val objectMethodSplits =string.split("|")
        val objectID = objectMethodSplits[0] // 必须为2段，不需要下标判断
        val fieldName = objectMethodSplits[1] // 必须为2段，不需要下标判断
        val krObject = getObject(objectID, "getField") ?: return ""
        val kRReflect = if (krObject is KRJavaObject) krObject.javaObject else (krObject as KRReflect)
        try {
            val value = kRReflect.getFieldValue<Any>(fieldName)
            if (value != null) {
                return setObject(KRReflect.create(value))
            }
        } catch (e: Throwable) {
            throwReflectError("反射对象属性失败","属性名:$fieldName exception:$e")
        }
        return ""
    }

    private fun setField(params: String?) : String {
        val string = params ?: return ""
        val paramsSplits = string.split(SPLIT_TAG)
        val objectMethodSplits = paramsSplits[0].split("|")// 必须 >= 1段，不需要下标判断
        val objectID = objectMethodSplits[0] // 必须为2段，不需要下标判断
        val fieldName = objectMethodSplits[1] // 必须为2段，不需要下标判断
        val krObject = getObject(objectID, "getField") ?: return ""
        val argObjects = getArgs(paramsSplits, fieldName) ?: return ""
        val kRReflect = if (krObject is KRJavaObject) krObject.javaObject else (krObject as KRReflect)
        try {
            kRReflect.setField(fieldName, argObjects[0])
        } catch (e: Throwable) {
            throwReflectError("反射设置对象属性失败","属性名:$fieldName exception:$e")
        }
        return ""
    }

    private fun invoke(params: String?): String {
        val string = params ?: return ""
        val paramsSplits = string.split(SPLIT_TAG)
        val objectMethodSplits = paramsSplits[0].split("|")// 必须 >= 1段，不需要下标判断
        val objectID = objectMethodSplits[0] // 必须为2段，不需要下标判断
        val methodName = objectMethodSplits[1] // 必须为2段，不需要下标判断

        val krObject = getObject(objectID, methodName) ?: return ""
        val argObjects = getArgs(paramsSplits, methodName) ?: return ""
        var returnObject : KRReflect? = null
        if (krObject is KRJavaObject) {
            try {
                returnObject = (krObject as KRJavaObject).javaObject.callMethod(methodName, *argObjects)
            } catch (e: Throwable) {
                throwReflectError("反射对象方法失败","方法名:$methodName exception:$e")
            }
        } else {
            try {
                returnObject = (krObject as KRReflect).callMethod(methodName, *argObjects)
            } catch (e: Throwable) {
                throwReflectError("反射类方法失败","类名:$objectID 方法名:$methodName exception:$e")
            }

        }
        if (returnObject != null) {
             return setObject(returnObject)
        }
        return ""
    }

    private fun getObject(objectID: String, method: String) : Any? {
        val javaObject = objectRegistry[objectID]
            ?: return if (objectID.toLongOrNull() == null) { // 说明为类名
                try {
                    KRReflect.create(Class.forName(objectID))
                } catch (e: Exception) {
                    throwReflectError("反射类名失败", "类名:$objectID exception:$e method: $method")
                    null
                }
            } else {
                throwReflectError("反射获取对象失败", "获取对象ID:$objectID, 方法: $method , 对象可能被自动清理,可尝试retain()/release()手动管理内存")
                null
            }
        return javaObject
    }

    private fun setObject(krReflect: KRReflect) : String {
        val krJavaObject =  KRJavaObject()
        krJavaObject.javaObject = krReflect
        autoObjectID++
        val objectID = autoObjectID.toString()
        objectRegistry[objectID] = krJavaObject
        setNeedAutoReleaseInNextLoop()
        return objectID
    }

    private fun setNeedAutoReleaseInNextLoop() {
        if (!needAutoReleaseNextLoop) {
            needAutoReleaseNextLoop = true
            // next loop
            KuiklyRenderCoreContextScheduler.scheduleTask(16) {
                objectRegistry.toMap().forEach {
                    if (it.value.krRetainCount <= 0) {
                        objectRegistry.remove(it.key)
                    }
                }
                needAutoReleaseNextLoop = false
            }
        }
    }

    private fun throwReflectError(title: String, message: String) {
        throw ReflectException("$title|$message")
    }

    private fun getArgs(splits: List<String>, method: String) : Array<Any>? {
        val args = arrayListOf<Any>()
        for (i in 1 until splits.count()) {
            val split = splits[i]
            val typeLength = split.substring(0, 1).toInt()
            val type = split.substring(1, typeLength + 1)
            val value =  if (split.length > typeLength + 1 ) split.substring(typeLength + 1, split.length) else ""

            var arg : Any? = null
            if (type == NATIVE_OBJECT) {
                arg = getObject(value, method)
                if (arg == null) {
                    throwReflectError("反射参数失败", "objectID:$value 不存在 method：$method")
                    return null
                } else if (arg is KRJavaObject) {
                    arg = arg.toJavaObject()
                }
            } else if (type == BOOLEAN) {
                arg = value.toInt() > 0
            } else if (type == INT_) {
                arg = value.toInt()
            } else if (type == U_INT) {
                arg = value.toUInt()
            } else if (type == SHORT) {
                arg = value.toShort()
            } else if (type == FLOAT) {
                arg = value.toFloat()
            } else if (type == DOUBLE) {
                arg = value.toDouble()
            }  else {
                arg = value
            }
            args.add(arg ?: "")
        }
        return args.toArray()

    }

    private fun retain(params: String?) : String {
        val string = params ?: return ""
        val krObject = getObject(string, "retain") ?: return ""
        (krObject as KRJavaObject).krRetainCount++
        return ""
    }

    private fun release(params: String?) : String {
        val string = params ?: return ""
        val krObject = getObject(string, "release") ?: return ""
        (krObject as KRJavaObject).krRetainCount--
        setNeedAutoReleaseInNextLoop()
        return ""
    }

    companion object {
        const val MODULE_NAME = "KRReflectionModule"
        private const val METHOD_RETAIN = "retain"
        private const val METHOD_RELEASE = "release"
        private const val METHOD_INVOKE = "invoke"
        private const val METHOD_GET_FIELD = "getField"
        private const val METHOD_SET_FIELD = "setField"
        private const val METHOD_TO_STRING = "toString"
        const val SPLIT_TAG = "\n$\t&@\n"
        const val NATIVE_OBJECT = "object"
        const val BOOLEAN = "boolean"
        const val INT_ = "int"
        const val U_INT = "uint"
        const val SHORT = "short"
        const val FLOAT = "float"
        const val DOUBLE = "double"
        const val STRING = "string"
    }
}

fun KRReflect.callMethod(name: String, vararg args: Any?): KRReflect {
    if (name == "newInstance" && get<Any>() == null) {
        return instance(*args)
    }
    return callWithReturn(name, *args)
}