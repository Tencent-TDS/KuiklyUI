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

package com.tencent.kuikly.core.base

import com.tencent.kuikly.core.collection.fastHashMapOf
import com.tencent.kuikly.core.manager.PagerManager

@DslMarker
annotation class ScopeMarker

@ScopeMarker
open class BaseObject {
    // 动态扩展字段
    val extProps by lazy(LazyThreadSafetyMode.NONE) {
        fastHashMapOf<String, Any>()
    }
    /*
        * 绑定监听一个表达式变化
        * @param valueBlock 所要监听观测的表达式结果（如：{this.props.key} 即监听key变量变化）
        * @param valueChange 表达式结果变化时回调 （注：首次调用该方法会马上回调，同时变化时也会回调）
        */
    open fun bindValueChange(valueBlock: () -> Any, valueChange: (value: Any) -> Unit) {
         PagerManager.getCurrentReactiveObserver().bindValueChange(valueBlock, this, valueChange)
    }
    /*
     * 删除所有该owner对象所对应的绑定监听（对应配对bindValueChange方法）
     */
    open fun unbindAllValueChange() {
        PagerManager.getCurrentReactiveObserver().unbindValueChange(this)
    }
}

fun Boolean.toInt(): Int {
    if (this) {
        return 1
    }
    return 0
}

fun Int.toBoolean(): Boolean {
    if (this != 0) {
        return true
    }
    return false
}
