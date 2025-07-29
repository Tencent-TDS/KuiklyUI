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

package com.tencent.kuikly.demo.pages.base.ktx

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray

/**
 * Created by zhiyueli on 2022/11/7 17:19.
 */
internal inline fun <reified T, R> JSONArray.map(map: (T) -> R): List<R> {
    val result = mutableListOf<R>()
    forEachIndex<T> { _, v ->
        result.add(map(v))
    }
    return result
}

internal inline fun <reified T> JSONArray.forEach(loop: (T) -> Unit) {
    forEachIndex<T> { _, v ->
        loop(v)
    }
}

internal inline fun <reified T> JSONArray.forEachIndex(loop: (Int, T) -> Unit) {
    for (i in 0 until this.length()) {
        val item = when {
            Boolean is T -> {
                this.optBoolean(i)
            }
            Int is T -> {
                this.optInt(i)
            }
            Long is T -> {
                this.optLong(i)
            }
            String is T -> {
                this.optString(i)
            }
            Double is T -> {
                this.optDouble(i)
            }
            else -> {
                this.opt(i)
            }
        }

        loop(i, item as T)
    }
}
