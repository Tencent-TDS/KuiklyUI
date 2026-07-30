/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 */

package com.tencent.kuikly.core.render.web.runtime.miniapp.expand.module.wx

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely

/** 图片 / 多媒体 API 渲染层实现。 */
class KRWXMediaModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        val args = params.toJSONObjectSafely().optJSONObject("args")
        return when (method) {
            METHOD_CHOOSE_IMAGE,
            METHOD_CHOOSE_MEDIA,
            METHOD_PREVIEW_IMAGE,
            METHOD_SAVE_IMAGE -> {
                KRWXApiBridge.invokeAsync(method, args, callback)
                null
            }
            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRWXMediaModule"
        private const val METHOD_CHOOSE_IMAGE = "chooseImage"
        private const val METHOD_CHOOSE_MEDIA = "chooseMedia"
        private const val METHOD_PREVIEW_IMAGE = "previewImage"
        private const val METHOD_SAVE_IMAGE = "saveImageToPhotosAlbum"
    }
}
