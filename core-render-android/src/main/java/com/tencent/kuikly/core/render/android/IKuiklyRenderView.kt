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

package com.tencent.kuikly.core.render.android

import android.content.Context
import android.content.Intent
import android.util.Size
import android.view.View
import android.view.ViewGroup
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.expand.component.image.KRImageLoader
import com.tencent.kuikly.core.render.android.expand.component.text.TypeFaceLoader
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderShadowExport
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewPropExternalHandler
import com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreTask
import com.tencent.tdf.module.TDFBaseModule
import com.tencent.tdf.module.TDFModuleProvider

/**
 * 代表KTV页面的根View接口，实现此接口的View，视为kuikly页面的根View
 *
 * <p>外界通过实现此接口的View与Kuikly页面交互和通信
 */
interface IKuiklyRenderView {

    /**
     * 获取实现[IKuiklyRenderView]的View
     */
    val view: ViewGroup

    /**
     * 获取上下文对象[IKuiklyRenderContext]
     */
    val kuiklyRenderContext: IKuiklyRenderContext

    /**
     * 获取暴露给KTV页面侧的暴露类
     *
     * <p>包括 [IKuiklyRenderViewExport], [IKuiklyRenderModuleExport]和[IKuiklyRenderShadowExport]
     */
    val kuiklyRenderExport: IKuiklyRenderExport

    /**
     * 初始化KuiklyRenderView
     * @param contextCode 执行上下文code
     * @param pageName 页面名字
     * @param params 传递给kuiklyCore页面的参数
     * @param size View大小
     * @param assetsPath assets 资源路径
     */
    fun init(
        contextCode: String,
        pageName: String,
        params: Map<String, Any>,
        size: Size? = null,
        assetsPath: String? = null
    )

    /**
     * Native事件发送给Kuikly页面
     * @param event 事件名字
     * @param data 事件数据
     */
    fun sendEvent(event: String, data: Map<String, Any>)

    /**
     * 获取[KuiklyRenderBaseModule]
     * @param T module的类型
     * @param name module名字
     * @return [KuiklyRenderBaseModule]的子类
     */
    fun <T : KuiklyRenderBaseModule> module(name: String): T?

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
     * View 可见
     */
    fun resume()

    /**
     * View 不可见
     */
    fun pause()

    /**
     * 销毁[IKuiklyRenderView]
     */
    fun destroy()

    /**
     * 同步布局和渲染（在当前线程渲染执行队列中所有任务以实现同步渲染）
     */
    fun syncFlushAllRenderTasks()

    /**
     * 执行任务当首屏完成后(优化首屏性能)（仅支持在主线程调用）
     */
    fun performWhenViewDidLoad(task: KuiklyRenderCoreTask)

    /**
     * 添加生命周期回调
     * @param callback
     */
    fun addKuiklyRenderLifecycleCallback(callback: IKuiklyRenderLifecycleCallback)

    /**
     * 移除生命周周期回调
     */
    fun removeKuiklyRenderLifeCycleCallback(callback: IKuiklyRenderLifecycleCallback)

    /**
     * 分发onActivityResult事件
     * @param requestCode
     * @param resultCode
     * @param data
     */
    fun dispatchOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}

/**
 * [IKuiklyRenderView]对象关联的上下文，可用于获取Android的[Context]对象或者是关联数据到[View]对象
 */
interface IKuiklyRenderContext {

    /**
     * 获取[android.content.Context]对象
     */
    val context: Context

    /**
     * 获取[IKuiklyRenderView]对象
     */
    val kuiklyRenderRootView: IKuiklyRenderView?

    /**
     * 初始化 KuiklyContextParams
     */
    fun initContextParams(contextParams: KuiklyContextParams)

    /**
     * 替换当前[IKuiklyRenderContext]
     * @param newContext 新的android上下文
     */
    fun replaceContext(newContext: Context)

    /**
     * 页面的单元转换是否由宿主外部决定
     */
    fun useHostDisplayMetrics(): Boolean

    /**
     * 根据key获取关联在[View]的数据
     * @param T 关联的数据
     * @param view 被关联的对象
     * @param key 数据对应的key
     * @return 关联的数据
     */
    fun <T> getViewData(view: View, key: String): T?

    /**
     * 将数据与[View]对象关联
     * @param view 被关联的对象
     * @param key 关联的key
     * @param data 关联的数据
     */
    fun putViewData(view: View, key: String, data: Any)

    /**
     * 移除关联View的数据
     * @param T 被移除的数据
     * @param view
     * @param key 数据对应的key
     * @return 被移除的对象
     */
    fun <T> removeViewData(view: View, key: String): T?

    /**
     * 清空关联View的数据
     */
    fun clearViewData(view: View)

