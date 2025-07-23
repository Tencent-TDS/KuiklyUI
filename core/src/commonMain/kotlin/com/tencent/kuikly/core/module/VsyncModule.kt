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

/**
 *  监听Vsync回调
 *  created by zhenhuachen on 2025/4/27.
 */
class VsyncModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    fun registerVsync(callback: () -> Unit) {
        toNative(
            keepCallbackAlive = true,
            methodName = METHOD_REGISTER_VSYNC,
            syncCall = false,
            param = null,
            callback = {
                callback()
            }
        )
    }

    fun unRegisterVsync() {
        toNative(
            keepCallbackAlive = false,
            methodName = METHOD_UNREGISTER_VSYNC,
            syncCall = false,
            param = null
        )
    }

    companion object {
        const val MODULE_NAME = ModuleConst.VSYNC
        const val METHOD_REGISTER_VSYNC = "registerVsync"
        const val METHOD_UNREGISTER_VSYNC = "unRegisterVsync"
    }
}