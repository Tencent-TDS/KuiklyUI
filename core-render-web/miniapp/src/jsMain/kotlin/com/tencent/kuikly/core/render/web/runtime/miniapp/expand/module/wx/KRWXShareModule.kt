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

/** 分享菜单 API 渲染层实现。 */
class KRWXShareModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        val args = params.toJSONObjectSafely().optJSONObject("args")
        return when (method) {
            METHOD_SHOW_SHARE_MENU,
            METHOD_HIDE_SHARE_MENU,
            METHOD_UPDATE_SHARE_MENU -> {
                KRWXApiBridge.invokeAsync(method, args, callback)
                null
            }
            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRWXShareModule"
        private const val METHOD_SHOW_SHARE_MENU = "showShareMenu"
        private const val METHOD_HIDE_SHARE_MENU = "hideShareMenu"
        private const val METHOD_UPDATE_SHARE_MENU = "updateShareMenu"
    }
}
