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

// NativeClass便捷调用方法接口
fun String.callOC(
    selector: String,
    arg0: Any? = null,
    arg1: Any? = null,
    arg2: Any? = null,
    arg3: Any? = null,
    arg4: Any? = null,
    arg5: Any? = null
): OCObject {
    return OCClass(this).call(selector, arg0, arg1, arg2, arg3, arg4, arg5)
}

fun String.callJava(
    classMethod: String,
    arg0: Any? = null,
    arg1: Any? = null,
    arg2: Any? = null,
    arg3: Any? = null,
    arg4: Any? = null,
    arg5: Any? = null
): JavaObject {
    return JavaClass(this).call(classMethod, arg0, arg1, arg2, arg3, arg4, arg5)
}

fun String.newInstance(
    arg0: Any? = null,
    arg1: Any? = null,
    arg2: Any? = null,
    arg3: Any? = null,
    arg4: Any? = null,
    arg5: Any? = null
): JavaObject {
    return JavaClass(this).newInstance(arg0, arg1, arg2, arg3, arg4, arg5)
}

fun String.getField(
    name: String
): JavaObject {
    return JavaClass(this).getField(name)
}

fun NativeObject<*>.callToString(
    method: String,
    arg0: Any? = null,
    arg1: Any? = null,
    arg2: Any? = null,
    arg3: Any? = null,
    arg4: Any? = null,
    arg5: Any? = null
): String {
    return call(method, arg0, arg1, arg2, arg3, arg4, arg5).toString()
}

fun OCObject.callToInt(
    selector: String,
    arg0: Any? = null,
    arg1: Any? = null,
    arg2: Any? = null,
    arg3: Any? = null,
    arg4: Any? = null,
    arg5: Any? = null
): Int {
    return call(selector, arg0, arg1, arg2, arg3, arg4, arg5).toInt()
}

fun OCObject.callToLong(
    selector: String,
    arg0: Any? = null,
    arg1: Any? = null,
    arg2: Any? = null,
    arg3: Any? = null,
    arg4: Any? = null,
    arg5: Any? = null
): Long {
    return call(selector, arg0, arg1, arg2, arg3, arg4, arg5).toLong()
}

fun OCObject.callToFloat(
    selector: String,
    arg0: Any? = null,
    arg1: Any? = null,
    arg2: Any? = null,
    arg3: Any? = null,
    arg4: Any? = null,
    arg5: Any? = null
): Float {
    return call(selector, arg0, arg1, arg2, arg3, arg4, arg5).toFloat()
}

fun OCObject.callToDouble(
    selector: String,
    arg0: Any? = null,
    arg1: Any? = null,
    arg2: Any? = null,
    arg3: Any? = null,
    arg4: Any? = null,
    arg5: Any? = null
): Double {
    return call(selector, arg0, arg1, arg2, arg3, arg4, arg5).toDouble()
}

fun NativeObject<*>.toInt(): Int {
    return toString().toIntOrNull() ?: 0
}

fun NativeObject<*>.toLong(): Long {
    return toString().toLongOrNull() ?: 0L
}

fun NativeObject<*>.toFloat(): Float {
    return toString().toFloatOrNull() ?: 0f
}

fun NativeObject<*>.toDouble(): Double {
    return toString().toDoubleOrNull() ?: 0.0
}
