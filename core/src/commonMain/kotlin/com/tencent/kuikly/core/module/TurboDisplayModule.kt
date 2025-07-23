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
 * @brief TurboDisplay首屏直出渲染模式（通过直接执行二进制产物渲染生成首屏，避免业务代码执行后再生成的首屏等待耗时）
 *        用于首屏直接上屏，彻底告别白屏，极大提升用户体验
 * 注：该TurboDisplay直出技术实现了首屏性能超过原生的突破（注：TurboDisplay首屏可交互）
 */
class TurboDisplayModule : Module() {
    /**
     * 设置当前UI作为下次页面启动的首屏（该首屏可交互)
     */
    fun setCurrentUIAsFirstScreenForNextLaunch() {
        asyncToNativeMethod(CURRENT_UI_AS_FIRST_SCREEN, null, null)
    }

    /**
     * 关闭TurboDisplay首屏直出渲染模式
     */
    fun closeTurboDisplayMode() {
        asyncToNativeMethod(CLOSE_TURBO_DISPLAY, null, null)
    }

    /**
     * 首屏是否为TurboDisplay模式
     */
    fun isTurboDisplay(): Boolean {
        return syncToNativeMethod(IS_TURBO_DISPLAY, null, null) == "1"
    }

    override fun moduleName(): String {
        return MODULE_NAME
    }

    companion object {
        const val MODULE_NAME = ModuleConst.TURBO_DISPLAY
        const val CURRENT_UI_AS_FIRST_SCREEN = "setCurrentUIAsFirstScreenForNextLaunch"
        const val CLOSE_TURBO_DISPLAY = "closeTurboDisplay"
        const val IS_TURBO_DISPLAY = "isTurboDisplay"
    }
}