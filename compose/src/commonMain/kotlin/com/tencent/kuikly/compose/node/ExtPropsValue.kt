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

package com.tencent.kuikly.compose.node

import com.tencent.kuikly.core.base.BaseObject
import kotlin.reflect.KProperty

internal interface ExtPropsVar<R> {
    operator fun getValue(thisRef: BaseObject, property: KProperty<*>): R
    operator fun setValue(thisRef: BaseObject, property: KProperty<*>, value: R)
}

internal fun <T : Any> extPropsVar(key: String, initVal: () -> T) = object : ExtPropsVar<T> {
    override fun getValue(thisRef: BaseObject, property: KProperty<*>): T {
        return thisRef.extProps.getOrPut(key, initVal) as T
    }

    override fun setValue(thisRef: BaseObject, property: KProperty<*>, value: T) {
        thisRef.extProps[key] = value
    }
}
