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

package com.tencent.kuikly.core.render.android.expand.component.image

import android.graphics.drawable.Drawable
import com.tencent.kuikly.core.render.android.adapter.HRImageLoadOption
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase

typealias FetchImageCallback = (drawable: Drawable?) -> Unit

/**
 * 图片加载器
 */
class KRImageLoader(val executeMode: KuiklyRenderCoreExecuteModeBase, val assetsPath: String?) {

    /**
     * 异步拉取图片并解码返回 Drawable
     *
     * @param options 图片加载参数
     * @param callback 图片加载回调
     */
    fun fetchImageAsync(options: HRImageLoadOption, callback: FetchImageCallback) {
        convertAssetsPathIfNeed(options)
        KuiklyRenderAdapterManager.krImageAdapter?.fetchDrawable(options, callback)
    }

    /**
     * 非内置场景需要将 assets 图片转换为本地路径
     */
    private fun convertAssetsPathIfNeed(options: HRImageLoadOption) {
        executeMode.convertAssetsPathIfNeed(options, assetsPath)
    }

}