    /**
     * 获取[KuiklyRenderBaseModule]
     * @param T module的类型
     * @param name module名字
     * @return [KuiklyRenderBaseModule]的子类
     */
    fun <T : KuiklyRenderBaseModule> module(name: String): T?

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
     * 获取图片加载器
     */
    fun getImageLoader(): KRImageLoader?

    /**
     * 获取TypeFace加载器
     */
    fun getTypeFaceLoader(): TypeFaceLoader?

}

/**
 * 获取暴露给KTV页面的暴露类
 * 包括[IKuiklyRenderViewExport], [IKuiklyRenderModuleExport]和[IKuiklyRenderShadowExport]
 */
interface IKuiklyRenderExport : IKuiklyRenderViewPropExternalHandler {

    /**
     * 注册并暴露[IKuiklyRenderModuleExport]给KTV页面
     * @param name 模块名字
     * @param creator 模块创建block
     */
    fun moduleExport(name: String, creator: () -> IKuiklyRenderModuleExport)

    /**
     * 注册 TDF 通用 Module
     *
     * @param clazz module 类型，用于获取 @TDFModule 注解获取 Module 信息
     * @param provider 用于构造 Module，懒加载调用
     */
    fun registerTDFModule(clazz: Class<out TDFBaseModule>, provider: TDFModuleProvider)

    /**
     * 获取 TDF 通用 Module
     *
     * @param moduleName module 名
     */
    fun getTDFModule(moduleName: String): TDFBaseModule

    /**
     * 注册并暴露renderView和shadow给KTV页面
     * @param viewName view名字
     * @param renderViewExportCreator  创建RenderView的闭包
     * @param shadowExportCreator 创建shadow的闭包
     */
    fun renderViewExport(
        viewName: String,
        renderViewExportCreator: (Context) -> IKuiklyRenderViewExport,
        shadowExportCreator: (() -> IKuiklyRenderShadowExport)? = null
    )

    /**
     * 注册View属性自定义处理器
     * @param handler 自定义属性处理器
     */
    fun viewPropExternalHandlerExport(handler: IKuiklyRenderViewPropExternalHandler)

    /**
     * 创建[IKuiklyRenderModuleExport]
     * @param name module对应的名字
     * @return [IKuiklyRenderModuleExport]
     */
    fun createModule(name: String): IKuiklyRenderModuleExport

    /**
     * 创建[IKuiklyRenderViewExport]
     * @param name renderView对应的名字
     * @param context android上下文
     * @return [IKuiklyRenderViewExport]
     */
    fun createRenderView(name: String, context: Context): IKuiklyRenderViewExport

    /**
     * 创建[IKuiklyRenderView]对应的[IKuiklyRenderShadowExport]
     * @param name shadow名字
     * @return [IKuiklyRenderShadowExport]
     */
    fun createRenderShadow(name: String): IKuiklyRenderShadowExport

}

/**
 * 页面打开时相关参数
 */
data class KuiklyContextParams(
    val executeMode: KuiklyRenderCoreExecuteModeBase,
    val pageUrl: String,
    val pageData: Map<String, Any>? = null,
    val assetsPath: String? = null)
/**
 * RenderView 生命周期回调
 */
interface IKuiklyRenderViewLifecycleCallback {

    /**
     * 启动
     */
    fun onInit()

    /**
     * 预加载 class 完成
     */
    fun onPreloadClassFinish()

    /**
     * renderCore 初始化开始
     */
    fun onInitCoreStart()

    /**
     * renderCore 初始化完成
     */
    fun onInitCoreFinish()

    /**
     * 初始化渲染环境开始
     */
    fun onInitContextStart()

    /**
     * 初始化渲染环境完成
     */
    fun onInitContextFinish()

    /**
     * 创建页面开始
     */
    fun onCreateInstanceStart()

    /**
     * 创建页面结束
     */
    fun onCreateInstanceFinish()

    /**
     * 页面首帧渲染完成
     */
    fun onFirstFramePaint()

    /**
     * View 可见
     */
    fun onResume()

    /**
     * View 不可见
     */
    fun onPause()

    /**
     * 页面退出
     */
    fun onDestroy()

    /**
     * 渲染异常
     */
    fun onRenderException(throwable: Throwable, errorReason: ErrorReason)

}

/**
 * ViewTree 更新事件回调
 */
interface IKuiklyRenderViewTreeUpdateListener {

    /**
     * 有更新 View Tree 的任务加入队列时调用
     */
    fun onUpdateViewTreeEnqueued()

    /**
     * 更新 View Tree 任务完成时调用
     */
    fun onUpdateViewTreeFinish()

}

/**
 * Kuikly页面生命周期回调
 */
interface IKuiklyRenderLifecycleCallback {

    /**
     * 上个Activity的Result回调
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @param data 数据
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}

}

/**
 * KuiklyyRenderContext的Wrapper
 * 提供kuiklyRenderContext的引用
 */
interface IKuiklyRenderContextWrapper {

    var kuiklyRenderContext: IKuiklyRenderContext?

}