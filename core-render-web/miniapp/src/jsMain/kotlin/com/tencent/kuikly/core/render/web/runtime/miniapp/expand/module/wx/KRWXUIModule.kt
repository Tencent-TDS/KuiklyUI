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
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi

/**
 * Toast / Loading / Modal / ActionSheet 渲染层实现。
 */
class KRWXUIModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        val paramsJson = params.toJSONObjectSafely()
        val args = paramsJson.optJSONObject("args")
        return when (method) {
            METHOD_SHOW_TOAST,
            METHOD_SHOW_LOADING,
            METHOD_SHOW_MODAL,
            METHOD_SHOW_ACTION_SHEET -> {
                KRWXApiBridge.invokeAsync(method, args, callback)
                null
            }

            METHOD_HIDE_TOAST -> {
                NativeApi.plat?.hideToast()
                null
            }

            METHOD_HIDE_LOADING -> {
                NativeApi.plat?.hideLoading()
                null
            }

            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRWXUIModule"

        private const val METHOD_SHOW_TOAST = "showToast"
        private const val METHOD_HIDE_TOAST = "hideToast"
        private const val METHOD_SHOW_LOADING = "showLoading"
        private const val METHOD_HIDE_LOADING = "hideLoading"
        private const val METHOD_SHOW_MODAL = "showModal"
        private const val METHOD_SHOW_ACTION_SHEET = "showActionSheet"
    }
}
