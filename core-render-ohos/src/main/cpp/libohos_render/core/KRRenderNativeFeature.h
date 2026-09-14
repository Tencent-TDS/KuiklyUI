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

#ifndef CORE_RENDER_OHOS_KRRENDERNATIVEFEATURE_H
#define CORE_RENDER_OHOS_KRRENDERNATIVEFEATURE_H

#include <memory>
#include <string>

#include "libohos_render/context/DefaultRenderNativeContextHandler.h"
#include "libohos_render/context/KRRenderContextParams.h"
#include "libohos_render/context/KRRenderNativeMode.h"
#include "libohos_render/layer/IKRRenderLayer.h"
#include "libohos_render/layer/KRRenderLayerHandler.h"

struct NativeFeature {
    static constexpr int kMode = 0;

    static std::shared_ptr<KRRenderExecuteMode> CreateMode() {
        return std::make_shared<KRRenderNativeMode>();
    }

    static std::shared_ptr<IKRRenderNativeContextHandler> CreateContextHandler(
        const std::shared_ptr<KRRenderContextParams> &) {
        return std::make_shared<DefaultRenderNativeContextHandler>();
    }

    static std::shared_ptr<IKRRenderLayer> CreateRenderLayer(std::weak_ptr<IKRRenderView> render_view,
                                                             std::shared_ptr<KRRenderContextParams> context,
                                                             std::shared_ptr<KRUIScheduler>) {
        auto handler = std::make_shared<KRRenderLayerHandler>();
        handler->Init(render_view, context);
        return handler;
    }
};

#endif  // CORE_RENDER_OHOS_KRRENDERNATIVEFEATURE_H
