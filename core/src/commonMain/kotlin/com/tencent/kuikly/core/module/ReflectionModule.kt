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

import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.reflection.NativeObject

/*
 * 反射类内部用到的native反射原子接口，禁止直接使用
 */
internal class ReflectionModule : Module() {
    override fun moduleName(): String = MODULE_NAME
    fun retain(objc: NativeObject<*>): String {
        return toNative(
            false,
            METHOD_RETAIN,
            objc.objectID,
            null,
            true
        ).toString()
    }

    fun release(objc: NativeObject<*>): String {
        return toNative(
            false,
            METHOD_RELEASE,
            objc.objectID,
            null,
            true
        ).toString()
    }

    fun getField(objc: NativeObject<*>, name: String) : String {
        return toNative(
            false,
            METHOD_GET_FIELD,
            objc.objectID + "|" + name,
            null,
            true
        ).toString()
    }

    fun setField(objc: NativeObject<*>, name: String, encodeArgs: String)  {
        toNative(
            false,
             METHOD_SET_FIELD,
            objc.objectID + "|" + name + encodeArgs,
            null,
            true
        )
    }

    fun toString(objc: NativeObject<*>): String {
        return toNative(
            false,
            METHOD_TO_STRING,
            objc.objectID,
            null,
            true
        ).toString()
    }

    fun call(objc: NativeObject<*>, method: String, encodeArgs: String): String {
        return toNative(
            false,
            METHOD_INVOKE,
            objc.objectID + "|" + method + encodeArgs,
            null,
            true
        ).toString()
    }

    companion object {
        const val MODULE_NAME = ModuleConst.REFLECTION
        const val METHOD_RETAIN = "retain"
        const val METHOD_RELEASE = "release"
        const val METHOD_GET_FIELD = "getField"
        const val METHOD_SET_FIELD = "setField"
        const val METHOD_TO_STRING = "toString"
        const val METHOD_INVOKE = "invoke"
        fun instance(): ReflectionModule {
            return PagerManager.getCurrentPager()
                .acquireModule<ReflectionModule>(ReflectionModule.MODULE_NAME)
        }
    }

}