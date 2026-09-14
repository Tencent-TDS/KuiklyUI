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

import com.tencent.kuikly.core.collection.fastHashMapOf

private const val ANIMATION_COMPLETION_MAP_KEY = "composeNativeAnimationCompletionMapKey"

private data class NativeAnimationCompletionHandler(
    val oneShot: Boolean,
    val completion: (Boolean) -> Unit
)

fun DeclarativeBaseView<*, *>.registerNativeAnimationCompletion(
    key: String,
    completion: (Boolean) -> Unit
) {
    val completionMap = nativeAnimationCompletionMap()
    completionMap[key] = NativeAnimationCompletionHandler(true, completion)
    installNativeAnimationCompletionListener(completionMap)
}

fun DeclarativeBaseView<*, *>.registerPersistentNativeAnimationCompletion(
    key: String,
    completion: (Boolean) -> Unit
) {
    val completionMap = nativeAnimationCompletionMap()
    completionMap[key] = NativeAnimationCompletionHandler(false, completion)
    installNativeAnimationCompletionListener(completionMap)
}

fun DeclarativeBaseView<*, *>.unregisterNativeAnimationCompletion(key: String) {
    nativeAnimationCompletionMap().remove(key)
}

private fun DeclarativeBaseView<*, *>.installNativeAnimationCompletionListener(
    completionMap: MutableMap<String, NativeAnimationCompletionHandler>
) {
    getViewEvent().listenInternalAnimationCompletion { params ->
        completionMap[params.animationKey]?.let { handler ->
            if (handler.oneShot) completionMap.remove(params.animationKey)
            handler.completion(params.finish.toBoolean())
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun DeclarativeBaseView<*, *>.nativeAnimationCompletionMap():
    MutableMap<String, NativeAnimationCompletionHandler> {
    val existing =
        extProps[ANIMATION_COMPLETION_MAP_KEY]
            as? MutableMap<String, NativeAnimationCompletionHandler>
    if (existing != null) return existing
    return fastHashMapOf<String, NativeAnimationCompletionHandler>().also {
        extProps[ANIMATION_COMPLETION_MAP_KEY] = it
    }
}

internal fun DeclarativeBaseView<*, *>.stageNativeAnimationFrameIfNeeded(): Boolean =
    (
        getPager().getValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY)
            as? NativeAnimationBridge
        )?.stageFrame(this) == true
