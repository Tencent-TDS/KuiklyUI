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

package com.tencent.kuikly.core.module

/*
 * @brief 提供常用的字符串编解码api
 */
class CodecModule : Module() {

    // 对字符串进行 URL 编码
    fun urlEncode(string: String): String {
        return toNative(false, METHOD_URL_ENCODE, string, null, true).toString()
    }

    // 对 URL 编码的字符串进行解码
    fun urlDecode(string: String): String {
        return toNative(false, METHOD_URL_DECODE, string, null, true).toString()
    }

    // 将字符串进行 Base64 编码
    fun base64Encode(string: String): String {
        return toNative(false, METHOD_BASE64_ENCODE, string, null, true).toString()
    }

    // 对 Base64 编码的字符串进行解码
    fun base64Decode(string: String): String {
        return toNative(false, METHOD_BASE64_DECODE, string, null, true).toString()
    }

    // 计算字符串的 MD5 散列值
    fun md5(string: String): String {
        return toNative(false, METHOD_MD5, string, null, true).toString()
    }

    // 计算字符串的 SHA256 散列值
    fun sha256(string: String): String {
        return toNative(false, METHOD_SHA256, string, null, true).toString()
    }

    override fun moduleName(): String {
        return MODULE_NAME
    }

    companion object {
        const val MODULE_NAME = ModuleConst.CODEC
        const val METHOD_URL_ENCODE = "urlEncode"
        const val METHOD_URL_DECODE = "urlDecode"
        const val METHOD_BASE64_ENCODE = "base64Encode"
        const val METHOD_BASE64_DECODE = "base64Decode"
        const val METHOD_MD5 = "md5"
        const val METHOD_SHA256 = "sha256"
    }
}