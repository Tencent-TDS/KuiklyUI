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

package com.tencent.kuikly.core.render.android.expand.module

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject

class KRClipboardModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            SET_TEXT -> setText(params)
            GET_TEXT -> getText()
            else -> super.call(method, params, callback)
        }
    }

    private fun setText(params: String?) {
        val json = try { JSONObject(params ?: "") } catch (_: Exception) { null } ?: return
        val text = json.optString("text", "")
        val clipboard = this.context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun getText(): String {
        val clipboard = this.context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return ""
        val ctx = this.context
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0 && ctx != null) {
            return clip.getItemAt(0).coerceToText(ctx).toString()
        }
        return ""
    }

    companion object {
        const val MODULE_NAME = "KRClipboardModule"
        private const val SET_TEXT = "setText"
        private const val GET_TEXT = "getText"
    }
} 