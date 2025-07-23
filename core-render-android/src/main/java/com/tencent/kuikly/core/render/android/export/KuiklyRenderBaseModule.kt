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

package com.tencent.kuikly.core.render.android.export

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.IKuiklyRenderLifecycleCallback

/**
 * Native暴露给KTV的module基类
 */
open class KuiklyRenderBaseModule : IKuiklyRenderModuleExport {

    private var _kuiklyRenderContext: IKuiklyRenderContext? = null

    /**
     * 获取Context上下文对象
     */
    val context: Context?
        get() {
            return if (kuiklyRenderContext?.context is ContextWrapper) {
                (kuiklyRenderContext?.context as ContextWrapper).baseContext
            } else {
                kuiklyRenderContext?.context
            }
        }

    /**
     * 获取Activity对象
     */
    override val activity: Activity?
        get() {
            var ctx = context
            if (ctx is Activity) {
                return ctx
            } else {
                while (ctx is ContextWrapper) {
                    if (ctx.baseContext is Activity) {
                        return ctx.baseContext as Activity
                    }
                    ctx = ctx.baseContext
                }
            }
            return null
        }

    /**
     * 获取KuiklyRenderContext
     */
    override var kuiklyRenderContext: IKuiklyRenderContext?
        get() = _kuiklyRenderContext
        set(value) {
            _kuiklyRenderContext = value
        }

    /**
     * 根据tag获取[View]
     * @param tag [View]对应的tag
     * @return 获取的[View]
     */
    fun viewWithTag(tag: Int): View? = kuiklyRenderContext?.getView(tag)

    /**
     * 添加生命周期回调
     * @param callback
     */
    fun addKuiklyRenderLifecycleCallback(callback: IKuiklyRenderLifecycleCallback) =
        kuiklyRenderContext?.kuiklyRenderRootView?.addKuiklyRenderLifecycleCallback(callback)

    /**
     * 移除生命周周期回调
     */
    fun removeKuiklyRenderLifeCycleCallback(callback: IKuiklyRenderLifecycleCallback) =
        kuiklyRenderContext?.kuiklyRenderRootView?.removeKuiklyRenderLifeCycleCallback(callback)
}
