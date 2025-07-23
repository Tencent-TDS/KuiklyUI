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

#ifndef CORE_RENDER_OHOS_MODULESREGISTERENTRY_H
#define CORE_RENDER_OHOS_MODULESREGISTERENTRY_H

#include "libohos_render/expand/modules/back_press/KRBackPressModule.h"
#include "libohos_render/expand/modules/cache/KRMemoryCacheModule.h"
#include "libohos_render/expand/modules/calendar/KRCalendarModule.h"
#include "libohos_render/expand/modules/codec/KRCodecModule.h"
#include "libohos_render/expand/modules/forward/KRForwardArkTSModule.h"
#include "libohos_render/expand/modules/log/KRLogModule.h"
#include "libohos_render/expand/modules/network/KRNetworkModule.h"
#include "libohos_render/expand/modules/performance/KRPerformanceModule.h"
#include "libohos_render/expand/modules/preferences/KRSharedPreferencesModule.h"
#include "libohos_render/export/IKRRenderModuleExport.h"

#endif  // CORE_RENDER_OHOS_MODULESREGISTERENTRY_H

/**
 * 内置Module均在此注册生成实例闭包
 */
static void ModulesRegisterEntry() {
    // 注册通用转发调用ArkTS层 Module
    IKRRenderModuleExport::RegisterForwardArkTSModuleCreator([] { return std::make_shared<KRForwardArkTSModule>(); });
    IKRRenderModuleExport::RegisterModuleCreator(kMemoryCacheModuleName,
                                                 [] { return std::make_shared<KRMemoryCacheModule>(); });
    IKRRenderModuleExport::RegisterModuleCreator(kLogModuleName, [] { return std::make_shared<KRLogModule>(); });

    IKRRenderModuleExport::RegisterModuleCreator(kNetworkModuleName,
                                                 [] { return std::make_shared<KRNetworkModule>(); });

    IKRRenderModuleExport::RegisterModuleCreator(kuikly::expand::KRSharedPreferencesModule::MODULE_NAME, [] {
        return std::make_shared<kuikly::expand::KRSharedPreferencesModule>();
    });

    IKRRenderModuleExport::RegisterModuleCreator(kuikly::module::KRCodecModule::MODULE_NAME,
                                                 [] { return std::make_shared<kuikly::module::KRCodecModule>(); });

    IKRRenderModuleExport::RegisterModuleCreator(kuikly::module::KRCalendarModule::MODULE_NAME,
                                                 [] { return std::make_shared<kuikly::module::KRCalendarModule>(); });

    IKRRenderModuleExport::RegisterModuleCreator(kuikly::module::KRPerformanceModule::MODULE_NAME, [] {
        return std::make_shared<kuikly::module::KRPerformanceModule>();
    });
    
    IKRRenderModuleExport::RegisterModuleCreator(kuikly::module::KRBackPressModule::MODULE_NAME, [] {
        return std::make_shared<kuikly::module::KRBackPressModule>();
    });

}