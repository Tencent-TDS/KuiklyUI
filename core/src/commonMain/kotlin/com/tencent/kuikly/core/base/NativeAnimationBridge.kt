/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

/**
 * Incremental hook used by Compose to stage a native property-animation transaction.
 *
 * Core deliberately owns only this small bridge contract. The coordinator and all Compose
 * animation policy remain in the Compose module.
 */
interface NativeAnimationBridge {
    fun stageProperty(
        view: AbstractBaseView<*, *>,
        attr: Attr,
        propertyKey: String,
        previousValue: Any?,
        targetValue: Any
    ): Boolean

    fun commitStagedProperties()

    /** Returns true when the frame write was consumed by the pending transaction. */
    fun stageFrame(view: AbstractBaseView<*, *>): Boolean

    fun destroy()

    companion object {
        const val PAGER_CACHE_KEY = "kuikly_compose_native_animation_coordinator"
    }
}

internal fun Attr.stageNativeAnimationPropertyIfNeeded(
    view: AbstractBaseView<*, *>?,
    propertyKey: String,
    previousValue: Any?,
    targetValue: Any
): Boolean {
    if (view == null) return false
    val bridge =
        view.getPager().getValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY)
            as? NativeAnimationBridge
    return bridge?.stageProperty(view, this, propertyKey, previousValue, targetValue) == true
}
