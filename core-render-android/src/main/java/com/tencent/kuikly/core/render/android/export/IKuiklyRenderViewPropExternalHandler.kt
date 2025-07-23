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

/**
 * [IKuiklyRenderViewExport]自定义属性Handler
 */
interface IKuiklyRenderViewPropExternalHandler {

    /**
     * 设置属性
     * @param renderViewExport
     * @param propKey
     * @param propValue
     * @return 是否处理该属性
     */
    fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean

    /**
     * 重置属性，只有在[IKuiklyRenderViewExport]是可复用的情况下，才会被调用
     * @param renderViewExport
     * @param propKey
     * @return 是否处理了resetProp
     */
    fun resetViewExternalProp(renderViewExport: IKuiklyRenderViewExport, propKey: String): Boolean
}
