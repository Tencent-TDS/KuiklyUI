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

#include "libohos_render/context/KRRenderContextParamsExt.h"


#if TURBO_DISPLAY_AVAILABLE
#include "libohos_render/context/KRRenderContextParams.h"
#include "libohos_render/foundation/type/KRRenderValue.h"

std::string GetTurboDisplayKeyFromContext(std::shared_ptr<KRRenderContextParams> context) {
    if (!context) {
        return "";
    }
    
    auto pageData = context->PageData();
    if (!pageData) {
        return "";
    }
    
    auto pageMap = pageData->toMap();
    auto it = pageMap.find("turboDisplayKey");
    if (it != pageMap.end() && it->second) {
        return it->second->toString();
    }
    return "";
}

std::shared_ptr<KRTurboDisplayConfigData> GetTurboDisplayConfigFromContext(
    std::shared_ptr<KRRenderContextParams> context) {
    
    auto config = std::make_shared<KRTurboDisplayConfigData>();
    
    if (!context) {
        return config;
    }
    
    auto pageData = context->PageData();
    if (!pageData) {
        return config;
    }
    
    auto pageMap = pageData->toMap();
    auto it = pageMap.find("turboDisplayConfig");
    if (it == pageMap.end() || !it->second || !it->second->isMap()) {
        return config;
    }
    
    auto configMap = it->second->toMap();
    
    auto domIt = configMap.find("diffDOMMode");
    if (domIt != configMap.end() && domIt->second) {
        config->diff_dom_mode = domIt->second->toInt();
    }
    
    auto viewIt = configMap.find("diffViewMode");
    if (viewIt != configMap.end() && viewIt->second) {
        config->diff_view_mode = viewIt->second->toInt();
    }
    
    auto autoIt = configMap.find("autoUpdateTurboDisplay");
    if (autoIt != configMap.end() && autoIt->second) {
        config->auto_update = autoIt->second->toBool();
    }
    
    auto enableIt = configMap.find("enableRealTreePersistentUpdate");
    if (enableIt != configMap.end() && enableIt->second) {
        config->enable_persistent_update = enableIt->second->toBool();
    }
    
    return config;
}

#endif // TURBO_DISPLAY_AVAILABLE
