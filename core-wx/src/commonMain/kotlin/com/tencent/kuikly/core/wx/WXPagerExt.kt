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

package com.tencent.kuikly.core.wx

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.pager.IModuleCreator
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.wx.module.WXApiModule
import com.tencent.kuikly.core.wx.module.WXClipboardModule
import com.tencent.kuikly.core.wx.module.WXLocationModule
import com.tencent.kuikly.core.wx.module.WXMediaModule
import com.tencent.kuikly.core.wx.module.WXRawApiModule
import com.tencent.kuikly.core.wx.module.WXScanModule
import com.tencent.kuikly.core.wx.module.WXShareModule
import com.tencent.kuikly.core.wx.module.WXStorageModule
import com.tencent.kuikly.core.wx.module.WXSystemModule
import com.tencent.kuikly.core.wx.module.WXUIModule

/**
 * Register all WeChat MiniProgram API modules provided by `core-wx` onto this Pager.
 *
 * Usage: call this method from your Pager subclass that actually uses WX APIs,
 * typically inside `createExternalModules()`:
 *
 * ```
 * override fun createExternalModules(): Map<String, Module>? {
 *     registerWXModules()
 *     return super.createExternalModules()
 * }
 * ```
 *
 * This method is safe to call from cross-platform code (commonMain): it has
 * a built-in runtime guard (`pageData.params.is_miniprogram == "1"`) and
 * becomes a no-op on non-MiniProgram runtimes. Android/iOS/macOS/js targets
 * all compile and link without error.
 *
 * Covered modules:
 * - P0: WXApi / WXStorage / WXUI / WXSystem
 * - P1: WXClipboard / WXLocation / WXScan / WXMedia / WXShare
 * - Fallback bridge: WXRawApi (pass-through to arbitrary `wx.xxx`)
 */
fun Pager.registerWXModules() {
    if (pageData.params.optString("is_miniprogram") != "1") {
        return
    }
    registerModule(WXApiModule.MODULE_NAME, object : IModuleCreator {
        override fun createModule(): Module = WXApiModule()
    })
    registerModule(WXStorageModule.MODULE_NAME, object : IModuleCreator {
        override fun createModule(): Module = WXStorageModule()
    })
    registerModule(WXUIModule.MODULE_NAME, object : IModuleCreator {
        override fun createModule(): Module = WXUIModule()
    })
    registerModule(WXSystemModule.MODULE_NAME, object : IModuleCreator {
        override fun createModule(): Module = WXSystemModule()
    })
    registerModule(WXClipboardModule.MODULE_NAME, object : IModuleCreator {
        override fun createModule(): Module = WXClipboardModule()
    })
    registerModule(WXLocationModule.MODULE_NAME, object : IModuleCreator {
        override fun createModule(): Module = WXLocationModule()
    })
    registerModule(WXScanModule.MODULE_NAME, object : IModuleCreator {
        override fun createModule(): Module = WXScanModule()
    })
    registerModule(WXMediaModule.MODULE_NAME, object : IModuleCreator {
        override fun createModule(): Module = WXMediaModule()
    })
    registerModule(WXShareModule.MODULE_NAME, object : IModuleCreator {
        override fun createModule(): Module = WXShareModule()
    })
    // Fallback bridge: passes through arbitrary wx.xxx calls.
    registerModule(WXRawApiModule.MODULE_NAME, object : IModuleCreator {
        override fun createModule(): Module = WXRawApiModule()
    })
}
