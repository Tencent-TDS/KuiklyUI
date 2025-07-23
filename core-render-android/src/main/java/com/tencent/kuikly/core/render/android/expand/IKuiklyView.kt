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

package com.tencent.kuikly.core.render.android.expand

import android.util.Size
import com.tencent.kuikly.core.render.android.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.android.KuiklyRenderView

/**
 * Created by kam on 2023/9/5.
 * Kuikly View粒度级别接入类，如果是页面级别接入，使用[KuiklyRenderViewDelegator]
 */
interface IKuiklyView {

    /**
     * 初始化KuiklyView
     * @param contextCode
     * @param pageName 页面名字
     * @param params 传递给kuiklyCore页面的参数
     * @param size View大小
     * @param assetsPath assets 资源路径
     */
    fun onAttach(
        contextCode: String,
        pageName: String,
        pageData: Map<String, Any>,
        size: Size? = null,
        assetsPath: String? = null
    )

    /**
     * View不可见，在Activity onPause的时机调用
     */
    fun onPause()

    /**
     * View可见，在View可见的时机调用，一般是Activity onResume的时候调用
     */
    fun onResume()

    /**
     * 释放KuiklyView内部的资源，在KuiklyView销毁的时候调用，一般是Activity onResume的时候或者KuiklyView被移除的时候调用
     */
    fun onDetach()

    /**
     * Native事件发送给Kuikly页面
     * @param event 事件名字
     * @param data 事件数据
     */
    fun sendEvent(event: String, data: Map<String, Any>)

    /**
     * 判断本地aar是否有pageName对应的页面
     * @param pageName kuikly页面名字
     * @return 页面是否存在
     */
    fun isPageExist(pageName: String): Boolean

    /**
     * 注册[KuiklyRenderView]生命周期回调
     * @param callback 生命周期回调
     */
    fun addKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback)

    /**
     * 解注册[KuiklyRenderView]生命周期回调
     * @param callback 生命周期回调
     */
    fun removeKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback)

}