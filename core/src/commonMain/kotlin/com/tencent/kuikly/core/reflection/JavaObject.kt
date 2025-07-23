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

import com.tencent.kuikly.core.module.ReflectionModule

open class JavaObject(objectId : String) : NativeObject<JavaObject>(objectId) {

    override fun createNativeObject(objectID: String): JavaObject {
        if (objectID.isEmpty()) {
            return EMPTY_OBJECT
        }
        return JavaObject(objectID)
    }
    /*
     * 获取对象属性
     */
    fun getField(name: String) : JavaObject {
        val objectId = ReflectionModule.instance().getField(this, name)
        return createNativeObject(objectId)
    }

    /*
    * 设置对象属性
    * @param 属性名
    * @pram value 被设置的值，支持NativeObject类，Boolean，Int，Float，Double，String类型
    */
    fun setField(name: String, value : Any) {
        val argsString = encodeArg(value)
        ReflectionModule.instance().setField(this, name, argsString)
    }

    override fun emptyConstObject(): JavaObject {
        return EMPTY_OBJECT
    }

    companion object {
        val EMPTY_OBJECT = JavaObject("")
    }
}