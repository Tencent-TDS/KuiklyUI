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

package com.tencent.kuikly.core.utils

import com.tencent.kuikly.core.collection.fastLinkedMapOf

fun getParamFromUrl(url: String, key: String): String {
    val paramFirstIndex = url.indexOfFirst {
        it.equals('?')
    }
    if (paramFirstIndex == -1) {
        return ""
    }

    url.substring(paramFirstIndex + 1).split("&").forEach { kvStr ->
        val kvPair = kvStr.split("=")
        if (kvPair.size == 2 && key.equals(kvPair[0])) {
            return kvPair[1]
        }
    }
    return ""
}

fun urlParams(url: String): Map<String, String> {
    val params = fastLinkedMapOf<String, String>()
    val urlParamStr = url.split("?").last()
    urlParamStr.split("&").forEach {
        val keyValue = it.split("=")
        if (keyValue.count() == 2) {
            val key = keyValue.first()
            val value = keyValue.last()
            params[key] = value
        }
    }
    return params
}