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
import androidx.annotation.UiThread
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext

/**
 * module组件协议，module通过实现[IKuiklyRenderModuleExport]协议, 将module暴露给kuikly
 */
interface IKuiklyRenderModuleExport {

    /**
     * 调用当前module的实例方法
     * module调用支持同步和异步。
     * 1.如果module方法在kotlin侧是同步调用的话，返回值通使用return返回
     * 2.如果module方法在kotlin侧是异步调用的话，返回值通过callback返回
     * @param method 方法名字
     * @param params 方法参数 (透传kotlin侧数据, 类型可为String, Array, ByteArray, Int，Float)
     * @param callback 回调
     * @return 如果方法调用是同步调用的话，该return值有效(类型可为String, Array, ByteArray, Int，Float)
     */
    fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any?  {
        if (params == null || params is String) {
            return call(method, params as? String, callback)
        }
        return null
    }
    fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? = null

    /**
     * Module销毁时，此方法会被调用，用于销毁module中的资源
     */
    @UiThread fun onDestroy() {}

    /**
     * Kuikly render context
     */
    var kuiklyRenderContext: IKuiklyRenderContext?

    /**
     * 获取实现[IKuiklyRenderViewExport]的View所在的[Activity]
     */
    val activity: Activity?

}

typealias KuiklyRenderCallback = (result: Any?) -> Unit
