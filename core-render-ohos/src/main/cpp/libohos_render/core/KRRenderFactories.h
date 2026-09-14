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

#ifndef CORE_RENDER_OHOS_KRRENDERFACTORIES_H
#define CORE_RENDER_OHOS_KRRENDERFACTORIES_H

#include <memory>

class IKRRenderView;
class IKRRenderLayer;
class KRRenderContextParams;
class KRRenderValue;
class KRUIScheduler;
class KRRenderExecuteMode;
class IKRRenderNativeContextHandler;

namespace kuikly {

struct RenderLayerFactory {
    static std::shared_ptr<IKRRenderLayer> CreateHandler(std::weak_ptr<IKRRenderView> render_view,
                                                         std::shared_ptr<KRRenderContextParams> context,
                                                         std::shared_ptr<KRUIScheduler> ui_scheduler);
};

struct ContextHandlerFactory {
    static std::shared_ptr<IKRRenderNativeContextHandler> CreateContextHandler(
        const std::shared_ptr<KRRenderContextParams> &context);
};

struct ExecuteModeFactory {
    static std::shared_ptr<KRRenderExecuteMode> Create(int mode);
};

namespace features {

void RegisterFeatureModules();

std::shared_ptr<KRRenderValue> PreparePageDataForKotlin(const std::shared_ptr<KRRenderValue> &page_data,
                                                        const std::shared_ptr<IKRRenderLayer> &layer);

}  // namespace features
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRRENDERFACTORIES_H
