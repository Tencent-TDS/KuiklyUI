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

package com.tencent.kuikly.core.render.android.core

import android.view.View
import com.tencent.kuikly.core.render.android.IKuiklyRenderView
import com.tencent.kuikly.core.render.android.IKuiklyRenderViewTreeUpdateListener
import com.tencent.kuikly.core.render.android.context.IKotlinBridgeStatusListener
import com.tencent.kuikly.core.render.android.exception.IKuiklyRenderExceptionListener
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreTask
import com.tencent.tdf.module.TDFBaseModule

/**
 * 渲染流程核心类
 */
interface IKuiklyRenderCore {

    /**
     * 初始化
     * @param renderView 代表KTV页面根View
     * @param contextCode 执行上下文code
     * @param url 页面url, 可以是一个pageName或者是一个带有v_bundleName=$pageName和其他参数的https链接
     * @param params 传递给KTV页面的参数
     * @param assetsPath assets 资源路径
     * @param contextInitCallback 初始化事件回调
     */
    fun init(
        renderView: IKuiklyRenderView,
        contextCode: String,
        url: String,
        params: Map<String, Any>,
        assetsPath: String?,
        contextInitCallback: IKuiklyRenderContextInitCallback
    )

    /**
     * Native发送事件给KTV页面
     * @param event 事件名字
     * @param data 事件数据
     */
    fun sendEvent(event: String, data: Map<String, Any>, shouldSync: Boolean = false)

    /**
     * 获取[IKuiklyRenderModuleExport]
     * @param name 模块名字
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
     * 销毁KuiklyRenderCore
     */
    fun destroy()
    /*
     * 同步布局和渲染（在当前线程渲染执行队列中所有任务以实现同步渲染）
     */
    fun syncFlushAllRenderTasks()

    /*
    * 执行任务当首屏完成后(优化首屏性能)（仅支持在主线程调用）
    */
    fun performWhenViewDidLoad(task: KuiklyRenderCoreTask)

    /**
     * 设置更新 View Tree 监听
     */
    fun setViewTreeUpdateListener(listener: IKuiklyRenderViewTreeUpdateListener)

    /**
     * 设置 Kotlin Bridge 状态监听
     */
    fun setKotlinBridgeStatusListener(listener: IKotlinBridgeStatusListener)

    /**
     * 设置渲染异常监听
     */
    fun setRenderExceptionListener(listener: IKuiklyRenderExceptionListener)
}

/**
 * 渲染环境初始化过程的回调
 */
interface IKuiklyRenderContextInitCallback {

    /**
     * 初始化渲染环境开始
     */
    fun onStart()

    /**
     * 初始化渲染环境完成
     */
    fun onFinish()

    /**
     * 开始调用 kotlin 创建 page 实例
     */
    fun onCreateInstanceStart()

    /**
     * 调用 kotlin 创建 page 实例完成
     */
    fun onCreateInstanceFinish()

}