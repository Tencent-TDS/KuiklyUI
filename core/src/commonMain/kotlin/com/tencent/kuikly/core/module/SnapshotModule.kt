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

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

class SnapshotModule : Module() {

    /*
     * @brief 生成当前pager的快照，用于下次打开该pager的首屏未出现时以该快照作为首屏，实现首屏0白屏体验
     * @param snapshotKey 快照的唯一key，用户端侧使用该快照的key，建议一般该key以（版本号+页面名+夜间模式）作为key
     */
    fun snapshotPager(snapshotKey: String) {
        val params = JSONObject()
        params.put("snapshotKey", snapshotKey)
        toNative(
            false,
            METHOD_SNAPSHOT_PAGER,
            params.toString(),
            null,
            false
        )

    }

    override fun moduleName(): String {
        return MODULE_NAME
    }

    companion object {
        const val MODULE_NAME = ModuleConst.SNAPSHOT
        const val METHOD_SNAPSHOT_PAGER = "snapshotPager"
    }

}