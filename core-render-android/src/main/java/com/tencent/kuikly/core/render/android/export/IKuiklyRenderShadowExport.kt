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

import android.util.SizeF
import androidx.annotation.WorkerThread
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext

/**
 * shadow 协议，一般用于自定义布局时实现
 */
interface IKuiklyRenderShadowExport {

    /**
     * 更新shadow对象属性时调用
     * @param propKey 属性key
     * @param propValue 属性value
     */
    @WorkerThread
    fun setProp(propKey: String, propValue: Any)

    /**
     * 调用shadow对象方法
     *
     * @param methodName 方法名
     * @param params 参数
     */
    fun call(methodName: String, params: String): Any? = null

    /**
     * 根据布局约束尺寸计算返回RenderView的实际尺寸
     * @param constraintSize 约束大小
     * @return 计算得到的大小
     */
    @WorkerThread
    fun calculateRenderViewSize(constraintSize: SizeF): SizeF
}
