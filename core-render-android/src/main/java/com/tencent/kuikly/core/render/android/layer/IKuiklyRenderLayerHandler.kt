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

package com.tencent.kuikly.core.render.android.layer

import android.graphics.RectF
import android.util.SizeF
import android.view.View
import com.tencent.kuikly.core.render.android.IKuiklyRenderView
import com.tencent.kuikly.core.render.android.KuiklyContextParams
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderShadowExport
import com.tencent.tdf.module.TDFBaseModule

/**
 * Kuikly页面渲染层协议
 */
interface IKuiklyRenderLayerHandler {

    /**
     * 初始化
     * @param renderView KTV页面的根View
     */
    fun init(renderView: IKuiklyRenderView)

    /**
     * 创建渲染试图
     * @param tag 视图id
     * @param viewName 视图标签名字
     */
    fun createRenderView(tag: Int, viewName: String)

    /**
     * 删除渲染试图
     * @param tag 视图id
     */
    fun removeRenderView(tag: Int)

    /**
     * 父渲染视图插入子渲染视图
     * @param parentTag 父视图id
     * @param childTag 子视图id
     * @param index 插入的位置
     */
    fun insertSubRenderView(parentTag: Int, childTag: Int, index: Int)

    /**
     * 设置渲染视图属性
     * @param tag 视图id
     * @param propKey 属性key
     * @param propValue 属性值
     */
    fun setProp(tag: Int, propKey: String, propValue: Any)

    /**
     * 设置view对应的shadow对象
     * @param tag 视图id
     * @param shadow 视图对应的shadow对象
     */
    fun setShadow(tag: Int, shadow: IKuiklyRenderShadowExport)

    /**
     * 渲染视图更新坐标
     * @param tag 视图id
     * @param frame 视图坐标
     */
    fun setRenderViewFrame(tag: Int, frame: RectF)

    /**
     * 渲染视图返回自定义布局尺寸
     * @param tag 视图id
     * @param constraintSize 约束尺寸
     * @return 计算得到的尺寸
     */
    fun calculateRenderViewSize(tag: Int, constraintSize: SizeF): SizeF

    /**
     * 调用渲染视图方法
     * @param tag 视图id
     * @param method 视图方法
     * @param params 方法参数
     * @param callback 回调
     */
    fun callViewMethod(
        tag: Int,
        method: String,
        params: String?,
        callback: KuiklyRenderCallback?
    )

    /**
     * 调用module方法
     * @param moduleName module 名字
     * @param method module 方法
     * @param params 参数
     * @param callback 回调
     */
    fun callModuleMethod(
        moduleName: String,
        method: String,
        params: Any?,
        callback: KuiklyRenderCallback?
    ): Any?

    /**
     * 调用 TDF 通用 Module 方法
     * @param moduleName module 名字
     * @param method module 方法
     * @param params 参数，Json 字符串
     * @param callId, 使用 successCallback Id，如果没有就使用 TDFModulePromise#CALL_ID_NO_CALLBACK
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    fun callTDFModuleMethod(
        moduleName: String,
        method: String,
        params: String?,
        callId: String?,
        successCallback: KuiklyRenderCallback?,
        errorCallback: KuiklyRenderCallback?
    ): Any?

    /**
     * 创建shadow
     * @param tag 视图id
     * @param viewName 视图名字
     */
    fun createShadow(tag: Int, viewName: String)

    /**
     * 删除shadow
     * @param tag 视图id
     */
    fun removeShadow(tag: Int)

    /**
     * 更新shadow对象属性
     * @param tag 视图id
     * @param propKey 属性key
     * @param propValue 属性value
     */
    fun setShadowProp(tag: Int, propKey: String, propValue: Any)

    /**
     * 根据视图id获取shadow
     * @param tag 视图id
     * @return [IKuiklyRenderShadowExport]
     */
    fun shadow(tag: Int): IKuiklyRenderShadowExport?

    /**
     * 调用 shadow 对象方法
     *
     * @param tag 视图id
     * @param methodName 方法名
     * @param params 参数
     */
    fun callShadowMethod(tag: Int, methodName: String, params: String): Any?

    /**
     * 根据name获取[IKuiklyRenderModuleExport]
     * @param T 实现[IKuiklyRenderModuleExport]的类
     * @param name module名字
     * @return 实现[IKuiklyRenderModuleExport]的类
     */
    fun <T : IKuiklyRenderModuleExport> module(name: String): T?

    /**
     * 获取 TDF 通用 Module
     * @param T module 类型
     * @param name module 名字
     * @return module
     */
    fun <T : TDFBaseModule> getTDFModule(name: String): T?

    /**
     * 根据tag获取[View]
     * @param tag [View]对应的tag
     * @return 获取的[View]
     */
    fun getView(tag: Int): View?

    /**
     * [IKuiklyRenderView]实例销毁时，此方法会被调用
     */
    fun onDestroy()
}