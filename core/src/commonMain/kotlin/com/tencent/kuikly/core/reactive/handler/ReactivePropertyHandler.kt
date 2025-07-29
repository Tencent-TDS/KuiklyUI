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

package com.tencent.kuikly.core.reactive.handler

import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.reactive.ObservableCollectionProperty
import com.tencent.kuikly.core.reactive.ObservableProperties
import com.tencent.kuikly.core.reactive.ObservableProvider
import com.tencent.kuikly.core.reactive.ObservableThreadSafetyMode
import com.tencent.kuikly.core.reactive.ReactiveObserver
import com.tencent.kuikly.core.reactive.UnsafePropertyAccessHandlerImpl
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.collection.ObservableSet
import kotlin.properties.ReadWriteProperty

/* 响应式部分 */
/**
 * 利用委托的机制，将一个字段变成可观测字段
 */
// 非容器属性 用法：var valueProp: Int by observable(0)
@Deprecated(
    "use PagerScope.observable(mode, value) instead",
    ReplaceWith(
        "observable<T>(ObservableThreadSafetyMode.NONE, init)",
        "com.tencent.kuikly.core.reactive.ObservableThreadSafetyMode"
    )
)
fun <T> observable(init: T): ReadWriteProperty<Any, T> {
    return ReactiveObserver.observable(init)
}

// 列表容器   用法：var listProp: ObservableList<String>  by observableList<String>()
@Deprecated("use PagerScope.observableList(mode) instead",
    ReplaceWith(
        "observableList<T>(ObservableThreadSafetyMode.NONE)",
        "com.tencent.kuikly.core.reactive.ObservableThreadSafetyMode"
    )
)
fun <T> observableList(): ReadWriteProperty<Any, ObservableList<T>> {
    return ReactiveObserver.observableList()
}

@Deprecated("use PagerScope.observableSet(mode) instead",
    ReplaceWith(
        "observableSet<T>(ObservableThreadSafetyMode.NONE)",
        "com.tencent.kuikly.core.reactive.ObservableThreadSafetyMode"
    )
)
fun <T> observableSet(): ReadWriteProperty<Any, ObservableSet<T>> {
    return ReactiveObserver.observableSet()
}

fun <T> PagerScope.observable(mode: ObservableThreadSafetyMode, init: T): ReadWriteProperty<Any, T> {
    if (mode == ObservableThreadSafetyMode.NONE) {
        return ObservableProperties(init, UnsafePropertyAccessHandlerImpl(this))
    }
    if (mode !is ObservableProvider) {
        throwRuntimeError("observable $mode is not implemented")
    }
    return (mode as ObservableProvider).observable(this, init)
}

fun <T> PagerScope.observableList(mode: ObservableThreadSafetyMode): ReadWriteProperty<Any, ObservableList<T>> {
    if (mode == ObservableThreadSafetyMode.NONE) {
        return ObservableCollectionProperty(ObservableList(), UnsafePropertyAccessHandlerImpl(this))
    }
    if (mode !is ObservableProvider) {
        throwRuntimeError("observableList $mode is not implemented")
    }
    return (mode as ObservableProvider).observableList(this)
}

fun <T> PagerScope.observableSet(mode: ObservableThreadSafetyMode): ReadWriteProperty<Any, ObservableSet<T>> {
    if (mode == ObservableThreadSafetyMode.NONE) {
        return ObservableCollectionProperty(ObservableSet(), UnsafePropertyAccessHandlerImpl(this))
    }
    if (mode !is ObservableProvider) {
        throwRuntimeError("observableSet $mode is not implemented")
    }
    return (mode as ObservableProvider).observableSet(this)
}

inline fun <T> PagerScope.observable(init: T) = observable(ObservableThreadSafetyMode.NONE, init)
inline fun <T> PagerScope.observableList() = observableList<T>(ObservableThreadSafetyMode.NONE)
inline fun <T> PagerScope.observableSet() = observableSet<T>(ObservableThreadSafetyMode.NONE)