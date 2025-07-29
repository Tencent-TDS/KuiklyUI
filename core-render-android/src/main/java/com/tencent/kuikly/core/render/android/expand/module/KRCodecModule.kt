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

import android.util.Base64
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.math.BigInteger
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Created by kam on 2023/5/9.
 */
class KRCodecModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_URL_ENCODE -> urlEncode(params)
            METHOD_URL_DECODE -> urlDecode(params)
            METHOD_BASE64_ENCODE -> base64Encode(params)
            METHOD_BASE64_DECODE -> base64Decode(params)
            METHOD_MD5 -> md5(params)
            METHOD_SHA256 -> sha256(params as? String ?: "")
            else -> super.call(method, params, callback)
        }
    }

    fun md5(params: String?): String {
        val string = params ?: return ""
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(string.toByteArray())
        val bigInt = BigInteger(1, digest)
        val md5Hash32 = bigInt.toString(16).padStart(32, '0')
        return md5Hash32.substring(8, 24) // 截取中间的16位
    }

    fun sha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            val hexString = StringBuilder()

            for (b in hash) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }

            hexString.toString()
        } catch (e: Throwable) {
            KuiklyRenderLog.e(TAG, "SHA-256 algorithm not found: ${e}")
            return ""
        }
    }

    private fun base64Decode(params: String?): String {
        val string = params ?: return ""
        return String(Base64.decode(string, Base64.NO_WRAP))
    }

    private fun base64Encode(params: String?): String {
        return KRCodecModule.base64Encode(params)
    }

    private fun urlDecode(params: String?): String {
        val string =  params ?: return ""
        return URLDecoder.decode(string)
    }

    private fun urlEncode(params: String?): String {
        val string = params ?: return ""
        return URLEncoder.encode(string)
    }
    companion object {
        const val MODULE_NAME = "KRCodecModule"
        const val TAG = MODULE_NAME
        private const val METHOD_URL_ENCODE = "urlEncode"
        private const val METHOD_URL_DECODE = "urlDecode"
        private const val METHOD_BASE64_ENCODE = "base64Encode"
        private const val METHOD_BASE64_DECODE = "base64Decode"
        private const val METHOD_MD5 = "md5"
        private const val METHOD_SHA256 = "sha256"

        fun base64Encode(params: String?): String {
            val string = params ?: return ""
            return Base64.encodeToString(string.toByteArray(), Base64.NO_WRAP)
        }
    }
}