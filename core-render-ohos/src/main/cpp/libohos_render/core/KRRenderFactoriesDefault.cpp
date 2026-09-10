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

#include "libohos_render/core/KRFeatureRegistry.h"
#include "libohos_render/core/KRRenderNativeFeature.h"

using ActiveFeatures = kuikly::features::KRFeatureRegistry<NativeFeature>;

namespace kuikly {

std::shared_ptr<IKRRenderLayer> RenderLayerFactory::CreateHandler(std::weak_ptr<IKRRenderView> render_view,
                                                                  std::shared_ptr<KRRenderContextParams> context,
                                                                  std::shared_ptr<KRUIScheduler> ui_scheduler) {
    auto layer = ActiveFeatures::CreateRenderLayer(render_view, context, ui_scheduler);
    return layer ? layer : NativeFeature::CreateRenderLayer(render_view, context, ui_scheduler);
}

std::shared_ptr<KRRenderExecuteMode> ExecuteModeFactory::Create(int mode) {
    auto execute_mode = ActiveFeatures::CreateExecuteMode(mode);
    return execute_mode ? execute_mode : NativeFeature::CreateMode();
}

std::shared_ptr<IKRRenderNativeContextHandler> ContextHandlerFactory::CreateContextHandler(
    const std::shared_ptr<KRRenderContextParams> &context) {
    int mode = 0;
    if (context && context->ExecuteMode()) {
        mode = context->ExecuteMode()->GetMode();
    }
    auto handler = ActiveFeatures::CreateContextHandler(mode, context);
    return handler ? handler : NativeFeature::CreateContextHandler(context);
}

namespace features {

void RegisterFeatureModules() {
    ActiveFeatures::RegisterModules();
}

std::shared_ptr<KRRenderValue> PreparePageDataForKotlin(const std::shared_ptr<KRRenderValue> &page_data,
                                                        const std::shared_ptr<IKRRenderLayer> &) {
    return page_data;
}

}  // namespace features
}  // namespace kuikly
