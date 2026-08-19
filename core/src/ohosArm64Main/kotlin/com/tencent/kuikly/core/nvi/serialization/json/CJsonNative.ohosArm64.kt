/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.core.nvi.serialization.json

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import ohos.kuikly_cjson_free_string
import ohos.kuikly_cjson_get_array
import ohos.kuikly_cjson_get_bool
import ohos.kuikly_cjson_get_number
import ohos.kuikly_cjson_get_object
import ohos.kuikly_cjson_get_string
import ohos.kuikly_cjson_has
import ohos.kuikly_cjson_key_at
import ohos.kuikly_cjson_owner_root
import ohos.kuikly_cjson_print
import ohos.kuikly_cjson_release
import ohos.kuikly_cjson_retain
import ohos.kuikly_cjson_size
import ohos.kuikly_cjson_value_kind

/**
 * OHOS-only FFI to shared_ptr-backed KuiklyCJsonOwner / cJSON* in render .so.
 *
 * Each Long handle is an opaque heap std::shared_ptr. [retain] returns a new
 * handle sharing the same owner; [release] drops that handle.
 */
@OptIn(ExperimentalForeignApi::class)
internal object CJsonNative {
    /** New handle sharing ownership with [ownerPtr], or 0. */
    fun retain(ownerPtr: Long): Long {
        if (ownerPtr == 0L) {
            return 0L
        }
        return kuikly_cjson_retain(ownerPtr)
    }

    fun release(ownerPtr: Long) {
        if (ownerPtr != 0L) {
            kuikly_cjson_release(ownerPtr)
        }
    }

    fun ownerRoot(ownerPtr: Long): Long {
        if (ownerPtr == 0L) {
            return 0L
        }
        return kuikly_cjson_owner_root(ownerPtr)
    }

    fun hasKey(ptr: Long, key: String): Boolean {
        if (ptr == 0L) {
            return false
        }
        return kuikly_cjson_has(ptr, key) != 0
    }

    fun size(ptr: Long): Int {
        if (ptr == 0L) {
            return 0
        }
        return kuikly_cjson_size(ptr)
    }

    fun keyAt(ptr: Long, index: Int): String? {
        if (ptr == 0L) {
            return null
        }
        return kuikly_cjson_key_at(ptr, index)?.toKString()
    }

    fun valueKind(ptr: Long, key: String): Int {
        if (ptr == 0L) {
            return 0
        }
        return kuikly_cjson_value_kind(ptr, key)
    }

    fun getBool(ptr: Long, key: String, fallback: Boolean): Boolean {
        if (ptr == 0L) {
            return fallback
        }
        return kuikly_cjson_get_bool(ptr, key, if (fallback) 1 else 0) != 0
    }

    fun getNumber(ptr: Long, key: String, fallback: Double): Double {
        if (ptr == 0L) {
            return fallback
        }
        return kuikly_cjson_get_number(ptr, key, fallback)
    }

    fun getString(ptr: Long, key: String): String? {
        if (ptr == 0L) {
            return null
        }
        return kuikly_cjson_get_string(ptr, key)?.toKString()
    }

    fun getObjectPtr(ptr: Long, key: String): Long {
        if (ptr == 0L) {
            return 0L
        }
        return kuikly_cjson_get_object(ptr, key)
    }

    fun getArrayPtr(ptr: Long, key: String): Long {
        if (ptr == 0L) {
            return 0L
        }
        return kuikly_cjson_get_array(ptr, key)
    }

    fun print(ptr: Long): String? {
        if (ptr == 0L) {
            return null
        }
        val p = kuikly_cjson_print(ptr)
        if (p == null) {
            return null
        }
        return try {
            p.toKString()
        } finally {
            kuikly_cjson_free_string(p)
        }
    }
}
