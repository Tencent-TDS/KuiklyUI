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
 * 微信小程序 图片 / 多媒体 API 封装。
 *
 * 对应微信 API:
 * - wx.chooseImage
 * - wx.chooseMedia
 * - wx.previewImage
 * - wx.saveImageToPhotosAlbum
 */
class WXMediaModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /**
     * wx.chooseImage
     * @param count 最多可选数量，默认 9
     * @param sizeType ["original","compressed"]
     * @param sourceType ["album","camera"]
     */
    fun chooseImage(
        count: Int = 9,
        sizeType: List<String> = listOf("original", "compressed"),
        sourceType: List<String> = listOf("album", "camera"),
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            put("count", count)
            put("sizeType", toArray(sizeType))
            put("sourceType", toArray(sourceType))
        }
        invoke(METHOD_CHOOSE_IMAGE, params, onSuccess, onFail)
    }

    /**
     * wx.chooseMedia
     * @param mediaType ["image","video","mix"]
     */
    fun chooseMedia(
        count: Int = 9,
        mediaType: List<String> = listOf("image", "video"),
        sourceType: List<String> = listOf("album", "camera"),
        maxDuration: Int = 10,
        sizeType: List<String> = listOf("original", "compressed"),
        camera: String = "back",
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            put("count", count)
            put("mediaType", toArray(mediaType))
            put("sourceType", toArray(sourceType))
            put("maxDuration", maxDuration)
            put("sizeType", toArray(sizeType))
            put("camera", camera)
        }
        invoke(METHOD_CHOOSE_MEDIA, params, onSuccess, onFail)
    }

    /** wx.previewImage —— 预览图片列表 */
    fun previewImage(
        urls: List<String>,
        current: String? = null,
        showmenu: Boolean = true,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            put("urls", toArray(urls))
            current?.also { put("current", it) }
            put("showmenu", showmenu)
        }
        invoke(METHOD_PREVIEW_IMAGE, params, onSuccess, onFail)
    }

    /** wx.saveImageToPhotosAlbum —— 保存图片到相册（需 scope.writePhotosAlbum） */
    fun saveImageToPhotosAlbum(
        filePath: String,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply { put("filePath", filePath) }
        invoke(METHOD_SAVE_IMAGE, params, onSuccess, onFail)
    }

    private fun toArray(list: List<String>): JSONArray {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr
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
        const val MODULE_NAME = WXModuleConst.MEDIA
        private const val METHOD_CHOOSE_IMAGE = "chooseImage"
        private const val METHOD_CHOOSE_MEDIA = "chooseMedia"
        private const val METHOD_PREVIEW_IMAGE = "previewImage"
        private const val METHOD_SAVE_IMAGE = "saveImageToPhotosAlbum"
    }
}
