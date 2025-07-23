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

import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.collection.fastLinkedHashSetOf
import com.tencent.kuikly.core.collection.toFastList
import com.tencent.kuikly.core.manager.PagerManager

class AnimationState {
    private var nextAnimations: MutableList<Animation> = fastArrayListOf()
    private var animIndex: Int = 0     // 记录正常动画的index
    private var layoutIndex: Int? = null   // 记录触发layout动画的index
    private var curAnimations: List<Animation> = listOf()
    var indexChangeCallback: ((animation: Animation?) -> Unit)? = null

    fun addAnimation(animation: Animation, makeDirty: Boolean) {
        nextAnimations.add(animation)
        // 首次触发标脏的index，即为layout动画的index
        if (layoutIndex == null && makeDirty) {
            layoutIndex = animIndex
        }
        animIndex++
        indexChangeCallback?.also { trigger ->
            currentAnimation()?.also {
                trigger(it)
            }
        }
    }

    fun layoutAnimation(): Animation? {
        return curAnimations.getOrElse(layoutIndex?:0) {
            curAnimations.lastOrNull()
        }
    }

    fun currentAnimation(): Animation? {
        return curAnimations.getOrElse(animIndex) {
            curAnimations.lastOrNull()
        }
    }

    fun willBeginAnimation() {
        animIndex = 0
        layoutIndex = null
        curAnimations = nextAnimations.toFastList()
        nextAnimations.clear()
    }

    fun didEndAnimation() {

    }
}

class AnimationManager {

    private val animationsHashMap: HashMap<Pair<String, Int>, AnimationState> by lazy(LazyThreadSafetyMode.NONE) { HashMap<Pair<String, Int>, AnimationState>() }
    private val viewRefToKeys: HashMap<Int, MutableSet<Pair<String, Int>>> by lazy(LazyThreadSafetyMode.NONE) { HashMap<Int, MutableSet<Pair<String, Int>>>() }
    private val currentChangingProperty: String?
        get() {
            return PagerManager.getCurrentReactiveObserver().currentChangingPropertyKey
        }

    private fun genKey(propertyKey: String, viewRef: Int) : Pair<String, Int> {
        return Pair(propertyKey, viewRef)
    }

    fun setAnimation(propertyKey: String, viewRef: Int, animation: Animation, makeDirty: Boolean) {
        val key = genKey(propertyKey, viewRef)
        val animationState = animationsHashMap.getOrPut(key) { AnimationState() }
        viewRefToKeys.getOrPut(viewRef) {
            fastLinkedHashSetOf()
        }.add(key)
        animationState.addAnimation(animation, makeDirty)
    }
    
    fun destroy() {
        viewRefToKeys.clear()
        animationsHashMap.clear()
    }

    fun clearAnimations(viewRef: Int) {
        viewRefToKeys[viewRef]?.let { keys ->
            for (key in keys) {
                animationsHashMap.remove(key)
            }
            viewRefToKeys.remove(viewRef)
        }
    }

    fun willBeginAnimation(viewRef: Int, trigger: (animation: Animation?) -> Unit) {
        currentChangingProperty?.also { propertyKey ->
            val key = genKey(propertyKey, viewRef)
            animationsHashMap[key]?.willBeginAnimation()
        }
        currentAnimationState(viewRef)?.also { animationState ->
            animationState.indexChangeCallback = trigger
            animationState.currentAnimation()?.also {
                trigger(it)
            }
        }
    }

    fun didEndAnimation(viewRef: Int) {
        currentChangingProperty?.also { propertyKey ->
            val key = genKey(propertyKey, viewRef)
            animationsHashMap[key]?.didEndAnimation()
        }
    }

    fun currentLayoutAnimation(viewRef: Int): Animation? {
        return currentAnimationState(viewRef)?.let { animationState ->
            return animationState.layoutAnimation()
        }
    }

    // 如果当前节点有，就从当前节点的idx获取，否则就往父亲找
    private fun currentAnimationState(viewRef: Int): AnimationState? {
        if (animationsHashMap.isEmpty()) {
            return null
        }
        return currentChangingProperty?.let { propertyKey ->
            var curRef = viewRef
            while (curRef != 0) {
                val key = genKey(propertyKey, curRef)
                val animationState = animationsHashMap[key]
                if (animationState != null) {
                    return animationState
                }
                curRef = parentRef(curRef)
            }
            return null
        }
    }

    private fun parentRef(viewRef: Int) : Int {
        return PagerManager.getCurrentPager().getViewWithNativeRef(viewRef)?.parentRef ?: 0
    }
}