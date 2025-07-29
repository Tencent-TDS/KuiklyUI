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

import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.pager.IPager
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface PagerScope {
    val pagerId: String
    fun getPager(): IPager = PagerManager.getPager(pagerId)
}

/**
 * 持有 pagerId 的类实现该接口，以便于在类中使用 by pagerId 的方式获取依赖于 pagerId 构建的对象
 */
@Deprecated("Use PagerScope instead", ReplaceWith("PagerScope"))
interface IPagerId : PagerScope {
    override var pagerId: String
}

/**
 * 根据 [PagerScope.pagerId] 来懒加载 [T] 实例, pagerId 没有变化时使用上一次的缓存值，否则再调用一次 [initializer]
 *
 * 使用方式:
 *
 * ```kotlin
 * // 一个需要依赖于 pagerId 来构建的类
 * class SkinColor(private val pagerId: String) { ... }
 *
 * // pagerId 的持有者
 * class Prop: IPagerId {
 *      override var pagerId: String = ""
 * }
 *
 * // 使用 by pagerId 的方式便捷地构建 SkinColor 实例
 * internal val Props.skinColor: ISkinColor by pagerId {
 *     SkinColorImpl(pagerId)
 * }
 *
 * ```
 *
 */
@Deprecated("Use PagerScope instead")
inline fun <reified T> pagerId(
    noinline initializer: (String) -> T
): ReadOnlyProperty<PagerScope, T> = PagerIdLazyImpl(initializer)

/**
 * 首次被调用 get() 时，会执行 [initializer] 一次，后续如果 pagerId 没变直接返回上一次的缓存值，否则再调一次 [initializer]
 */
class PagerIdLazyImpl<out T>(private val initializer: (String) -> T) :
    ReadOnlyProperty<PagerScope, T> {
    private var _lastPagerId: String? = null
    private var _value: T? = null

    override fun getValue(thisRef: PagerScope, property: KProperty<*>): T {
        val curPagerId = thisRef.pagerId
        if (curPagerId != _lastPagerId || _value == null) {
            _value = initializer(curPagerId)
        }
        _lastPagerId = thisRef.pagerId
        @Suppress("UNCHECKED_CAST")
        return _value as T
    }
}
