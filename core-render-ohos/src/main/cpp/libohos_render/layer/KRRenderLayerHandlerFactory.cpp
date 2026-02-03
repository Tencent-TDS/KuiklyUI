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

#include "libohos_render/layer/KRRenderLayerHandlerFactory.h"
#include "libohos_render/layer/KRRenderLayerHandler.h"

// 编译期自动检测 TurboDisplay 是否可用
#if __has_include("libohos_render/layer/KRTurboDisplayRenderLayerHandler.h")
  #include "libohos_render/layer/KRTurboDisplayRenderLayerHandler.h"
  #include "libohos_render/context/KRRenderContextParamsExt.h"
  #define TURBO_DISPLAY_AVAILABLE 1
#else
  #define TURBO_DISPLAY_AVAILABLE 0
#endif

std::shared_ptr<IKRRenderLayer> KRRenderLayerHandlerFactory::CreateHandler(
    std::weak_ptr<IKRRenderView> renderView,
    std::shared_ptr<KRRenderContextParams> context,
    std::shared_ptr<KRUIScheduler> uiScheduler) {
    
#if TURBO_DISPLAY_AVAILABLE
    // 尝试获取 TurboDisplayKey
    std::string turboKey = GetTurboDisplayKeyFromContext(context);
    
    if (!turboKey.empty()) {
        auto turboHandler = std::make_shared<KRTurboDisplayRenderLayerHandler>();
        turboHandler->SetUIScheduler(uiScheduler);
        turboHandler->Init(renderView, context);
        return turboHandler;
    }
#endif
    
    // 默认创建普通Handler
    auto handler = std::make_shared<KRRenderLayerHandler>();
    handler->Init(renderView, context);
    return handler;
}
