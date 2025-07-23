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
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.css.decoration.IKRViewDecoration
import com.tencent.kuikly.core.render.android.css.ktx.activity
import com.tencent.kuikly.core.render.android.css.ktx.clearViewData
import com.tencent.kuikly.core.render.android.css.ktx.drawCommonDecoration
import com.tencent.kuikly.core.render.android.css.ktx.drawCommonForegroundDecoration
import com.tencent.kuikly.core.render.android.css.ktx.removeOnSetFrameObservers
import com.tencent.kuikly.core.render.android.css.ktx.resetCommonProp
import com.tencent.kuikly.core.render.android.css.ktx.setCommonProp
import com.tencent.kuikly.core.render.android.css.ktx.stopAnimations

/**
 * 渲染视图组件协议, 组件通过实现[IKuiklyRenderViewExport]协议 完成一个kuikly ui组件暴露
 */
interface IKuiklyRenderViewExport : IKuiklyRenderModuleExport, IKRViewDecoration {

    /**
     * 更新属性时调用
     * @param propKey 视图实例属性名
     * @param propValue 视图实例属性值，类型一般为字符串等基础数据结构以及KuiklyRenderCallback（用于事件绑定）
     * @return 是否处理，返回true并且组件是[IKuiklyRenderViewExport.reusable]为true时，
     * [com.tencent.kuikly.core.render.android.layer.IKuiklyRenderLayerHandler]会记录属性的key，
     * 在组件被复用时，调用[IKuiklyRenderViewExport.resetProp]
     * 和[IKuiklyRenderViewExport.resetShadow]方法供组件重置View
     */
    @UiThread
    fun setProp(propKey: String, propValue: Any): Boolean = view().setCommonProp(propKey, propValue)

    override fun drawCommonDecoration(w: Int, h: Int, canvas: Canvas) {
        view().drawCommonDecoration(canvas)
    }

    override fun drawCommonForegroundDecoration(w: Int, h: Int, canvas: Canvas) {
        view().drawCommonForegroundDecoration(canvas)
    }

    /**
     * 重置view, 准备被复用 (可选实现). 若实现该方法返回true则意味着能被复用
     */
    val reusable: Boolean
        get() = false

    /**
     * 重置属性
     * @param propKey 被重置属性的key
     */
    @UiThread
    fun resetProp(propKey: String): Boolean {
        val view = view()
        view.removeOnSetFrameObservers()
        return view.resetCommonProp(propKey)
    }

    /**
     * 重置shadow
     */
    @UiThread
    fun resetShadow() {
    }

    /**
     * 设置当前renderView实例对应的shadow对象
     */
    @UiThread
    fun setShadow(shadow: IKuiklyRenderShadowExport) {
    }

    /**
     * 实现[IKuiklyRenderViewExport]的[android.view.View]
     */
    fun view(): View = this as View

    /**
     * 获取Kuikly页面根View
     * @return Kuikly页面根View
     */
    fun krRootView(): ViewGroup? = kuiklyRenderContext?.kuiklyRenderRootView?.view

    /**
     * Kuikly render context
     */
    override var kuiklyRenderContext: IKuiklyRenderContext?
        get() {
            return view().context as? IKuiklyRenderContext
        }
        set(_) {}

    /**
     * 获取实现[IKuiklyRenderViewExport]的View所在的[Activity]
     */
    override val activity: Activity?
        get() {
             return view().context.activity
        }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "bringToFront" -> {
                view().bringToFront()
            }
            else -> super.call(method, params, callback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        view().stopAnimations()
        view().clearViewData()
    }

    /**
     * 当[View]被add到父亲时，会回调该方法
     * @param parent [View]的父亲
     */
    fun onAddToParent(parent: ViewGroup) {}

    /**
     * 当[View]被父亲remove时，会回调该方法
     * @param parent [View]的父亲
     */
    fun onRemoveFromParent(parent: ViewGroup) {}
}
