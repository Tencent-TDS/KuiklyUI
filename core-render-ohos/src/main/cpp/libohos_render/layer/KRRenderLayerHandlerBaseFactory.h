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
#ifndef OHOSAPP_KRRENDERLAYERHANDLERFACTORY_H
#define OHOSAPP_KRRENDERLAYERHANDLERFACTORY_H

#include <memory>
#include "libohos_render/layer/IKRRenderLayer.h"

class IKRRenderView;
class KRRenderContextParams;
class KRUIScheduler;

/**
 * 渲染层处理器工厂
 * 根据配置自动选择创建普通Handler或TurboDisplayHandler
 */
class KRRenderLayerHandlerBaseFactory {
public:
    virtual ~KRRenderLayerHandlerBaseFactory() = default;
    
    /**
     * 创建渲染层处理器
     * @param renderView 根渲染视图
     * @param context 上下文参数
     * @param uiScheduler UI调度器
     * @return 渲染层处理器实例
     */
    static std::shared_ptr<IKRRenderLayer> CreateHandler(
        std::weak_ptr<IKRRenderView> renderView,
        std::shared_ptr<KRRenderContextParams> context,
        std::shared_ptr<KRUIScheduler> uiScheduler);
    
    
private:
    std::shared_ptr<KRRenderLayerHandlerBaseFactory> instance;
};

#endif //OHOSAPP_KRRENDERLAYERHANDLERFACTORY_H
