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

package com.tencent.kuikly.core.render.android.adapter

/**
 * Created by kam on 2023/3/27.
 */
object KuiklyRenderAdapterManager {
    /**
     * 线程异常处理适配器
     */
    var krUncaughtExceptionHandlerAdapter: IKRUncaughtExceptionHandlerAdapter? = null

    /**
     * 字体适配器
     */
    var krFontAdapter: IKRFontAdapter? = null

    /**
     * 图片加载适配器
     */
    var krImageAdapter: IKRImageAdapter? = null

    /**
     * 颜色转换适配器
     */
    var krColorParseAdapter: IKRColorParserAdapter? = null

    /**
     * Kuikly Log适配器
     */
    var krLogAdapter: IKRLogAdapter? = null

    /**
     * 页面跳转适配器
     */
    var krRouterAdapter: IKRRouterAdapter? = null

    /**
     * 线程适配器
     */
    var krThreadAdapter: IKRThreadAdapter? = null

    /**
     * PAGView适配器
     */
    var krPagViewAdapter: IKRPAGViewAdapter? = null

    /**
     * APNGView适配器
     */
    var krAPNGViewAdapter: IKRAPNGViewAdapter? = null

    /**
     * VideoView适配器
     */
    var krVideoViewAdapter: IKRVideoViewAdapter? = null

    /**
     * 文本后置处理器适配器
     */
    var krTextPostProcessorAdapter: IKRTextPostProcessorAdapter? = null
}

object KuiklyRenderLog {
    fun i(tag: String, msg: String) {
        KuiklyRenderAdapterManager.krLogAdapter?.i(tag, msg)
    }
    fun d(tag: String, msg: String) {
        KuiklyRenderAdapterManager.krLogAdapter?.d(tag, msg)
    }
    fun e(tag: String, msg: String) {
        KuiklyRenderAdapterManager.krLogAdapter?.e(tag, msg)
    }
}
