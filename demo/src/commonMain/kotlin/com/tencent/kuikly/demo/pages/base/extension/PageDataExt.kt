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

package com.tencent.kuikly.demo.pages.base.extension

import com.tencent.kuikly.core.base.toBoolean
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.PageData

internal const val USER_DATA = "userData"
internal const val EXT_USER_DATA = "ext_userdata"

internal val PageData.userData: JSONObject
    get() {
        if (params.has(EXT_USER_DATA)) {
            // for android
            return params.optJSONObject(EXT_USER_DATA) ?: JSONObject()
        } else if (params.has(USER_DATA)) {
            // for ios
            return params.optJSONObject(USER_DATA) ?: JSONObject()
        }
        return JSONObject()
    }

internal val PageData.uin: String
    get() {
        return params.optString("uin", "")
    }

internal val PageData.appId: Int
    get() {
        return params.optInt("appId", 0)
    }

internal val PageData.isNightMode: Boolean
    get() {
        return params.optInt("isNightMode", 0).toBoolean()
    }
