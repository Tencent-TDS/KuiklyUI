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

import com.tencent.kuikly.core.collection.fastLinkedMapOf
import com.tencent.kuikly.core.collection.toFastMap
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.pager.PageData
import com.tencent.kuikly.core.reactive.ReactiveObserver

internal typealias FrameTask = (frame: Frame) -> Unit

abstract class Props : BaseObject(), IPagerId {
    protected val propsMap = fastLinkedMapOf<String, Any>()
    override var pagerId: String = ""
    var nativeRef: Int = 0
    // 是否允许属性值没有变化的时候，也强制更新;
    var forceUpdate = false
    val pagerData: PageData
        get() = getPager().pageData

    open fun viewDidRemove() {
        ReactiveObserver.unbindValueChange(this)
    }

    infix fun String.with(propsValue: Any) {
        bindProp(this, propsValue)
    }

    private fun bindProp(propKey: String, propValue: Any) {
        if (propValue is Function<*>) {
            ReactiveObserver.bindValueChange(this) {
                val newValue = (propValue as () -> Any)()
                setProp(propKey, newValue)
            }
        } else {
            setProp(propKey, propValue)
        }
    }

    fun bindPropBlock(propValue: Any, propBlock: (value: Any) -> Unit) {
        if (propValue is Function<*>) {
            ReactiveObserver.bindValueChange(this) {
                val newValue = (propValue as () -> Any)()
                propBlock(newValue)
            }
        } else {
            propBlock(propValue)
        }
    }

    fun getProp(propKey: String): Any? {
        return propsMap[propKey]
    }

    fun setProp(propKey: String, propValue: Any) {
        if (propsMap[propKey] == propValue && !forceUpdate) {
            return
        }
        propsMap[propKey] = propValue
        view()?.didSetProp(propKey, propValue)
    }

    fun updatePropCache(propKey: String, propValue: Any) {
        propsMap[propKey] = propValue
    }

    fun setNeedLayout() {
        view()?.flexNode?.markDirty()
    }

    fun setPropsToRenderView() {
        view()?.also {
            // 当有圆角和阴影同时存在时 或 有背景渐变的叶子节点时，需要wrapperBoxShadowView兼容对齐安卓表现
            if (getPager().pageData.isIOS
                && ((propsMap.containsKey(Attr.StyleConst.BOX_SHADOW)
                && propsMap.containsKey(Attr.StyleConst.BORDER_RADIUS))
                || (propsMap.containsKey(Attr.StyleConst.BACKGROUND_IMAGE) && (it !is ViewContainer<*, *>)))) {
                it.syncProp(Attr.StyleConst.WRAPPER_BOX_SHADOW_VIEW, 1)
            }
            propsMap.keys.forEach { propKey ->
                propsMap[propKey]?.also { propValue ->
                    it.syncProp(propKey, propValue)
                }
            }
        }
    }

    fun isEmpty(): Boolean {
        return propsMap.isEmpty()
    }

    fun copyPropsMap(): Map<String, Any> {
        return propsMap.toFastMap()
    }

    fun view(): AbstractBaseView<*, *>? {
        if (pagerId.isNotEmpty() && nativeRef != 0) {
            return getPager().getViewWithNativeRef(nativeRef)
        }
        return null
    }
}
