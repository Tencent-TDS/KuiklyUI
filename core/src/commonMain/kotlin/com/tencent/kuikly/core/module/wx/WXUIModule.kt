/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 */

package com.tencent.kuikly.core.module.wx

import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 微信小程序交互类 API 的封装（Toast / Modal / Loading / ActionSheet）。
 *
 * 对应微信 API:
 * - wx.showToast / wx.hideToast
 * - wx.showLoading / wx.hideLoading
 * - wx.showModal
 * - wx.showActionSheet
 */
class WXUIModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /**
     * wx.showToast
     * @param icon "success" | "error" | "loading" | "none"
     * @param duration 毫秒
     * @param mask 是否展示透明蒙层，防止触摸穿透
     */
    fun showToast(
        title: String,
        icon: String = "success",
        duration: Int = 1500,
        mask: Boolean = false,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null
    ) {
        val params = JSONObject().apply {
            put("title", title)
            put("icon", icon)
            put("duration", duration)
            put("mask", mask)
        }
        invoke(METHOD_SHOW_TOAST, params, onSuccess, onFail)
    }

    /** wx.hideToast */
    fun hideToast() {
        invoke(METHOD_HIDE_TOAST, null, null, null)
    }

    /**
     * wx.showLoading
     * @param mask 是否展示透明蒙层
     */
    fun showLoading(
        title: String = "加载中",
        mask: Boolean = false,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null
    ) {
        val params = JSONObject().apply {
            put("title", title)
            put("mask", mask)
        }
        invoke(METHOD_SHOW_LOADING, params, onSuccess, onFail)
    }

    /** wx.hideLoading */
    fun hideLoading() {
        invoke(METHOD_HIDE_LOADING, null, null, null)
    }

    /**
     * wx.showModal
     * 回调参数 data 字段：
     * - confirm: Boolean 是否点击了确认
     * - cancel:  Boolean 是否点击了取消
     * - content: String? 用户在 editable=true 时的输入内容
     */
    fun showModal(
        title: String? = null,
        content: String? = null,
        showCancel: Boolean = true,
        cancelText: String = "取消",
        cancelColor: String = "#000000",
        confirmText: String = "确定",
        confirmColor: String = "#576B95",
        editable: Boolean = false,
        placeholderText: String = "",
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null
    ) {
        val params = JSONObject().apply {
            title?.also { put("title", it) }
            content?.also { put("content", it) }
            put("showCancel", showCancel)
            put("cancelText", cancelText)
            put("cancelColor", cancelColor)
            put("confirmText", confirmText)
            put("confirmColor", confirmColor)
            put("editable", editable)
            put("placeholderText", placeholderText)
        }
        invoke(METHOD_SHOW_MODAL, params, onSuccess, onFail)
    }

    /**
     * wx.showActionSheet
     * 回调参数 data 字段：tapIndex: Int
     */
    fun showActionSheet(
        itemList: List<String>,
        itemColor: String = "#000000",
        alertText: String? = null,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null
    ) {
        val arr = JSONArray()
        itemList.forEach { arr.put(it) }
        val params = JSONObject().apply {
            put("itemList", arr)
            put("itemColor", itemColor)
            alertText?.also { put("alertText", it) }
        }
        invoke(METHOD_SHOW_ACTION_SHEET, params, onSuccess, onFail)
    }

    private fun invoke(
        method: String,
        params: JSONObject?,
        onSuccess: CallbackFn?,
        onFail: CallbackFn?
    ) {
        val wrapped = JSONObject().apply {
            put(WXApiModule.KEY_ARGS, params ?: JSONObject())
        }
        asyncToNativeMethod(method, wrapped) { data ->
            val ok = data?.optInt(WXApiModule.KEY_OK, 0) == 1
            val payload = data?.optJSONObject(WXApiModule.KEY_DATA)
            if (ok) onSuccess?.invoke(payload) else onFail?.invoke(payload)
        }
    }

    companion object {
        const val MODULE_NAME = WXModuleConst.UI

        private const val METHOD_SHOW_TOAST = "showToast"
        private const val METHOD_HIDE_TOAST = "hideToast"
        private const val METHOD_SHOW_LOADING = "showLoading"
        private const val METHOD_HIDE_LOADING = "hideLoading"
        private const val METHOD_SHOW_MODAL = "showModal"
        private const val METHOD_SHOW_ACTION_SHEET = "showActionSheet"
    }
}
