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
 * 微信小程序 分享菜单相关 API 封装。
 *
 * 对应微信 API:
 * - wx.showShareMenu
 * - wx.hideShareMenu
 * - wx.updateShareMenu
 *
 * 注：具体的分享内容（onShareAppMessage）需要业务在 Page 级别配置，
 *     该 Module 只负责控制分享菜单的开关与动态参数更新。
 */
class WXShareModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /**
     * wx.showShareMenu
     * @param withShareTicket 是否使用带 shareTicket 的分享
     * @param menus ["shareAppMessage", "shareTimeline"]
     */
    fun showShareMenu(
        withShareTicket: Boolean = false,
        menus: List<String> = listOf("shareAppMessage", "shareTimeline"),
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val arr = JSONArray()
        menus.forEach { arr.put(it) }
        val params = JSONObject().apply {
            put("withShareTicket", withShareTicket)
            put("menus", arr)
        }
        invoke(METHOD_SHOW_SHARE_MENU, params, onSuccess, onFail)
    }

    /** wx.hideShareMenu */
    fun hideShareMenu(
        menus: List<String>? = null,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            menus?.also { list ->
                val arr = JSONArray()
                list.forEach { arr.put(it) }
                put("menus", arr)
            }
        }
        invoke(METHOD_HIDE_SHARE_MENU, params, onSuccess, onFail)
    }

    /**
     * wx.updateShareMenu
     * @param withShareTicket
     * @param isUpdatableMessage 是否为动态消息
     * @param activityId 动态消息 activityId
     * @param templateInfo 动态消息参数
     * @param toDoActivityId 被分享的动态消息的 ID
     */
    fun updateShareMenu(
        withShareTicket: Boolean = false,
        isUpdatableMessage: Boolean = false,
        activityId: String? = null,
        templateInfo: JSONObject? = null,
        toDoActivityId: String? = null,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            put("withShareTicket", withShareTicket)
            put("isUpdatableMessage", isUpdatableMessage)
            activityId?.also { put("activityId", it) }
            templateInfo?.also { put("templateInfo", it) }
            toDoActivityId?.also { put("toDoActivityId", it) }
        }
        invoke(METHOD_UPDATE_SHARE_MENU, params, onSuccess, onFail)
    }

    private fun invoke(
        method: String,
        params: JSONObject?,
        onSuccess: CallbackFn?,
        onFail: CallbackFn?,
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
        const val MODULE_NAME = WXModuleConst.SHARE
        private const val METHOD_SHOW_SHARE_MENU = "showShareMenu"
        private const val METHOD_HIDE_SHARE_MENU = "hideShareMenu"
        private const val METHOD_UPDATE_SHARE_MENU = "updateShareMenu"
    }
}
